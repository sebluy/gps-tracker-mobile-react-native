(ns ^:figwheel-load re-natal-schejuler.android.core)

(set! js.React (js.require "react-native/Libraries/react-native/react-native.js"))

(def app-registry js.React.AppRegistry)

(defn alert [title]
  (.alert (.-Alert js.React) title))

(defn component-constructor [type]
  (fn [props & children]
    (apply js.React.createElement type (clj->js props) children)))

(def Text (component-constructor (.-Text js.React)))
(def TouchableHighlight (component-constructor (.-TouchableHighlight js.React)))
(def View (component-constructor (.-View js.React)))
(def ListView (component-constructor js.React.ListView))

(defn simple-datasource [vals]
  (let [ds (js.React.ListView.DataSource.
            #js {:rowHasChanged (fn [r1 r2] (not= r1 r2))})]
    (.cloneWithRows ds (clj->js vals))))

(def WaypointPathListClass
  (js.React.createClass
   #js {:displayName "WaypointPathList"
        :getInitialState
        (fn []
          (let [ds (js.React.ListView.DataSource. #js {:rowHasChanged (fn [r1 r2] (not= r1 r2))})]
            #js {:dataSource (.cloneWithRows ds #js ["row 1" "row 2"])}))
        :render
        (fn []
          (this-as this
                   (ListView {:dataSource (.. this -state -dataSource)
                              :renderRow (fn [rowData] (Text {} rowData))})))}))

(def WaypointPathList (component-constructor WaypointPathListClass))

(defn better-view []
  (View {:style {:margin 40}}
        (TouchableHighlight {:onPress #(alert "Hi")
                             :style {:padding 10
                                     :backgroundColor "#C0C0C0"
                                     :borderRadius 5}}
                            (Text {:style {:textAlign "center"}} "Refresh"))
        (ListView {:dataSource (simple-datasource (paths->list waypoint-paths))
                   :renderRow (fn [rowData] (Text {} rowData))})
        (Text {:style {:marginTop 40}} "No Waypoints")))

(defn render []
  (js.React.render (better-view) 1))

(render)

(def waypoint-paths @response)

(defn paths->list [waypoint-paths]
  (map (fn [path] (-> path :id str)) waypoint-paths))

(-> waypoint-paths)

(def response (atom nil))

(get-waypoint-paths response)

(defn get-waypoint-paths [atom]
  (-> (js.fetch "https://fierce-dawn-3931.herokuapp.com/api"
                (clj->js {:method "POST"
                          :headers {:content-type "application/edn"}
                          :body (str [{:action :get-paths
                                       :path-type :waypoint}])}))
      (.then #(.text %))
      (.then (fn [body]
               (reset! atom (first (cljs.reader/read-string body)))
               (alert "Message recieved")))
      (.catch (fn [reason] (alert (str "Oops" reason))))))

(defn ^:export init []
  (.registerRunnable app-registry "ReNatalSchejuler" render))
