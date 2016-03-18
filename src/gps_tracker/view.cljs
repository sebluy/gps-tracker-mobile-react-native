(ns gps-tracker.view
  (:require [gps-tracker.state :as s]
            [gps-tracker.react :as r]
            [gps-tracker.path :as path]
            [gps-tracker.quiescent :as q]
            [gps-tracker.handlers :as h]
            [gps-tracker.util :as u]))

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
     :page {:margin 40}})))

(q/defcomponent WaypointPathRow
  [id]
  (r/touchable-highlight
   {:onPress #(s/handle h/show-waypoint-path id)
    :style styles.button}
   (r/text
    {:style styles.text}
    (u/date->string id))))

(q/defcomponent RefreshButton
  []
  (r/touchable-highlight
   {:onPress #(s/handle h/get-waypoint-paths)
    :style [styles.button styles.narrowButton styles.marginVertical]}
   (r/text
    {:style styles.text}
    "Refresh")))

(q/defcomponent WaypointPathList
  [waypoint-paths]
  (cond (= waypoint-paths :pending)
        (r/progress-bar
         {:style [styles.marginVertical]
          :styleAttr "Inverse"})
        (seq waypoint-paths)
        (r/scroll-view
         {:style [styles.scrollView styles.marginVertical]}
         (r/list-view
          {:dataSource (r/simple-datasource (map :id waypoint-paths))
           :renderRow WaypointPathRow}))
        :else
        (r/text
         {:style [styles.text styles.marginVertical]}
         "No Waypoints")))

(q/defcomponent WaypointPathsPage
  [state]
  (r/view
   {:style styles.page}
   (r/text
    {:style [styles.text styles.bigText styles.marginVertical]}
    "Waypoint Paths")
   (RefreshButton)
   (WaypointPathList (state :waypoint-paths))))

(defn get-path [state id]
  (->> (state :waypoint-paths)
       (filter #(= id (% :id)))
       first))

(q/defcomponent WaypointPathPage
  [state]
  (let [id (get-in state [:page :path-id])
         path-attrs (path/waypoint-attributes (get-path state id))]
    (r/view
     {:style styles.page}
     (r/text
      {:style [styles.text styles.marginVertical styles.bigText]}
      (u/date->string id))
     (r/text
      {:style [styles.text styles.marginVertical]}
      (str "Count: " (path-attrs :count)))
     (r/text
      {:style [styles.text styles.marginVertical]}
      (str "Distance: " (path-attrs :distance))))))

(q/defcomponent Page
  [state]
  (condp = (get-in state [:page :id])
    :waypoint-path-list (WaypointPathsPage state)
    :show-waypoint-path (WaypointPathPage state)
    (r/text {} "Nothing here")))

(q/defcomponent Main
  [state]
  (r/view
   {}
   (r/toolbar
    {:title "GPSTracker"
     :style styles.toolbar})
   (Page state)))
