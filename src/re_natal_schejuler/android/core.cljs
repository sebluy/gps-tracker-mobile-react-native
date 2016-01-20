(ns ^:figwheel-no-load re-natal-schejuler.android.core
  (:require [cljs.reader :as reader]))

(set! js.React (js.require "react-native/Libraries/react-native/react-native.js"))
(set! js.React.ProgressBar (js.require "ProgressBarAndroid"))

(def app-registry js.React.AppRegistry)

(defn alert [title]
  (.alert (.-Alert js.React) title))

(defn component-constructor [type]
  (fn [props & children]
    (apply js.React.createElement type (clj->js props) children)))

(def Text (component-constructor js.React.Text))
(def TouchableHighlight (component-constructor js.React.TouchableHighlight))
(def View (component-constructor js.React.View))
(def ListView (component-constructor js.React.ListView))
(def ProgressBar (component-constructor js.React.ProgressBar))

(defn paths->list [waypoint-paths]
  (map (fn [path] (-> path :id date->string)) waypoint-paths))

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
                (alert (str "Oops" reason))))))

(defn simple-datasource [vals]
  (let [ds (js.React.ListView.DataSource.
            #js {:rowHasChanged (fn [r1 r2] (not= r1 r2))})]
    (.cloneWithRows ds (clj->js vals))))

(defn view [state]
  (View {:style {:margin 40}}
        (TouchableHighlight {:onPress #(get-waypoint-paths)
                             :style {:padding 10
                                     :backgroundColor "#C0C0C0"
                                     :borderRadius 5}}
                            (Text {:style {:textAlign "center"}} "Refresh"))
        (if (and (not= (state :waypoint-paths) :pending) (seq (state :waypoint-paths)))
          (ListView {:style {:marginTop 40}
                     :dataSource (simple-datasource (paths->list (state :waypoint-paths)))
                     :renderRow (fn [rowData] (Text {:style {:textAlign "center"}} rowData))})
          (if (= (state :waypoint-paths) :pending)
            (ProgressBar {:style {:marginTop 40}
                          :styleAttr "Inverse"})
            (Text {:style {:marginTop 40}} "No Waypoints")))))

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

(defn render [state]
  (js.React.render (view state) 1))

#_(render @state)

(defn ^:export init []
  (.registerRunnable app-registry "ReNatalSchejuler" render))
