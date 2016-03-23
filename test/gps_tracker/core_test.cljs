(ns gps-tracker.core-test
  (:require [cljs.test :as t]
            [gps-tracker.core :as c]
            [gps-tracker.remote :as r]
            [schema.test :as st]))

(t/deftest a-test
  (t/testing "1 = 1"
    (t/is (= 1 1))))

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
      (t/is (= (c/coerce-position js) clj)))))

(t/deftest handle
  (t/testing "state handlers"
    (let [time (js/Date. "1/2/16 8:00 PM")
          position {:time time :latitude 1.2 :longitude 2.4 :speed 3.6}]
      (with-redefs [c/watch-position (constantly 1)
                    c/clear-watch (constantly nil)
                    c/now (constantly time)
                    js/setInterval (constantly 1)
                    js/clearInterval (constantly nil)
                    r/post (constantly nil)]
        (->> (c/init)
             (c/handle `(:start))
             (c/handle `(:new-position ~position))
             (c/handle `(:tick))
             (c/handle `(:new-position ~position))
             (c/handle `(:tick))
             (c/handle `(:stop))
             (c/handle `(:upload))
             (c/handle `(:cleanup-upload)))))))

(t/use-fixtures :once st/validate-schemas)

(t/run-tests)
