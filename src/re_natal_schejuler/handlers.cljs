(ns re-natal-schejuler.handlers
  (:require [re-natal-schejuler.react :as react]
            [re-natal-schejuler.state :as state]
            [re-natal-schejuler.path :as path]
            [re-natal-schejuler.util :as util]
            [cljs.reader :as reader]))

(defn get-waypoint-paths-failure [state reason]
  (react/alert "Oops" (str reason))
  (dissoc state :waypoint-paths))

(defn get-waypoint-paths-success [state body]
  (assoc state :waypoint-paths (first (reader/read-string body))))

(defn get-waypoint-paths [state]
  (-> (js.fetch "https://fierce-dawn-3931.herokuapp.com/api"
                (clj->js {:method "POST"
                          :headers {:content-type "application/edn"}
                          :body (str [{:action :get-paths
                                       :path-type :waypoint}])}))
      (.then #(.text %))
      (.then (fn [body]
               (state/handle get-waypoint-paths-success body)))
      (.catch (fn [reason]
                (state/handle get-waypoint-paths-failure reason))))
  (assoc state :waypoint-paths :pending))

(defn get-path [state id]
  (->> (state :waypoint-paths)
       (filter #(= id (% :id)))
       first))

(defn show-waypoint-path [state id]
  (react/alert (util/date->string id)
         (-> (path/waypoint-attributes (get-path state id))
             (util/attributes->str)))
  state)
