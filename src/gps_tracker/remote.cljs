(ns gps-tracker.remote
  (:require [cljs.reader :as reader]
            [gps-tracker.react :as r]
            [gps-tracker.remote :as rem]
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
               (on-success (first (reader/read-string body)))
               (.catch (fn [reason]
                         (on-failure)))))))

(s/defschema State {:page :remote
                    :path cs/TrackingPath
                    :pending? s/Bool
                    :failed? s/Bool})
(defn init [path]
  {:page :remote
   :path path
   :pending? false
   :failed? false})

(s/defschema AskToUpload
  (sh/list '(:ask-to-upload) (sh/singleton cs/TrackingPath)))
(s/defschema Send (s/eq '(:send)))
(s/defschema Failure (s/eq '(:failure)))
(s/defschema Success (s/eq '(:success)))
(s/defschema Cleanup (s/eq '(:cleanup)))

(s/defschema Action
  (either AskToUpload
          Send
          Failure
          Success
          Cleanup))

(s/defn handle :- State [action :- Action state :- State]
  (case (first action)

    :ask-to-upload
    (let [path (last action)]
      (init path))

    :send
    (do
      (post {:action :add-path
                 :path-type :tracking
                 :path (state :path)}
                #(address '(:remote :success))
                #(address '(:remote :failure)))
      (assoc state :pending? true))

    :failure
    (assoc state
           :failed? true
           :pending? false)

    state))

(defn upload-message [failed?]
  (if failed?
    ("Upload failed. Try Again?")
    ("Upload?")))

(defn upload-view [address state]
  (r/view
   nil
   (r/text nil (upload-message (state :failed?)))
   (button "Yes" #(address '(:remote :send)))
   (button "No" #(address '(:remote :cleanup)))))

(defn pending-upload-view []
  (r/progress-bar nil))

(defn view [address state]
  (if (state :pending?)
    (pending-upload-view)
    (upload-view address state)))
