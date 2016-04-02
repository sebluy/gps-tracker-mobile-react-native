(ns gps-tracker.util
  (:require [clojure.string :as str]
            [schema.core :as sc]
            [gps-tracker.react :as r]
            [gps-tracker.styles :as sty]))

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

(defn key->title [key]
  (-> key name str/capitalize))

(defn attributes->str [attrs]
  (str/join "\n" (map (fn [[key value]]
                      (str (key->title key) ": " value))
                    attrs)))

(defn time-field->string [n]
  (if (< n 10)
    (str "0" n)
    (str n)))

(def sec 1000)
(def minute (* 60 sec))
(def hour (* 60 minute))

(defn duration-str [ms]
  (let [s (mod (js/Math.floor (/ ms sec)) 60)
        m (mod (js/Math.floor (/ ms minute)) 60)
        h (mod (js/Math.floor (/ ms hour)) 24)]
    (str/join ":" (map time-field->string [h m s]))))

(defn duration [t1 t2]
  (- (.getTime t2) (.getTime t1)))

(defn either [& schemas]
  (sc/pred
   (fn [val]
     (some #(nil? (sc/check % val)) schemas))))

(defn now []
  (js/Date.))

(defn button [text on-press]
  (r/touchable-highlight
   {:onPress on-press
    :style sty/button}
   (r/text
    {:style sty/button-text}
    text)))

(def mile 1609.34)

(defn miles
  "Converts meters to miles"
  [meters]
  (/ meters mile))

(defn mph
  "Converts meters/second to miles/hour"
  [mps]
  (/ (* mps (/ hour sec)) mile))
