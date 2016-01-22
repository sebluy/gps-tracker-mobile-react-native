(ns re-natal-schejuler.core
  (:require [re-natal-schejuler.react :as r]
            [re-natal-schejuler.view :as v]
            [re-natal-schejuler.state :as s]
            [re-natal-schejuler.handlers :as h]))

(defn render [state]
  (js.React.render (v/Main state) 1))

(defn mount-root []
  (render @s/state))

;(mount-root)
;(js.React.render (js.React.createElement js.React.Text nil "Cleared") 1)

;(-> @s/state)
;(s/handle h/navigate {:id :somewhere-else})

;(-> js.React.BackAndroid)

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
  (.registerRunnable r/app-registry "ReNatalSchejuler" render))
