(ns gps-tracker.core
  (:require [gps-tracker.react :as r]
            [gps-tracker.remote :as rem]
            [gps-tracker.tracking :as t]
            [gps-tracker.address :as a]
            [gps-tracker.util :as u]
            [gps-tracker-common.schema-helpers :as sh]
            [schema.core :as sc]
            [gps-tracker.styles :as st]))

(declare handle)
(declare render)
(declare address)

(defonce state (atom nil))
(defonce debug (atom {:state '() :actions '()}))

;(->> @debug :actions (take 2))
;(swap! debug assoc :actions '())
;(-> @debug)
;(-> @state keys)

(sc/defschema State (u/either t/State rem/State))

(sc/defschema Action
  (u/either (sh/list (sc/eq :remote) rem/Action)
            (sh/list (sc/eq :tracking) t/Action)))

(sc/defn init :- State []
  {:page :home})

(sc/defn handle :- State [action :- Action state :- State]
  (case (first action)
    :remote
    (rem/handle (a/forward address (a/tag :remote)) (rest action) state)

    :tracking
    (t/handle (a/forward address (a/tag :tracking)) (rest action) state)

    state))

(defn home [address]
  (u/button "Start Tracking" #(address `(:tracking :start))))

(defn view [address state]
  (r/view
   {:style [st/styles.purple
            st/styles.fullPage]}
   (r/view
    {:style [st/styles.page]}
    (case (state :page)
      :tracking
      (t/view (a/forward address (a/tag :tracking)) state)

      :remote
      (rem/view (a/forward address (a/tag :remote)) state)

      (home address)))))


(defn address [action]
  (swap! debug update :actions conj action)
  (swap! state (partial handle action))
  (swap! debug update :state conj @state)
  (render @state))

(defn render [state]
  (js.React.render (view address state) 1))

(defn ^:export init! []
  (reset! state (init))
  #_(js.React.BackAndroid.addEventListener
   "hardwareBackPress"
   (fn []
     (address '(:back))
     true))
  (.registerRunnable r/app-registry "GPSTracker" #(render @state)))
