(ns reactive-clojure.simple
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent :refer [atom]]
            [reactive-clojure.marbles-sandbox :as sandbox]
            [reactive-clojure.marbles :as marbles]
            [cljs.core.async :refer [put! chan <! onto-chan]]))

(enable-console-print!)

(def marbles (atom {:input [{:t 10 :l 1}
                            {:t 30 :l 2}
                            {:t 80 :l 3}]
                    :output []}))

(defn render []
  (let [input (chan)
        output (chan)
        _ (onto-chan input (:input @marbles))]
    (swap! marbles assoc :output [])
    (go (loop []
          (let [m (<! input)]
            (when m
              (>! output m)
              (recur)))))

    (go (loop []
          (let [m (<! output)]
            (swap! marbles (fn [old]
                             (merge-with into old {:output [m]})))
            (recur))))))

(render)

(defn simple-get []
  (sandbox/marble-sandbox
   (sandbox/sandbox "input" (marbles/marbles-box (map (partial marbles/marble marbles render) (:input @marbles))))
   (sandbox/operator "(<! channel)")
   (sandbox/sandbox "output" (marbles/marbles-box (map marbles/static-marble (:output @marbles))))))
