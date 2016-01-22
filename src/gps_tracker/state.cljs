(ns gps-tracker.state)

(defonce state (atom {}))

(defn handle [handler-fn & args]
  (swap! state (fn [state] (apply handler-fn state args))))
