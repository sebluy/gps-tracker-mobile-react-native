(ns gps-tracker.core
  (:require [gps-tracker.react :as r]))

(declare handle)
(declare render)

(defonce state (atom nil))
(defonce debug (atom {:state '() :actions '()}))

;(-> @state :tracking-paths)
;(-> @debug :state #_(nth 0) :checkpoints)
;(->> @debug :actions (take 2))
;(swap! debug assoc :actions '())
;(-> @debug)

;(s/defschema Page {:id (s/eq :home)})

;(s/defschema State {:page p/Page})

;(def Action s/Any)
;(s/defschema Action s/Any)

;(js/Object.keys js.React.ToastAndroid)

(defn toast [msg]
  (js.React.ToastAndroid.show msg js.React.ToastAndroid.LONG))

;(toast "hi")

;(str (js->clj #js {:hi "you"}))

;(Object.keys js.React.Geolocation)
#_(js.React.Geolocation.getCurrentPosition
 (fn [location] (-> location js->clj str toast))
 (fn [error] (toast error))
 #js {:timeout 5000})

(defn init []
  {:page {:id :home}})

(defn handle [action state]
  state)

(defn view [address state]
  (r/text nil "Hello, world!"))

(defn address [action]
  (swap! debug update :actions conj action)
  (swap! state (partial handle action))
  (swap! debug update :state conj @state)
  (render @state))

(defn render [state]
  (js.React.render (view address state) 1))

(defn ^:export init! []
  (reset! state (init))
  (js.React.BackAndroid.addEventListener
   "hardwareBackPress"
   (fn []
     (address '(:back))
     true))
  (.registerRunnable r/app-registry "GPSTracker" #(render @state)))
