(ns re-natal-schejuler.util
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
