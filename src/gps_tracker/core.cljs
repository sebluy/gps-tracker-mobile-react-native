(ns gps-tracker.core
  (:require [gps-tracker.react :as r]
            [clojure.string :as s]
            [gps-tracker.view :as v]))

(declare handle)
(declare render)
(declare address)

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
  (js.React.ToastAndroid.show msg js.React.ToastAndroid.SHORT))

(defn watch-position []
  (let [on-success (fn [position]
                     (address `(:new-position ~(js->clj position))))
        on-error toast
        options #js {:enableHighAccuracy true}]
    (js.React.Geolocation.watchPosition on-success on-error options)))

(defn clear-watch [id]
  (js.React.Geolocation.clearWatch id))

(defn start-tracking [state]
  (let [interval (js/setInterval #(address `(:tick)) 200)]
    (assoc state
           :tracking true
           :interval interval
           :now (js/Date.)
           :started (js/Date.))))

(defn stop-tracking [{:keys [interval] :as state}]
  (js/clearInterval interval)
  (-> state
      (assoc :tracking false)
      (dissoc :started :now)))

(defn init []
  {:tracking false})

(defn handle [action state]
  (case (first action)
    :start
    (start-tracking state)

    :tick
    (assoc state :now (js/Date.))

    :stop
    (stop-tracking state)

    state))

(defn button [text on-press]
  (r/touchable-highlight
   {:onPress on-press
    :style [v/styles.button
            v/styles.goldBorder]}
   (r/text
    {:style [v/styles.text
             v/styles.bigText]}
    text)))

(defn stop-tracking-button [address]
  (r/touchable-highlight
   {:onPress #(address '(:stop))}
   (r/text nil "Stop Tracking")))

(defn last-position-view [position]
  (r/text nil (str position)))

(defn pad2-0 [s]
  (if (= 1 (count s))
    (str "0" s)
    s))

(defn duration-str [ms]
  (let [s (mod (js/Math.floor (/ ms 1000)) 60)
        m (mod (js/Math.floor (/ ms 60000)) 60)
        h (mod (js/Math.floor (/ ms (* 60 60 1000))) 24)]
    (s/join ":" (map (comp pad2-0 str) [h m s]))))

(defn duration [t1 t2]
  (- (.getTime t2) (.getTime t1)))


(defn tracking-view [{:keys [started now]}]
  (r/view
   nil
   (button "Stop Tracking" #(address '(:stop)))

   (r/view
    {:style [v/styles.timeBox
             v/styles.goldBorder]}
    (r/text nil (duration-str (duration started now))))))

(defn view [address {:keys [tracking last-position] :as state}]
  (r/view
   {:style [v/styles.purple
            v/styles.fullPage]}
   (r/view
    {:style [v/styles.page]}
    (if tracking
      (tracking-view state)
      (button "Start Tracking" #(address '(:start)))))))

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
