(ns gps-tracker.path
  (:require [goog.string :as gstring]
            [goog.string.format]))

(def MEAN_EARTH_RADIUS 6371e3)

(defn distance-between-radians
  "Computes the distance between two latlngs.
  Latlng coordinates should be in radians.
  Result is given in meters."
  [a b]
  (let [del-lat (Math.abs (- (a :latitude) (b :latitude)))
        del-lng (Math.abs (- (a :longitude) (b :longitude)))
        d (* 2 (Math.asin (Math.sqrt (+ (Math.pow (Math.sin (/ del-lat 2)) 2)
                                        (* (Math.cos (a :latitude))
                                           (Math.cos (b :latitude))
                                           (Math.pow (Math.sin (/ del-lng 2)) 2))))))]
    (* d MEAN_EARTH_RADIUS)))

(defn degrees->radians [degrees]
  (/ (* degrees 2 Math.PI) 360))

(defn point->radians [point]
  (-> point
      (update :latitude degrees->radians)
      (update :longitude degrees->radians)))

(defn distance-between [[a b]]
  (distance-between-radians (point->radians a) (point->radians b)))

(defn total-distance [{:keys [points]}]
  "Returns total distance in meters of path, where straight line segments
   are used between points."
  (->> points
       (partition 2 1)
       (map distance-between)
       (reduce +)))

(defn waypoint-attributes [path]
  {:distance (gstring/format "%.2fm" (total-distance path))
   :count (count (path :points))})
