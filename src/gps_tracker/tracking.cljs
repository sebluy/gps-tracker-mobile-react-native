(ns gps-tracker.tracking
  (:require [gps-tracker.react :as r]
            [gps-tracker.path :as p]
            [gps-tracker.util :as u]
            [gps-tracker-common.schema-helpers :as sh]
            [gps-tracker-common.schema :as cs]
            [schema.core :as s]
            [gps-tracker.styles :as st]))

;; STATE

(s/defschema Tracking {:fix? (s/eq true)
                       :path cs/TrackingPath
                       :watch-id s/Int
                       :interval s/Int
                       :now cs/Date
                       :started cs/Date})

(s/defschema PendingFix {:fix? (s/eq false)
                         :watch-id s/Int})

;; idle state is after tracking has stopped, but keep path so
;; something else can use it
(s/defschema Idle {(s/optional-key :path) cs/TrackingPath})

(s/defschema BadPositionState {:bad-position? (s/eq true)})

(s/defschema State (u/either Tracking PendingFix Idle BadPositionState))

;; ACTIONS

(s/defschema Start (s/eq '(:start)))

(s/defschema Cleanup (s/eq '(:cleanup)))

(s/defschema ReceivePosition
  (sh/list (s/eq :receive-position) (sh/singleton cs/TrackingPoint)))

(s/defschema BadPositionAction
  (s/eq '(:bad-position)))

(s/defschema Tick (s/eq '(:tick)))

(s/defschema Action
  (u/either Start
            Cleanup
            ReceivePosition
            BadPositionAction
            Tick))

;; HANDLERS

(s/defn coerce-position :- cs/TrackingPoint [js-pos]
  (let [{:strs [timestamp coords]} (js->clj js-pos)
        {:strs [latitude longitude speed]} coords]
    {:time (js/Date. timestamp)
     :latitude latitude
     :longitude longitude
     :speed speed}))

(defn filter-position-10s [f]
  (let [last (atom nil)]
    (fn [position]
      (cond
        (nil? @last)
        (do (reset! last (position :time))
            (f position))

        (>= (u/duration @last (position :time)) (* 10 u/sec))
        (do (reset! last (position :time))
            (f position))

        :else
        nil))))

(defn map-coerce-position [f]
  (fn [position]
    (f (coerce-position position))))

(defn on-new-position [address]
  (-> (fn [position] (address `(:receive-position ~position)))
      (filter-position-10s)
      (map-coerce-position)))

(defn watch-position [address]
  (let [on-success (on-new-position address)
        on-error #(address `(:bad-position))
        options #js {:enableHighAccuracy true}]
    (js.React.Geolocation.watchPosition on-success on-error options)))

(defn clear-watch [id]
  (js.React.Geolocation.clearWatch id))

(defn request-fix [address]
  (let [watch-id (watch-position address)]
    {:fix? false
     :watch-id watch-id}))

(defn start-tracking [address position state]
  (let [interval (js/setInterval #(address `(:tick)) 250)
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

    :bad-position
    (if (state :fix)
      state
      (do
        (stop-tracking state)
        {:bad-position? true}))

    :cleanup
    (stop-tracking state)

    state))

;; VIEW

(defn bad-position-view []
  (r/text {:style st/title} "Please enable location updates and try again."))

(defn stat-view [stat]
  (r/text {:style st/stat} stat))

(defn path-stats-view [{:keys [started now path]}]
  (r/view
   nil
   (stat-view (str "Time Elapsed: " (u/duration-str (u/duration started now))))
   (stat-view (str "Total Distance: " (str (.toFixed (u/miles (p/total-distance path)) 2) " miles")))
   (stat-view (str "Count: " (count (path :points))))
   (stat-view (str "Average Speed: " (str (.toFixed (u/mph (p/average-speed path)) 2) " mph")))
   (stat-view
    (str "Current Speed: " (str (.toFixed (u/mph ((last (path :points)) :speed)) 2) " mph")))))

(defn pending-fix-view []
  (r/view
   nil
   (r/text {:style st/title} (str "Pending Fix"))
   (r/progress-bar nil)))

(defn view [address state]
  (cond
    (state :fix?)
    (path-stats-view state)

    (state :bad-position?)
    (bad-position-view)

    :else
    (pending-fix-view)))
