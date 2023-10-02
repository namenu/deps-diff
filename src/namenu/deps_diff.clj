(ns namenu.deps-diff
  (:require [clojure.data :as data]
            [clojure.pprint]
            [clojure.edn :as edn]
            [clojure.set :as set]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.tools.deps :as deps]
            [clojure.tools.deps.util.maven :as mvn]
            [namenu.deps-diff.output :as output]
            [namenu.deps-diff.spec :as spec]))

;; from tools.gitlibs
(defn- run-git
  [& args]
  (let [command-args (cons "git" (map str args))]
    (let [proc-builder (ProcessBuilder. ^java.util.List command-args)
          proc         (.start proc-builder)
          exit         (.waitFor proc)
          out          (slurp (.getInputStream proc))
          err          (slurp (.getErrorStream proc))]
      {:args command-args, :exit exit, :out out, :err err})))

(defn- read-edn
  [path]
  (cond
    ;; file
    (.exists (clojure.java.io/file path))
    (edn/read-string (slurp (str path)))

    ;; git
    :else
    (let [path (if (str/ends-with? path "deps.edn")
                 path
                 (str path ":" "deps.edn"))
          sh   (run-git "show" path)]
      (when-not (zero? (:exit sh))
        (throw (ex-info (str "Couldn't fetch deps.edn from git ref " path)
                        {:output (:err sh)})))
      (edn/read-string (:out sh)))))

(defn- resolve-deps [deps aliases]
  (-> (deps/create-basis {:project (merge {:mvn/repos mvn/standard-repos} deps)
                          :aliases aliases})
      :libs
      (update-vals #(s/conform ::spec/coord %))))

(defmulti make-output (fn [_ opts] (keyword (:format opts))))

(defmethod make-output :default
  [v _]
  (prn v))

(defmethod make-output :markdown
  [data _]
  (output/markdown data))

(defmethod make-output :cli
  [data _]
  (output/cli data))

(s/def :input/aliases (s/* keyword?))

(defn diff
  "
  opts
    :base - git sha
    :target - file path
    :aliases - seq of aliases to be used creating basis
    :format - #{:edn, :markdown}
  "
  [{:keys [base target aliases] :as opts}]
  (assert (s/valid? :input/aliases aliases))
  (let [deps-from     (resolve-deps (read-edn base) aliases)
        deps-to       (resolve-deps (read-edn target) aliases)

        key-set       (comp set keys)

        [removed-deps added-deps common-deps] (data/diff (key-set deps-from) (key-set deps-to))
        modified-deps (set/union (select-keys deps-from common-deps) (select-keys deps-to common-deps))]
    (make-output
      {:removed  (into (sorted-map) (select-keys deps-from removed-deps))
       :added    (into (sorted-map) (select-keys deps-to added-deps))
       :modified (into (sorted-map) modified-deps)}
      opts)))

(comment
  ;; git show e0f4689c07bc652492bf03eba7edac20ab2bee0f:test/resources/base.edn > base.edn
  ;; clojure -X namenu.deps-diff/diff base.edn deps.edn

  (diff {:base    "test-resources/base/deps.edn"
         :target  "test-resources/target/deps.edn"
         :aliases [:test]
         :format  :repl})

  (diff {:base   "b2a1ca302959b720e703618a912a4b140389ee55" :target "deps.edn"
         :format :cli})

  (read-edn "HEAD:deps.edn")

  (resolve-deps (read-edn "HEAD:deps.edn") [])

  (s/conform ::spec/coord
             {:git/url       "https://github.com/green-labs/superlifter.git",
              :git/sha       "e0df5b36b496c485c75f38052a71b18f02772cc0",
              :deps/manifest :deps,
              :deps/root     "/Users/namenu/.gitlibs/libs/superlifter/superlifter/e0df5b36b496c485c75f38052a71b18f02772cc0",
              :dependents    ['green-labs/gosura],
              :parents       #{['green-labs/gosura]},
              :paths         ["/Users/namenu/.gitlibs/libs/superlifter/superlifter/e0df5b36b496c485c75f38052a71b18f02772cc0/src"]}
             )

  :test)
