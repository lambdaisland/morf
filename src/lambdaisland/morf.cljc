(ns lambdaisland.morf
  (:require [clojure.walk :as walk])
  #?(:cljs (:require-macros [lambdaisland.morf])))

(defn- placeholder? [o]
  (and (symbol? o)
       (#{\? \!} (first (name o)))))

(defn- find-placeholders [formdef]
  (let [p (atom #{})
        form (walk/postwalk
              (fn [o]
                (if (placeholder? o)
                  (do
                    (swap! p conj o)
                    (list `deref o))
                  o))
              formdef)]
    [@p form]))

(defprotocol IMorf
  (reinit! [r]))

(defn- compute-bindings [binding form placeholders init]
  (let [lookup (gensym "lookup")]
    (conj (into []
                (mapcat (fn [p]
                          [p `(let [init# ~(get init p)]
                                (cljs.core/specify! (reagent.core/atom init#)
                                  IMorf
                                  (~'reinit! [this#] (reset! this# init#))))])
                        placeholders))

          lookup
          (zipmap
           (map (comp keyword str) placeholders)
           placeholders)

          binding
          `(cljs.core/specify! (reagent.core/reaction ~form)
             cljs.core/ILookup
             (~'-lookup
              ([_# k#] (~'-lookup ~lookup k#))
              ([_# k# not-found#] (~'-lookup ~lookup k# not-found#)))
             IMorf
             (~'reinit! [_#]
              (doseq [p# ~(into [] placeholders)]
                (reinit! p#)))))))

#?(:clj
   (defmacro with-form [[binding formdef & {:keys [init]}] & body]
     (let [[placeholders form] (find-placeholders formdef)]
       `(reagent.core/with-let
          ~(compute-bindings binding form placeholders init)
          ~@body))))

#?(:clj
   (defmacro deform [binding formdef & {:keys [init defonce?]}]
     (let [[placeholders form] (find-placeholders formdef)]
       `(do
          ~@(for [[bind form] (partition 2 (compute-bindings binding form placeholders init))]
              (if defonce?
                `(defonce ~bind ~form)
                `(def ~bind ~form)))))))

