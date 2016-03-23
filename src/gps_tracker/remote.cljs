(ns gps-tracker.remote
  (:require [cljs.reader :as reader]))

(defn post [action on-success on-failure]
  (-> (js.fetch "https://fierce-dawn-3931.herokuapp.com/api"
                (clj->js {:method "POST"
                          :headers {:content-type "application/edn"}
                          :body (str [action])}))
      (.then #(.text %))
      (.then (fn [body]
               (on-success (first (reader/read-string body)))
      (.catch (fn [reason]
                (on-failure)))))))
