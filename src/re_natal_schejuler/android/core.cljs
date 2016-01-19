(ns ^:figwheel-load re-natal-schejuler.android.core)

(set! js/React (js/require "react-native/Libraries/react-native/react-native.js"))

(def app-registry js/React.AppRegistry)

(defn alert [title]
  (.alert (.-Alert js/React) title))

(def Text (.-Text js/React))
(def View (.-View js/React))

(defn better-view []
  [View {:style {:margin 40}}
   [Text {} "Hello"]
   [Text {} "Man"]])

(defn tree->components [tree]
  (if (vector? tree)
    (let [[type props & children] tree]
      (apply
       js/React.createElement
       type
       (clj->js props)
       (map tree->components children)))
    (clj->js tree)))

(defn render []
  (js/React.render (tree->components (better-view)) 1))

(render)

(defn ^:export init []
  (.registerRunnable app-registry "ReNatalSchejuler" render))
