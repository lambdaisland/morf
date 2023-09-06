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

(defn- compute-bindings [binding form placeholders init]
  (let [lookup (gensym "lookup")]
    (conj (into []
                (mapcat (fn [p]
                          [p `(reagent.core/atom ~(get init p))])
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
              ([_# k# not-found#] (~'-lookup ~lookup k# not-found#)))))))

#?(:clj
   (defmacro with-form [[binding formdef & {:keys [init]}] & body]
     (let [[placeholders form] (find-placeholders formdef)]
       `(reagent.core/with-let
          ~(compute-bindings binding form placeholders init)
          ~@body))))

#?(:clj
   (defmacro deform [binding formdef & {:keys [init]}]
     (let [[placeholders form] (find-placeholders formdef)]
       `(do
          ~@(for [[bind form] (partition 2 (compute-bindings binding form placeholders init))]
              `(def ~bind ~form))
          ))))
