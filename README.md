# morf

<!-- badges -->
[![cljdoc badge](https://cljdoc.org/badge/com.lambdaisland/morf)](https://cljdoc.org/d/com.lambdaisland/morf) [![Clojars Project](https://img.shields.io/clojars/v/com.lambdaisland/morf.svg)](https://clojars.org/com.lambdaisland/morf)
<!-- /badges -->

Morf is a state / "form" utility library for Reagent.

## Features

<!-- installation -->
## Installation

To use the latest release, add the following to your `deps.edn` ([Clojure CLI](https://clojure.org/guides/deps_and_cli))

```
com.lambdaisland/morf {:mvn/version "0.3.14"}
```

or add the following to your `project.clj` ([Leiningen](https://leiningen.org/))

```
[com.lambdaisland/morf "0.3.14"]
```
<!-- /installation -->

## Rationale

`with-form` / `deform` macros for Reagent.

If you've ever worked in reagent for a long enough time, there is one piece of
glue which is really missing.

A way to create and hold state into buckets. Re-frame choose to go about solving
this problem by having a single global state, and then using it's conventions to
update that state. But even re-frame will leave you stranded when it come's to
organising local component state.

Now something like re-frame (and other home grown solutions following the same path):
1. Global state and managing it requires creation of either multiple functions, reactions, cursors etc.
2. Local component state is also littered with `reagent/atom` and
   `reagent/cursor` calls. In the age of react, we seem to have forgotten the
   simplicity of the html `<form>` which acted a way to house some state
   together.

Now `morf` tries to solve both of these problems.

To reduce the ceremony, what if you we could wrap the good 'ol reagent atoms
with some sugar? (Check `deform` usage below)

And for local component state, well you would say that some of this is the job
of `reagent/with-let`. And I would certainly agree. For state which is
absolutely local to a component, this makes a lot of sense. But even then
managing local state cleanly often ends up with creating loads of atoms and
reactions locally. (Check `with-form` usage below)

And when state needs to be passed down, around, up and about! In those cases we
need to remove state from the component heirarchy and house it at the top level.

## Usage

```clojure
(ns ...
  (require [lambdaisland.morf :as morf])
  
(morf/deform !config
  {:question !question
   :answer-to-life 42
   :answer !answer}
  {:init {!question "What's your name?"}
   :defonce? false})
   
(defn ask-question-reagent-component []
  [:div
    [:input {:on-change (fn [e] (reset! !question (.. e -target -value)))}]
    [:p @!question]
    [:p @!answer]])
```

Using the `deform` macro allows the creation of multiple reagent atoms, and reactions.

Every symbol inside of the body starting with a `!` or `?` will be converted into a reagent atom.

The top level binding (`!config` in this case) will be a reagent reaction which
updates whenever any of the nested atoms update.

The above macro will roughly produce code like this:
```clojure
(def !question (reagent/atom "What's your name?"))
(def !answer (reagent/atom nil))
(def !config (reagent/reaction {:question !question :answer !answer :answer-to-life 42}))
```

Here `!config` reaction has been made to implement the `ILookup` protocol, so you can directly do:

```clojure
(:answer-to-life @!config)
```

For local components imagine you have a component as follows:

```
(defn ask-question-component []
  (reagent/with-let [!form (reagent/atom {})
                     !age (reagent/cursor !form [:age])
                     !first-name (reagent/cursor !form [:first-name])
                     !last-name (reagent/cursor !form [:last-name])
                     !full-name (reagent/reaction (str @!first-name @!last-name))
                     !spinner-timeout (reagent/atom 10)]
    ...))
```

Wouldn't it be nice if you could something like this:
```
(defn ask-question-component []
  (morf/with-form [!form {:age !age
                          :first-name !first-name
                          :last-name !last-name
                          ;; (:full-name @!form) will be automatically converted into a reaction
                          :full-name (str !first-name !last-name)
                          :spinner-timeout 10}]
    ...)) 
```

<!-- opencollective -->
## Lambda Island Open Source

Thank you! morf is made possible thanks to our generous backers. [Become a
backer on OpenCollective](https://opencollective.com/lambda-island) so that we
can continue to make morf better.

<a href="https://opencollective.com/lambda-island">
<img src="https://opencollective.com/lambda-island/organizations.svg?avatarHeight=46&width=800&button=false">
<img src="https://opencollective.com/lambda-island/individuals.svg?avatarHeight=46&width=800&button=false">
</a>
<img align="left" src="https://github.com/lambdaisland/open-source/raw/master/artwork/lighthouse_readme.png">

&nbsp;

morf is part of a growing collection of quality Clojure libraries created and maintained
by the fine folks at [Gaiwan](https://gaiwan.co).

Pay it forward by [becoming a backer on our OpenCollective](http://opencollective.com/lambda-island),
so that we continue to enjoy a thriving Clojure ecosystem.

You can find an overview of all our different projects at [lambdaisland/open-source](https://github.com/lambdaisland/open-source).

&nbsp;

&nbsp;
<!-- /opencollective -->

<!-- contributing -->
## Contributing

We warmly welcome patches to morf. Please keep in mind the following:

- adhere to the [LambdaIsland Clojure Style Guide](https://nextjournal.com/lambdaisland/clojure-style-guide)
- write patches that solve a problem 
- start by stating the problem, then supply a minimal solution `*`
- by contributing you agree to license your contributions as MPL 2.0
- don't break the contract with downstream consumers `**`
- don't break the tests

We would very much appreciate it if you also

- update the CHANGELOG and README
- add tests for new functionality

We recommend opening an issue first, before opening a pull request. That way we
can make sure we agree what the problem is, and discuss how best to solve it.
This is especially true if you add new dependencies, or significantly increase
the API surface. In cases like these we need to decide if these changes are in
line with the project's goals.

`*` This goes for features too, a feature needs to solve a problem. State the problem it solves first, only then move on to solving it.

`**` Projects that have a version that starts with `0.` may still see breaking changes, although we also consider the level of community adoption. The more widespread a project is, the less likely we're willing to introduce breakage. See [LambdaIsland-flavored Versioning](https://github.com/lambdaisland/open-source#lambdaisland-flavored-versioning) for more info.
<!-- /contributing -->

<!-- license -->
## License

Copyright &copy; 2023 Arne Brasseur and Contributors

Licensed under the term of the Mozilla Public License 2.0, see LICENSE.
<!-- /license -->
