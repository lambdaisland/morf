(ns lambdaisland.morf
  "Morf provides syntactic sugar over reagent atoms and reactions, either inside a
  component for component-local state, using [[with-form]], or at the top level
  for shared state with [[deform]]."
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
   (defmacro with-form
     "Define a new 'form' to be used within the scope of the block, typically used
  inside a Reagent component. The form itself is a reaction and can be
  dereferenced. Any symbols in the form starting with ? or ! are bound to
  ratoms, and can be individually set or read."
     {:style/indent [1]}
     [[binding formdef & {:keys [init]}] & body]
     (let [[placeholders form] (find-placeholders formdef)]
       `(reagent.core/with-let
            ~(compute-bindings binding form placeholders init)
          ~@body))))

#?(:clj
   (defmacro deform
     "Define a new 'form', bound to the given symbol (var) at the top level. The form
  itself is a reaction and can be dereferenced. Any symbols in the form starting
  with ? or ! are bound to ratoms, and can be individually set or read.

  Use `:init` to specify initial values for the ratoms."
     {:style/indent [1]}
     [binding & more]
     (let [doc (when (string? (first more)) (first more))
           [formdef & more] (if doc (rest more) more)
           {:keys [init defonce?]} more
           [placeholders form] (find-placeholders formdef)]
       `(do
          ~@(for [[bind form] (partition 2 (compute-bindings binding form placeholders init))]
              (if defonce?
                `(defonce ~(cond-> bind
                             doc
                             (with-meta {:doc doc})) ~form)
                `(def ~bind ~@(when doc [doc]) ~form)))))))


#?(:clj
   (defmacro deform-once
     "Like [[deform]], but expands to a `defonce` instead of a `def`. Good for
  maintaining state across reloads."
     {:style/indent [1]}
     [binding formdef & opts]
     `(deform ~binding ~formdef ~@opts :defonce? true)))
