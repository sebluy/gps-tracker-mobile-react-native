(ns gps-tracker.core-test
  (:require [cljs.test :as t]
            [gps-tracker.core :as c]
            [gps-tracker.util :as u]
            [gps-tracker.tracking :as tr]
            [gps-tracker.remote :as r]
            [schema.test :as st]))

(t/deftest position-coercion
  (t/testing "js to clojure location coercion"
    (let [time (js/Date. "1/2/16 8:00 PM")
          speed 1.2
          latitude 2.3
          longitude 3.4
          clj {:time time
               :speed speed
               :latitude latitude
               :longitude longitude}
          js #js {:timestamp (.getTime time)
                  :coords #js {:latitude latitude
                               :longitude longitude
                               :speed speed}}]
      (t/is (= (tr/coerce-position js) clj)))))

(defn lame-position [date]
  #js {:timestamp (.getTime date)
       :coords #js {:latitude 1.0 :longitude 1.0 :speed 1.0}})

(defn receive-action-time [action]
  (-> action second :time))

(defn add-time [date ms]
  (js/Date. (+ (.getTime date) ms)))

(t/deftest on-new-position
  (t/testing "on new position"
    (let [actions (atom '())
          address #(swap! actions conj %)
          on-new-position (tr/on-new-position address)
          start-time (js/Date. "1/2/16 8:00 PM")
          plus-7 (add-time start-time (* 7 u/sec))
          plus-11 (add-time start-time (* 11 u/sec))
          plus-12 (add-time start-time (* 12 u/sec))
          plus-21 (add-time start-time (* 21 u/sec))]
      (doseq [time [start-time plus-7 plus-11 plus-12 plus-21]]
        (on-new-position (lame-position time)))
      (t/is (= (map receive-action-time @actions)
               `(~plus-21 ~plus-11 ~start-time))))))

(defn generate-positions [time speed]
  (lazy-seq (cons {:time time :latitude 1.2 :longitude 2.4 :speed speed}
                  (generate-positions (add-time time u/sec) (+ speed 2)))))

(def actions
  (let [positions (generate-positions (js/Date. "1/2/16 8:00:01 PM") 1)]
    [`(:tracking :start)
     `(:tracking :receive-position ~(first positions))
     `(:tracking :tick)
     `(:tracking :receive-position ~(fnext positions))
     `(:tracking :tick)
     `(:tracking :stop)
     `(:remote :send)
     `(:remote :failure)
     `(:remote :send)
     `(:remote :success)
     `(:remote :cleanup)]))

(defn mock-now [start]
  (let [time (atom start)]
    (fn []
      (swap! time #(add-time % u/sec))
      @time)))

(defn handle-with-redefs [action state]
  (let [time (js/Date. "1/2/16 8:00 PM")]
    (with-redefs [tr/watch-position (constantly 1)
                  tr/clear-watch (constantly nil)
                  u/now (mock-now time)
                  js/setInterval (constantly 1)
                  js/clearInterval (constantly nil)
                  r/post (constantly nil)]
      (c/handle action state))))

(defn run [actions]
  (reduce #(handle-with-redefs %2 %1) (c/init) actions))

(t/deftest handle
  (t/testing "state handlers"
    (run actions)))

;;;; ---- View -----

(defn render-loop [state actions delay]
  (when (seq actions)
    (let [next-state (handle-with-redefs (first actions) state)]
      (c/render next-state)
      (js/setTimeout #(render-loop next-state (rest actions) delay) delay))))

(defn run-with-render [actions delay]
  (let [state (c/init)]
    (c/render state)
    (js/setTimeout #(render-loop state actions delay) delay)))

(t/use-fixtures :once st/validate-schemas)

;;(t/run-tests)

;;(run-with-render actions 1000)
