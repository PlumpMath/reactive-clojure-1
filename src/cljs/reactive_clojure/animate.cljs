(ns reactive-clojure.animate
  (:require [goog.events :as events])
  (:import [goog.events EventType]))

(defn id->box
  "Given an ID of a DOM element, finds its wrapping parent element"
  [id]
  (let [marble (.getElementById js/document id)
        box (.-parentElement marble)]
    box))

(defn box->xpos
  "Given a DOM element, finds out its leftmost position"
  [box]
  (let [rect (.getBoundingClientRect box)]
    (.-left rect)))

(defn box->width
  "Given a DOM element, finds out its current width"
  [box]
  (let [rect (.getBoundingClientRect box)]
    (.-width rect)))

(defn id->store-path
  "Finds the path of marble with the given id within the deref-able store"
  [id store]
  (let [[idx el] (first (filter (fn [[idx el]] (= id (:id el)))
                                (map-indexed vector (:input @store))))]
    [idx el]))

(defn drag-move-fn
  "Creates a function that's able to move a marble. It requires:
  - the ID of the DOM element that represents the marble
  - the ID of the marble (used to find the marble in the store)
  - the deref-able store that contains all the marbles
  - the function that rerenders the diagram"
  [elem-id marble-id store render]
  (let [parent   (-> elem-id id->box)
        parent-x (-> parent box->xpos)
        parent-w (-> parent box->width)]
    (fn [evt]
      (let [xpos (.-clientX evt)
            relpos (- xpos parent-x)
            newtime (max 0
                         (min 100 (/ (* relpos 100) parent-w)))
            [idx el] (id->store-path marble-id store)
            newinput (vec (sort-by :t (assoc-in (:input @store) [idx :t] newtime)))]
        (swap! store assoc :input newinput)
        (render)))))

(defn drag-end-fn
  "Create the function that will stop the animation on mouse up. Requires:
  - the function that's moving the marble
  - the deref-able reference to the function that invokes this fn on mouse up"
  [drag-move drag-end]
  (fn [evt]
    (events/unlisten js/window EventType.MOUSEMOVE drag-move)
    (events/unlisten js/window EventType.MOUSEUP @drag-end)))

(defn dragging
  "Start the dragging animation for a marble. It requires
  - the ID of the DOM element representing the marble
  - the label of the marble (used to find the marble in the store)
  - the deref-able store containing all the marbles
  - the render function that will re-render the marble diagrams"
  [elem-id marble-id store render]
  (let [drag-move (drag-move-fn elem-id marble-id store render)
        drag-end-atom (atom nil)
        drag-end (drag-end-fn drag-move drag-end-atom)]
    (reset! drag-end-atom drag-end)
    (events/listen js/window EventType.MOUSEMOVE drag-move)
    (events/listen js/window EventType.MOUSEUP drag-end)))
