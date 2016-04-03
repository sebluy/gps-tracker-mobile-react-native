(ns gps-tracker.page
  (:require [gps-tracker.react :as r]
            [gps-tracker.remote :as rem]
            [gps-tracker.path :as p]
            [gps-tracker.tracking :as t]
            [gps-tracker.address :as a]
            [gps-tracker.util :as u]
            [gps-tracker-common.schema-helpers :as sh]
            [schema.core :as sc]
            [gps-tracker.styles :as st]))

;; SCHEMAS

(sc/defschema Home {:page (sc/eq :home)})
(sc/defschema State (u/either t/State rem/State Home))

(sc/defschema Action
  (u/either (sh/list (sc/eq :remote) rem/Action)
            (sh/list (sc/eq :tracking) t/Action)
            (sc/eq '(:back))))

(sc/defn init :- State []
  {:page :home})

;; HANDLERS

(declare handle)

(sc/defn before :- State [action :- Action state :- State]
  (cond
    (= action '(:tracking :start))
    (assoc state :page :tracking)

    :else
    state))


(sc/defn after :- State [address action :- Action state :- State]
  (cond
    (= action '(:tracking :cleanup))
    (let [path (state :path)]
      (if (p/valid? path)
        (rem/init path)
        (init)))

    (= action '(:remote :cleanup))
    (init)

    (= action '(:back))
    (if (= (state :page) :home)
      (js.React.BackAndroid.exitApp)
      (handle address `(~(state :page) :cleanup) state))

    :else
    state))

(sc/defn delegate :- State [address action :- Action state :- State]
  (case (first action)
    :remote
    (when (= (state :page) :remote)
      (rem/handle (a/forward address (a/tag :remote)) (rest action) state))

    :tracking
    (when (= (state :page) :tracking)
      (t/handle (a/forward address (a/tag :tracking)) (rest action) state))

    state))

(sc/defn handle :- State [address action :- Action state :- State]
  (->> state
       (before action)
       (delegate address action)
       (after address action)))

;; VIEW

(defn home [address]
  (u/button "Start Tracking" #(address `(:tracking :start))))

(defn view [address state]
  (r/view
   {:style st/main}
   (case (state :page)
     :tracking
     (t/view (a/forward address (a/tag :tracking)) state)

     :remote
     (rem/view (a/forward address (a/tag :remote)) state)

     (home address))))
