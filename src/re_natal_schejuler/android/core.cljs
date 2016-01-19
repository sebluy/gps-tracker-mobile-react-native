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

(defn better-view []
  (View {:style {:margin 40}}
        (TouchableHighlight {:onPress #(alert "Hi")
                             :style {:padding 10
                                     :backgroundColor "#C0C0C0"
                                     :borderRadius 5}}
                            (Text {:style {:textAlign "center"}} "Refresh"))
        (Text {:style {:marginTop 40}} "No Waypoints")))

(defn render []
  (js.React.render (better-view) 1))

(def response (atom nil))

(-> @response)

(-> (js.fetch "https://fierce-dawn-3931.herokuapp.com/api"
              (clj->js {:method "POST"
                        :headers {:content-type "application/edn"}
                        :body (str [{:action :get-paths
                                     :path-type :waypoint}])}))
    (.then #(.body %))
    (.then #(reset! response (cljs.reader/read-string %))))

(cljs.reader/read-string @response)

(render)

(defn ^:export init []
  (.registerRunnable app-registry "ReNatalSchejuler" render))
