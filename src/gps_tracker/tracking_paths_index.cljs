(ns gps-tracker.tracking-paths-index
  (:require [gps-tracker.styles :as st]
            [gps-tracker.react :as r]))

(defn view [paths]
  (if (empty? paths)
    (r/text {:style st/title} "No tracking paths.")
    (r/text {:style st/title} "Tracking paths index unimplemented.")))
