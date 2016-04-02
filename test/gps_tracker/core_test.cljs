(ns ^:figwheel-no-load gps-tracker.core-test
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

(defn generate-positions [initial]
  (lazy-seq (cons initial
                  (generate-positions
                   (-> initial
                       (update :time #(add-time % u/sec))
                       (update :latitude #(+ % 0.01))
                       (update :longitude #(+ % 0.01))
                       (update :speed #(+ % 1)))))))

(def actions
  (let [positions (generate-positions {:time (js/Date. "1/2/16 8:00:01 PM")
                                       :latitude 43.37
                                       :longitude -82.81
                                       :speed 1})]
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

(def now (atom nil))

(defn set-now! [date]
  (reset! now date))

(defn get-now! []
  (let [n @now]
    (swap! now #(add-time % u/sec))
    n))

(defn handle-with-redefs [action state]
  (with-redefs [tr/watch-position (constantly 1)
                tr/clear-watch (constantly nil)
                u/now get-now!
                js/setInterval (constantly 1)
                js/clearInterval (constantly nil)
                r/post (constantly nil)]
    (c/handle action state)))

(defn run [actions]
  (set-now! (js/Date. "1/2/16 8:00 PM"))
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

(defn run-with-render! [actions delay]
  (set-now! (js/Date. "1/2/16 8:00 PM"))
  (let [state (c/init)]
    (c/render state)
    (js/setTimeout #(render-loop state actions delay) delay)))

(def manual (atom nil))

(defn render-manual-reset! [actions]
  (set-now! (js/Date. "1/2/16 8:00 PM"))
  (let [state (c/init)]
    (c/render state)
    (reset! manual {:state state :actions actions})))

(defn render-manual-next! []
  (let [{:keys [actions state]} @manual]
    (when (seq actions)
      (let [next-state (handle-with-redefs (first actions) state)]
        (swap! manual assoc :state next-state :actions (rest actions))
        (c/render next-state)
        (first actions)))))

(defn render-manual-refresh! []
  (c/render (@manual :state)))

(t/use-fixtures :once st/validate-schemas)

(t/run-tests)

;(run-with-render! actions 1000)

;(render-manual-reset! actions)
;(render-manual-refresh!)
;(render-manual-next!)
