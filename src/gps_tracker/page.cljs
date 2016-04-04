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

(sc/defschema Home {:id (sc/eq :home)})
(sc/defschema Tracking {:id (sc/eq :tracking)
                        :state t/State})
(sc/defschema Remote {:id (sc/eq :remote)
                      :state rem/State})
(sc/defschema State (u/either Tracking
                              Remote
                              Home))


(sc/defschema Action
  (u/either (sh/list (sc/eq :remote) rem/Action)
            (sh/list (sc/eq :tracking) t/Action)
            (sc/eq '(:back))))

(sc/defn init :- State []
  {:id :home})

;; HANDLERS

(declare handle)

(sc/defn before :- State [action :- Action state :- State]
  (cond
    (= action '(:tracking :start))
    {:id :tracking
     :state {}}

    :else
    state))

(sc/defn after :- State [address action :- Action state :- State]
  (cond
    (= action '(:tracking :cleanup))
    (let [path (get-in state [:state :path])]
      (if (p/valid? path)
        {:id :remote
         :state (rem/init path)}
        (init)))

    (= action '(:remote :cleanup))
    (init)

    (= action '(:back))
    (if (= (state :id) :home)
      (js.React.BackAndroid.exitApp)
      (handle address `(~(state :id) :cleanup) state))

    :else
    state))

(sc/defn delegate :- State [address action :- Action state :- State]
  (case (first action)
    :remote
    (when (= (state :id) :remote)
      (update state :state
              (partial rem/handle
                       (a/forward address (a/tag :remote))
                       (rest action))))

    :tracking
    (when (= (state :id) :tracking)
      (update state :state
              (partial t/handle
                       (a/forward address (a/tag :tracking))
                       (rest action))))

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
   (case (state :id)
     :tracking
     (t/view (a/forward address (a/tag :tracking)) (state :state))

     :remote
     (rem/view (a/forward address (a/tag :remote)) (state :state))

     (home address))))
