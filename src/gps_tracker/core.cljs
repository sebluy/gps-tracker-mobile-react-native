(ns gps-tracker.core
  (:require [gps-tracker.react :as r]
            [gps-tracker.remote :as rem]
            [gps-tracker.path :as p]
            [gps-tracker.util :as u]
            [gps-tracker-common.schema-helpers :as sh]
            [gps-tracker-common.schema :as cs]
            [clojure.walk :as w]
            [schema.core :as s]
            [gps-tracker.view :as v]))

(declare handle)
(declare render)
(declare address)

(defonce state (atom nil))
(defonce debug (atom {:state '() :actions '()}))

;(->> @debug :actions (take 2))
;(swap! debug assoc :actions '())
;(-> @debug)
;(-> @state keys)

(defn either [& schemas]
  (s/pred
   (fn [val]
     (some #(nil? (s/check % val)) schemas))))

(s/defschema Tracking {:tracking? (s/eq true)
                       :fix? (s/eq true)
                       :path cs/TrackingPath
                       :watch-id s/Int
                       :interval s/Int
                       :now cs/Date
                       :started cs/Date})

(s/defschema Upload {:tracking? (s/eq false)
                     :path cs/TrackingPath
                     :pending? s/Bool
                     :failed? s/Bool})

(s/defschema PendingFix {:tracking? (s/eq true)
                         :fix? (s/eq false)
                         :watch-id s/Int})

(s/defschema NotTracking (s/eq {:tracking? false}))

(s/defschema State (either Tracking NotTracking PendingFix Upload))

(s/defschema Start (s/eq '(:start)))
(s/defschema Stop (s/eq '(:stop)))

(s/defschema NewPosition
  (sh/list (s/eq :new-position) (sh/singleton cs/TrackingPoint)))

(s/defschema Tick (s/eq '(:tick)))

(s/defschema UploadA (s/eq '(:upload)))
(s/defschema UploadFailed (s/eq '(:upload-failed)))
(s/defschema CleanupUpload (s/eq '(:cleanup-upload)))

(s/defschema Action
  (either Start
          Stop
          NewPosition
          Tick
          UploadA
          UploadFailed
          CleanupUpload))

(s/defn coerce-position :- cs/TrackingPoint [js-pos]
  (let [{:strs [timestamp coords]} (js->clj js-pos)
        {:strs [latitude longitude speed]} coords]
    {:time (js/Date. timestamp)
     :latitude latitude
     :longitude longitude
     :speed speed}))

(defn watch-position []
  (let [on-success (fn [position]
                     (address `(:new-position ~(coerce-position position))))
        on-error r/toast
        options #js {:enableHighAccuracy true}]
    (js.React.Geolocation.watchPosition on-success on-error options)))

(defn clear-watch [id]
  (js.React.Geolocation.clearWatch id))

(defn request-fix [state]
  (let [watch-id (watch-position)]
    (assoc state
           :tracking? true
           :fix? false
           :watch-id watch-id)))

(defn now []
  (js/Date.))

(defn start-tracking [position state]
  (let [interval (js/setInterval #(address `(:tick)) 200)
        time (now)]
    (assoc state
           :fix? true
           :path {:id now :points [position]}
           :interval interval
           :now time
           :started time)))

(defn stop-tracking [{:keys [interval watch-id] :as state}]
  (clear-watch watch-id)
  (when interval
    (js/clearInterval interval))
  (-> state
      (assoc :tracking? false)
      (dissoc :started :now :interval :watch-id :fix?)))

(defn ask-to-upload [state]
  (assoc state
         :pending? false
         :failed? false))

(defn add-position [position state]
  (update-in state [:path :points] conj position))

(s/defn init :- State []
  {:tracking? false})

(s/defn handle :- State [action :- Action state :- State]
  (case (first action)
    :start
    (request-fix state)

    :tick
    (assoc state :now (now))

    :new-position
    (if (state :fix?)
      (add-position (last action) state)
      (start-tracking (last action) state))

    :upload
    (do
      (rem/post {:action :add-path
                 :path-type :tracking
                 :path (state :path)}
                #(address '(:cleanup-upload))
                #(address '(:upload-failed)))
      (assoc state :pending? true))

    :upload-failed
    (assoc state
           :failed? true
           :pending? false)

    :cleanup-upload
    (dissoc state :path :pending? :failed?)

    :stop
    (if (state :path)
      (-> state
          (stop-tracking)
          (ask-to-upload))
      (stop-tracking state))

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

(defn path-stats-view [{:keys [started now path]}]
  (r/view
   {:style [v/styles.timeBox
            v/styles.goldBorder]}
   (r/text nil (str "Time Elapsed: " (u/duration-str (u/duration started now))))
   (r/text nil (str "Total Distance: " (.toFixed (p/total-distance path) 2)))
   (r/text nil (str "Count: " (count (path :points))))
   (r/text nil (str "Average Speed: " (.toFixed (p/average-speed path) 2)))
   (when (> (count (path :points)) 1)
     (r/text nil (str "Current Speed: "
                      (.toFixed ((last (path :points)) :speed) 2))))))

(defn pending-fix-view []
  (r/view
   {:style [v/styles.timeBox
            v/styles.goldBorder]}
   (r/text nil (str "Pending Fix"))
   (r/progress-bar nil)))

(defn tracking-view [state]
  (r/view
   nil
   (button "Stop Tracking" #(address '(:stop)))
   (if (state :fix?)
     (path-stats-view state)
     (pending-fix-view))))

(defn upload-view [state]
  (r/view
   nil
   (r/text nil "Upload?")
   (button "Yes" #(address '(:upload)))
   (button "No" #(address '(:cleanup-upload)))))

(defn retry-upload-view []
  (r/view
   nil
   (r/text nil "Upload Failed. Retry?")
   (button "Yes" #(address '(:upload)))
   (button "No" #(address '(:cleanup-upload)))))

(defn pending-upload-view []
  (r/progress-bar nil))

(defn view [address state]
  (r/view
   {:style [v/styles.purple
            v/styles.fullPage]}
   (r/view
    {:style [v/styles.page]}
    (cond
      (state :tracking?)
      (tracking-view state)

      (state :pending?)
      (pending-upload-view)

      (state :failed?)
      (retry-upload-view)

      (state :path)
      (upload-view state)

      :else
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
  #_(js.React.BackAndroid.addEventListener
   "hardwareBackPress"
   (fn []
     (address '(:back))
     true))
  (.registerRunnable r/app-registry "GPSTracker" #(render @state)))
