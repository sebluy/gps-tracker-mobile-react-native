(ns ^:figwheel-no-load re-natal-schejuler.android.core
  (:require [cljs.reader :as reader]
            [re-natal-schejuler.quiescent :as q]))

(set! js.React (js.require "react-native/Libraries/react-native/react-native.js"))
(set! js.React.ProgressBar (js.require "ProgressBarAndroid"))
(set! js.React.Toolbar (js.require "ToolbarAndroid"))

(def app-registry js.React.AppRegistry)

(defn alert
  ([title] (alert title nil))
  ([title msg]
   (.alert (.-Alert js.React) title msg)))

(def text (q/constructor js.React.Text))
(def image (q/constructor js.React.Image))
(def touchable-highlight (q/constructor js.React.TouchableHighlight))
(def view (q/constructor js.React.View))
(def list-view (q/constructor js.React.ListView))
(def progress-bar (q/constructor js.React.ProgressBar))
(def toolbar (q/constructor js.React.Toolbar))

(defonce state (atom {}))

(declare render)

(add-watch state :render
           (fn [_ _ _ new-state]
             (render new-state)))

(defn get-waypoint-paths []
  (swap! state assoc :waypoint-paths :pending)
  (-> (js.fetch "https://fierce-dawn-3931.herokuapp.com/api"
                (clj->js {:method "POST"
                          :headers {:content-type "application/edn"}
                          :body (str [{:action :get-paths
                                       :path-type :waypoint}])}))
      (.then #(.text %))
      (.then (fn [body]
               (swap! state assoc :waypoint-paths (first (reader/read-string body)))))
      (.catch (fn [reason]
                (swap! state dissoc :waypoint-paths)
                (alert "Oops" (str reason))))))

(defn simple-datasource [vals]
  (let [ds (js.React.ListView.DataSource.
            #js {:rowHasChanged (fn [r1 r2] (not= r1 r2))})]
    (.cloneWithRows ds (clj->js vals))))

(defn zero-pad [s]
  (if (= (count s) 1)
    (str "0" s)
    s))

(defn time-string [date]
  (let [hours (.getHours date)
        am? (< hours 12)]
    (str (zero-pad (str (if am? hours (- hours 12))))
         ":"
         (.getMinutes date)
         " "
         (if am? "AM" "PM"))))

(defn date->string [date]
  (str (.toLocaleDateString date) " " (time-string date)))

(defn waypoint-path-row [id]
  (touchable-highlight {:onPress #(show-waypoint-path id)
                       :style {:padding 5
                               :marginBottom 15
                               :backgroundColor "#C0C0C0"
                               :borderRadius 5}}
                      (text {:style {:textAlign "center"}} (date->string id))))

(defn show-waypoint-path [id]
  (alert (date->string id)
         (str "Count: "
              (->> (@state :waypoint-paths)
                   (filter #(= id (% :id)))
                   first
                   :points
                   count))))

(q/defcomponent Simple
  [msg]
  (text {} msg))

(q/defcomponent View
  :on-mount #(get-waypoint-paths)
  [state]
  (view {}
        (toolbar {:title "GPSTracker"
                  :style {:height 56
                          :backgroundColor "#C0C0C0"}})
        (view {:style {:margin 40}}
              (text {:style {:textAlign "center"
                             :fontSize 18}}
                    "Waypoint Paths")
              (touchable-highlight {:onPress #(get-waypoint-paths)
                                    :style {:padding 10
                                            :marginTop 15
                                            :marginHorizontal 40
                                            :backgroundColor "#C0C0C0"
                                            :borderRadius 5}}
                                   (text {:style {:textAlign "center"}} "Refresh"))
              (if (and (not= (state :waypoint-paths) :pending) (seq (state :waypoint-paths)))
                (list-view {:style {:marginTop 15}
                            :dataSource (simple-datasource (map :id (state :waypoint-paths)))
                            :renderRow waypoint-path-row})
                (if (= (state :waypoint-paths) :pending)
                  (progress-bar {:style {:marginTop 40}
                                 :styleAttr "Inverse"})
                  (text {:style {:marginTop 40
                                 :textAlign "center"}}
                        "No Waypoints"))))))

(defn render [state]
  (js.React.render (View state) 1))

#_(render @state)

(defn mount-root []
  (render @state))

(defn ^:export init []
  (.registerRunnable app-registry "ReNatalSchejuler" render))
