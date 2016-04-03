(ns gps-tracker.core
  (:require [gps-tracker.react :as r]
            [gps-tracker.page :as p]
            [gps-tracker.address :as a]
            [gps-tracker.util :as u]
            [gps-tracker-common.schema-helpers :as sh]
            [schema.core :as sc]
            [gps-tracker.styles :as st]))

(declare handle)
(declare render)
(declare address)

;(-> @state)
;(-> @debug :actions)

(defonce state (atom nil))
(defonce debug (atom {:state '() :actions '()}))

(sc/defschema State {:page p/State})

(sc/defschema Action
  (sh/list (sc/eq :page) p/Action))

(sc/defn init :- State []
  {:page (p/init)})

(sc/defn handle :- State [action :- Action state :- State]
  (case (first action)
    :page
    (update state :page
            (partial p/handle
                     (a/forward address (a/tag :page))
                     (rest action)))

    state))

(defn address [action]
  (swap! debug update :actions conj action)
  (swap! state (partial handle action))
  (swap! debug update :state conj @state)
  (render @state))

(defn render [state]
  (js.React.render (p/view (a/forward address (a/tag :page)) (state :page)) 1))

(defn on-back [f]
  (js.React.BackAndroid.addEventListener
   "hardwareBackPress"
   (fn [] (f) true)))

(defn ^:export init! []
  (reset! state (init))
  (on-back #(address '(:page :back)))
  (.registerRunnable r/app-registry "GPSTracker" #(render @state)))
