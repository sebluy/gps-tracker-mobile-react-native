(ns gps-tracker.handlers
  (:require [gps-tracker.react :as react]
            [gps-tracker.state :as state]
            [gps-tracker.path :as path]
            [gps-tracker.util :as util]
            [cljs.reader :as reader]))

;;;; history
(defn create-history [state]
  (assoc state :history '()))

(defn push-history [state page]
  (update state :history conj page))

(defn pop-history [state]
  (update state :history pop))

;;;; pages and navigation
(defn set-page [state page]
  (assoc state :page page))

(defn navigate [state page]
  (-> state
      (push-history (state :page))
      (set-page page)))

(defn back [state]
  (let [last-page (first (state :history))]
    (when (nil? last-page)
      (js.React.BackAndroid.exitApp))
    (cond-> state
      last-page (-> (set-page last-page)
                    (pop-history)))))

(defn initialize [state]
  (-> state
      (set-page {:id :waypoint-path-list})
      (create-history)))

;; network waypoint paths
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

;; display waypoint path
(defn show-waypoint-path [state id]
  (navigate state {:id :show-waypoint-path :path-id id}))
