(ns re-natal-schejuler.state)

(defonce state (atom {}))

(defn handle [handler-fn & args]
  (swap! state (fn [state] (apply handler-fn state args))))
