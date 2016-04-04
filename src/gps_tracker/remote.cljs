(ns gps-tracker.remote
  (:require [cljs.reader :as reader]
            [gps-tracker.styles :as st]
            [gps-tracker.react :as r]
            [gps-tracker.util :as u]
            [gps-tracker-common.schema-helpers :as sh]
            [gps-tracker-common.schema :as cs]
            [schema.core :as s]))

(defn post [action on-success on-failure]
  (-> (js.fetch "https://fierce-dawn-3931.herokuapp.com/api"
                (clj->js {:method "POST"
                          :headers {:content-type "application/edn"}
                          :body (str [action])}))
      (.then #(.text %))
      (.then (fn [body]
               (on-success (first (reader/read-string body)))))
      (.catch (fn [reason]
                (on-failure)))))

(s/defschema State {:path cs/TrackingPath
                    :status (s/enum :fresh :pending :success :failure)})

(defn init [path]
  {:path path
   :status :fresh})

(s/defschema Send (s/eq '(:send)))
(s/defschema Failure (s/eq '(:failure)))
(s/defschema Success (s/eq '(:success)))
(s/defschema Cleanup (s/eq '(:cleanup)))

(s/defschema Action
  (u/either Send
            Failure
            Success
            Cleanup))

(s/defn handle :- State [address action :- Action state :- State]
  (case (first action)

    :send
    (do
      (post {:action :add-path
             :path-type :tracking
             :path (state :path)}
            #(address '(:success))
            #(address '(:failure)))
      (assoc state :status :pending))

    :failure
    (assoc state :status :failure)

    :success
    (assoc state :status :success)

    state))

(defn upload-message [status]
  (if (= status :failure)
    "Upload failed. Retry?"
    "Upload?"))

(defn upload-view [address state]
  (r/view
   nil
   (r/text {:style st/title} (upload-message (state :status)))
   (u/button "Yes" #(address '(:send)))
   (u/button "No" #(address '(:cleanup)))))

(defn done-view [address]
  (r/text {:style st/title} "Success"))

(defn pending-upload-view []
  (r/view
   nil
   (r/text {:style st/title} "Please wait...")
   (r/progress-bar nil)))

(defn view [address state]
  (case (state :status)
    :pending
    (pending-upload-view)

    :success
    (done-view address)

    (upload-view address state)))
