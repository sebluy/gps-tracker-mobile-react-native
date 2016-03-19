(ns gps-tracker.core
  (:require [gps-tracker.react :as r]
            [gps-tracker.path :as p]
            [clojure.string :as string]
            [clojure.walk :as w]
            [schema.core :as s]
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

(-> @state :path)

;(s/defschema Page {:id (s/eq :home)})

(s/defschema State {:path s/Any ; tracking path
                    :watch-id s/Int
                    :tracking s/Bool
                    :interval s/Int
                    :now s/Any ; date
                    :started s/Any ; date
                    })

;(def Action s/Any)
;(s/defschema Action s/Any)

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
  (let [interval (js/setInterval #(address `(:tick)) 200)
        now (js/Date.)
        watch-id (watch-position)]
    (assoc state
           :path {:id now :points []}
           :watch-id watch-id
           :tracking true
           :interval interval
           :now now
           :started now)))

(defn stop-tracking [{:keys [interval watch-id] :as state}]
  (clear-watch watch-id)
  (js/clearInterval interval)
  (-> state
      (assoc :tracking false)
      (dissoc :started :now :interval :watch-id)))

(defn add-position [position state]
  (let [coords (position "coords")
        latitude (coords "latitude")
        longitude (coords "longitude")
        speed (coords "speed")]
    (update-in state [:path :points]
               conj {:latitude latitude :longitude longitude :speed speed})))

(defn init []
  {:tracking false})

(defn handle [action state]
  (case (first action)
    :start
    (start-tracking state)

    :tick
    (assoc state :now (js/Date.))

    :new-position
    (add-position (last action) state)

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
    (string/join ":" (map (comp pad2-0 str) [h m s]))))

(defn duration [t1 t2]
  (- (.getTime t2) (.getTime t1)))

(defn tracking-view [{:keys [started now path]}]
  (r/view
   nil
   (button "Stop Tracking" #(address '(:stop)))
   (r/view
    {:style [v/styles.timeBox
             v/styles.goldBorder]}
    (r/text nil (str "Time Elapsed: " (duration-str (duration started now))))
    (r/text nil (str "Total Distance: " (.toFixed (p/total-distance path) 2)))
    (r/text nil (str "Count: " (count (path :points))))
    (r/text nil (str "Average Speed: " (.toFixed (p/average-speed path) 2)))
    (when (> (count (path :points)) 1)
      (r/text nil (str "Current Speed: " (.toFixed ((last (path :points)) :speed) 2)))))))

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
