(ns ^:figwheel-no-load re-natal-schejuler.android.core
  (:require [re-natal-schejuler.react :as r]
            [re-natal-schejuler.quiescent :as q]
            [re-natal-schejuler.state :as state]
            [re-natal-schejuler.handlers :as h]
            [re-natal-schejuler.util :as util]))

(def GREY "#C0C0C0")

(def styles
  (js.React.StyleSheet.create
   (clj->js
    {:button {:padding 5
              :margin 5
              :borderRadius 5
              :backgroundColor GREY}
     :narrowButton {:marginHorizontal 40}
     :text {:textAlign "center"}
     :bigText {:fontSize 18}
     :toolbar {:height 56
               :backgroundColor GREY}
     :scroll-view {:height 300}
     :marginVertical {:marginVertical 10}
     :main {:margin 40}})))

(defn waypoint-path-row [id]
  (r/touchable-highlight
   {:onPress #(state/handle h/show-waypoint-path id)
    :style styles.button}
   (r/text
    {:style styles.text}
    (util/date->string id))))

(q/defcomponent View
  :on-mount #(state/handle h/get-waypoint-paths)
  [state]
  (r/view
   {}
   (r/toolbar
    {:title "GPSTracker"
     :style styles.toolbar})
   (r/view
    {:style styles.main}
    (r/text
     {:style [styles.text styles.bigText styles.marginVertical]}
     "Waypoint Paths")
    (r/touchable-highlight
     {:onPress #(state/handle h/get-waypoint-paths)
      :style [styles.button styles.narrowButton styles.marginVertical]}
     (r/text
      {:style styles.text}
      "Refresh"))
    (if (and (not= (state :waypoint-paths) :pending) (seq (state :waypoint-paths)))
      (r/scroll-view
       {:style [styles.scrollView styles.marginVertical]}
       (r/list-view
        {:dataSource (r/simple-datasource (map :id (state :waypoint-paths)))
         :renderRow waypoint-path-row}))
      (if (= (state :waypoint-paths) :pending)
        (r/progress-bar
         {:style [styles.marginVertical]
          :styleAttr "Inverse"})
        (r/text
         {:style [styles.text styles.marginVertical]}
         "No Waypoints"))))))

(defn render [state]
  (js.React.render (View state) 1))

(defn mount-root []
  (render @state/state))

;(mount-root)
;(js.React.render (js.React.createElement js.React.Text nil "Cleared") 1)

(defn ^:export init []
  (add-watch state/state :render
             (fn [_ _ _ new-state]
               (render new-state)))
  (.registerRunnable r/app-registry "ReNatalSchejuler" render))
