(ns re-natal-schejuler.react
  (:require [re-natal-schejuler.quiescent :as q]))

(set! js.React (js.require "react-native/Libraries/react-native/react-native.js"))

(set! js.React.ProgressBar (js.require "ProgressBarAndroid"))
(set! js.React.Toolbar (js.require "ToolbarAndroid"))

(def app-registry js.React.AppRegistry)

(def text (q/constructor js.React.Text))
(def image (q/constructor js.React.Image))
(def touchable-highlight (q/constructor js.React.TouchableHighlight))
(def view (q/constructor js.React.View))
(def list-view (q/constructor js.React.ListView))
(def progress-bar (q/constructor js.React.ProgressBar))
(def toolbar (q/constructor js.React.Toolbar))
(def scroll-view (q/constructor js.React.ScrollView))

(defn alert
  ([title] (alert title nil))
  ([title msg]
   (.alert (.-Alert js.React) title msg)))

(defn simple-datasource [vals]
  (let [ds (js.React.ListView.DataSource.
            #js {:rowHasChanged (fn [r1 r2] (not= r1 r2))})]
    (.cloneWithRows ds (clj->js vals))))
