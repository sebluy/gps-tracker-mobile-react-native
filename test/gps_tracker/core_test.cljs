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

(def actions
  (let [time (js/Date. "1/2/16 8:00 PM")
        position {:time time :latitude 1.2 :longitude 2.4 :speed 3.6}]
    [`(:tracking :start)
     `(:tracking :receive-position ~position)
     `(:tracking :tick)
     `(:tracking :receive-position ~position)
     `(:tracking :tick)
     `(:tracking :stop)
     `(:remote :send)
     `(:remote :failure)
     `(:remote :send)
     `(:remote :success)
     `(:remote :cleanup)]))

(defn handle-with-redefs [action state]
  (let [time (js/Date. "1/2/16 8:00 PM")]
    (with-redefs [tr/watch-position (constantly 1)
                  tr/clear-watch (constantly nil)
                  u/now (constantly time)
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

(run-with-render actions 1000)

(t/use-fixtures :once st/validate-schemas)

(t/run-tests)
