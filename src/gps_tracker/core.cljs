(ns gps-tracker.core
  (:require [gps-tracker.react :as r]
            [gps-tracker.view :as v]
            [gps-tracker.state :as s]
            [gps-tracker.handlers :as h]))

(defn render [state]
  (js.React.render (v/Main state) 1))

(defn mount-root []
  (render @s/state))

;(mount-root)
;(js.React.render (js.React.createElement js.React.Text nil "Cleared") 1)

;(-> @s/state)
;(s/handle h/navigate {:id :somewhere-else})

;(-> js.React.BackAndroid)
;(+ 1 1)

(defn ^:export init []
  (s/handle h/initialize)
  (js.React.BackAndroid.addEventListener
   "hardwareBackPress"
   (fn []
     (s/handle h/back)
     true))
  (add-watch s/state :render
             (fn [_ _ _ new-state]
               (render new-state)))
  (.registerRunnable r/app-registry "GPSTracker" render))
