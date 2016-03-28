(ns gps-tracker.tracking
  (:require [gps-tracker.react :as r]
            [gps-tracker.path :as p]
            [gps-tracker.util :as u]
            [gps-tracker-common.schema-helpers :as sh]
            [gps-tracker-common.schema :as cs]
            [schema.core :as s]
            [gps-tracker.styles :as st]))

(s/defschema Tracking {:page (s/eq :tracking)
                       :fix? (s/eq true)
                       :path cs/TrackingPath
                       :watch-id s/Int
                       :interval s/Int
                       :now cs/Date
                       :started cs/Date})

(s/defschema PendingFix {:page (s/eq :tracking)
                         :fix? (s/eq false)
                         :watch-id s/Int})

(s/defschema Idle {:page (s/eq :tracking)
                   (s/optional-key :path) cs/TrackingPath})

(s/defschema State (u/either Tracking PendingFix Idle))

(s/defschema Start (s/eq '(:start)))

(s/defschema Stop (s/eq '(:stop)))

(s/defschema ReceivePosition
  (sh/list (s/eq :receive-position) (sh/singleton cs/TrackingPoint)))

(s/defschema Tick (s/eq '(:tick)))

(s/defschema Action
  (u/either Start
            Stop
            ReceivePosition
            Tick))

(s/defn coerce-position :- cs/TrackingPoint [js-pos]
  (let [{:strs [timestamp coords]} (js->clj js-pos)
        {:strs [latitude longitude speed]} coords]
    {:time (js/Date. timestamp)
     :latitude latitude
     :longitude longitude
     :speed speed}))

(defn watch-position [address]
  (let [on-success (fn [position]
                     (address `(:receive-position ~(coerce-position position))))
        on-error r/toast
        options #js {:enableHighAccuracy true}]
    (js.React.Geolocation.watchPosition on-success on-error options)))

(defn clear-watch [id]
  (js.React.Geolocation.clearWatch id))

(defn request-fix [address]
  (let [watch-id (watch-position address)]
    {:page :tracking
     :fix? false
     :watch-id watch-id}))

(defn start-tracking [address position state]
  (let [interval (js/setInterval #(address `(:tick)) 200)
        time (u/now)]
    (assoc state
           :fix? true
           :path {:id time :points [position]}
           :interval interval
           :now time
           :started time)))

(defn stop-tracking [{:keys [interval watch-id] :as state}]
  (clear-watch watch-id)
  (when interval
    (js/clearInterval interval))
  (dissoc state :started :now :interval :watch-id :fix?))

(defn add-position [position state]
  (update-in state [:path :points] conj position))

(s/defn handle :- State [address action :- Action state :- State]
  (case (first action)
    :start
    (request-fix address)

    :tick
    (assoc state :now (u/now))

    :receive-position
    (if (state :fix?)
      (add-position (last action) state)
      (start-tracking address (last action) state))

    :stop
    (stop-tracking state)

    state))

;;;; VIEW

(defn path-stats-view [{:keys [started now path]}]
  (r/view
   {:style [st/styles.timeBox
            st/styles.goldBorder]}
   (r/text nil (str "Time Elapsed: " (u/duration-str (u/duration started now))))
   (r/text nil (str "Total Distance: " (.toFixed (p/total-distance path) 2)))
   (r/text nil (str "Count: " (count (path :points))))
   (r/text nil (str "Average Speed: " (.toFixed (p/average-speed path) 2)))
   (when (> (count (path :points)) 1)
     (r/text nil (str "Current Speed: "
                      (.toFixed ((last (path :points)) :speed) 2))))))

(defn pending-fix-view []
  (r/view
   {:style [st/styles.timeBox
            st/styles.goldBorder]}
   (r/text nil (str "Pending Fix"))
   (r/progress-bar nil)))

(defn view [address state]
  (r/view
   nil
   (u/button "Stop Tracking" #(address '(:stop)))
   (if (state :fix?)
     (path-stats-view state)
     (pending-fix-view))))
