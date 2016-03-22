(ns gps-tracker.util
  (:require [clojure.string :as s]))

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
  (-> key name s/capitalize))

(defn attributes->str [attrs]
  (s/join "\n" (map (fn [[key value]]
                      (str (key->title key) ": " value))
                    attrs)))

(defn time-field->string [n]
  (if (< n 10)
    (str "0" n)
    (str n)))

(defn duration-str [ms]
  (let [s (mod (js/Math.floor (/ ms 1000)) 60)
        m (mod (js/Math.floor (/ ms 60000)) 60)
        h (mod (js/Math.floor (/ ms (* 60 60 1000))) 24)]
    (s/join ":" (map time-field->string [h m s]))))

(defn duration [t1 t2]
  (- (.getTime t2) (.getTime t1)))
