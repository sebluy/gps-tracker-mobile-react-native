(ns re-natal-schejuler.quiescent
  (:require [cljsjs.react])
  (:require-macros [re-natal-schejuler.quiescent :refer [react-method]]))

(def ^:dynamic *component*
  "Within a component lifecycle function, is be bound to the underlying ReactJS instance." nil)

(def ^:private lifecycle-impls
  "Mapping of public lifecycle API to internal ReactJS API."
  (let [basic (fn [impl]
                (react-method []
                  (apply impl
                    (.-value (.-props *component*))
                    (.-constants (.-props *component*)))))
        with-old-value (fn [impl]
                         (react-method [prev-props _]
                           (apply impl
                             (.-value (.-props *component*))
                             (.-value prev-props)
                             (.-constants (.-props *component*)))))
        with-nil-old-value (fn [impl]
                             (react-method []
                               (apply impl
                                 (.-value (.-props *component*))
                                 nil
                                 (.-constants (.-props *component*)))))
        with-callback (fn [impl]
                        (react-method [cb]
                          (apply impl
                            cb
                            (.-value (.-props *component*))
                            (.-constants (.-props *component*)))))]
    {:on-mount {:componentDidMount basic}
     :on-update {:componentDidUpdate with-old-value}
     :on-unmount {:componentWillUnmount basic}
     :on-render {:componentDidUpdate with-old-value
                 :componentDidMount with-nil-old-value}
     :will-enter {:componentWillEnter with-callback}
     :did-enter  {:componentDidEnter basic}
     :will-leave {:componentWillLeave with-callback}
     :did-leave {:componentDidLeave basic}}))

(defn- build-lifecycle-impls
  [opts-map]
  (reduce (partial merge-with
            (fn [_ _]
              (throw "Component definition should not provide handlers for both :on-render and (:on-mount | :on-update).")))
    (map (fn [[key impl]]
           (when-let [impl-map (lifecycle-impls key)]
             (into {} (for [[method impl-ctor] impl-map]
                        [method (impl-ctor impl)]))))
      opts-map)))

(defn component
  "Return a factory function that will create a ReactElement, using the provided function as the
  'render' method for a ReactJS component, which is created and instantiated behind-the-scenes.
  The given render function should take a single immutable value as its first argument, and return
  a single ReactElement. Additional arguments to the returned factory function are
  /constant arguments/  which will be passed on as additional arguments to the  supplied render
  function, but will *not* be included in any calculations regarding whether the element should
  re-render. As such, they are suitable for values that will remain constant for  the lifetime of
  the rendered element, such as message channels and configuration objects.
  The optional 'opts' argument is a map which contains additional configuration keys:
     :keyfn - a single-argument function which is invoked at component construction time. It is
     passed the component's value, and returns the ReactJS key used to uniquely identify this
     component among its children.
     :name - the name of the element, used for debugging purposes.
     :on-mount - A function which will be invoked once, immediately after initial rendering occurs.
     It is passed the DOM node, the value and any constant args passed to the render fn. This maps
     to the 'componentDidMount' lifecycle method in ReactJS.
     :on-update - A function which will be invoked immediately after an update is flushed to the DOM,
     but not on the initial render. It is is passed the underlying DOM node, the value,
     the _old_ value, and any constant args passed to the render fn. This maps to the
     'componentDidUpdate' lifecycle method in ReactJS.
     :on-unmount - A function which will be invoked immediately before a the component is unmounted
     from the DOM. It is passed the underlying DOM node, the most recent value and the most recent
     constant args passed to the render fn. This maps to the 'componentWillUnmmount' lifecycle
     method in ReactJS.
     :on-render - A function which will be invoked immediately after the DOM is updated, both on the
     initial render and any subsequent updates. It is is passed the underlying DOM node, the
     value, the _old_  value (which will be nil in the case of the initial render) and any constant
     args passed to the render fn. This maps to both the 'componentDidMount' and
     'componentDidUpdate' lifecycle methods in ReactJS.
     :will-enter - A function invoked whenever this component is added to a ReactTransitionGroup.
     Invoked at the same time as :onMount. Is passed the underlying DOM node, a callback
     function, the value and any constant args passed to the render fn. Maps to the
     'componentWillEnter' lifecycle  method in ReactJS. See the ReactJS documentation at
     http://facebook.github.io/react/docs/animation.html for full documentation of the behavior.
     :did-enter - A function invoked after the callback provided to :willEnter is called. It is
     passed the underlying DOM node, the value and any constant args passed to the render fn. Maps
     to the 'componentDidEnter' lifecycle method in ReactJS. See the ReactJS documentation at
     http://facebook.github.io/react/docs/animation.html for full documentation of the behavior.
     :will-leave - A function invoked whenever this component is removed from a ReactTransitionGroup.
     Is passed the underlying DOM node, a callback function, the most recent value and the most
     recent constant args passed to the render fn. The DOM node will not be removed until the
     callback is called. Maps to the 'componentWillEnter' lifecycle method in ReactJS. See the
     ReactJS documentation at http://facebook.github.io/react/docs/animation.html for full
     documentation of the behavior.
     :did-leave - A function invoked after the callback provided to :willLeave is called (at the same
     time as :onUnMount). Is passed the underlying DOM node, the most recent value and the most
     recent constant args passed to the render fn. Maps to the 'componentDidLeave' lifecycle method
     in ReactJS. See the ReactJS  documentation at
     http://facebook.github.io/react/docs/animation.html for full documentation of the behavior.
  The *component* dynamic var will be bound to the underlying ReactJS component for all invocations
  of the render function and invocations of functions defined in the opts map."
  ([renderer] (component renderer {}))
  ([renderer opts]
    (let [impl (merge
                 (when (:name opts) {:displayName (:name opts)})
                 {:shouldComponentUpdate (react-method [next-props _]
                                           (not= (.-value (.-props *component*))
                                                 (.-value next-props)))
                  :render (react-method []
                            (apply renderer
                              (.-value (.-props *component*))
                              (.-constants (.-props *component*))))}
                 (build-lifecycle-impls opts))
          react-component (.createClass js/React (clj->js impl))]
      (fn [value & constant-args]
        (let [props (js-obj)]
          (set! (.-value props) value)
          (set! (.-constants props) constant-args)
          (when-let [keyfn (:keyfn opts)]
            (set! (.-key props) (keyfn value)))
          (.createElement js/React react-component props))))))

(defn unmount
  "Remove a mounted Element from the given DOM node."
  [node]
  (.unmountComponentAtNode js/React node))

(defn constructor
  "Return a DOM node constructor function. The argument may be any
  value accepted by React.createElement (that is, the string name of a
  HTML tag, or an instance of ReactClass).
  Returns a function that takes props and children (the same as the
  built-in ReactJS element constructors)."
  [type]
  (fn [props & children]
    (apply js/React.createElement type (clj->js props) children)))
