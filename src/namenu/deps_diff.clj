(ns namenu.deps-diff
  (:require [clojure.data :as data]
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

(defmulti print-result! (fn [_ opts] (keyword (:format opts))))

(defmethod print-result! :default
  [v _]
  (prn v))

(defmethod print-result! :markdown
  [data _]
  (output/markdown data))

(defmethod print-result! :cli
  [data _]
  (output/cli data))

(defn diff*
  [{:keys [base target aliases]}]
  (let [deps-from (resolve-deps (read-edn base) aliases)
        deps-to   (resolve-deps (read-edn target) aliases)

        key-set   (comp set keys)

        [removed-deps added-deps common-deps] (data/diff (key-set deps-from) (key-set deps-to))]
    ;; don't need to sort here
    {:removed  (into (sorted-map) (select-keys deps-from removed-deps))
     :added    (into (sorted-map) (select-keys deps-to added-deps))
     :modified (into (sorted-map) (set/difference (set (select-keys deps-to common-deps))
                                                  (select-keys deps-from common-deps)))}))

(defn diff
  "
  opts
    :base - git sha
    :target - file path
    :aliases - seq of aliases to be used creating basis
    :format - #{:edn, :markdown, :cli}
  "
  [{:keys [base target aliases format] :as opts}]
  (assert (s/valid? ::spec/ref base))
  (assert (s/valid? ::spec/ref target))
  (assert (s/valid? ::spec/aliases aliases))
  (assert (s/valid? ::spec/format format))

  (let [d         (diff* opts)
        exit-code (if (output/equal? d) 0 1)]
    (print-result! d opts)
    (System/exit exit-code)))

(comment
  ;; git show e0f4689c07bc652492bf03eba7edac20ab2bee0f:test/resources/base.edn > base.edn
  ;; clojure -X namenu.deps-diff/diff base.edn deps.edn

  (diff* {:base "HEAD" :target "deps.edn" :format :cli})
  (diff* {:base    "test-resources/base/deps.edn"
         :target  "test-resources/target/deps.edn"
         :aliases [:poly]})

  (diff* {:base   "b2a1ca302959b720e703618a912a4b140389ee55" :target "deps.edn"
         :format :cli})

  (read-edn "HEAD:deps.edn")

  (resolve-deps (read-edn "HEAD:deps.edn") [])
  (resolve-deps {:deps      {'green-labs/gosura {:git/url "https://github.com/green-labs/gosura"
                                                 :git/sha "f1d586669f37a3ca99e14739f9abfb1a02128274"}}
                 :mvn/repos mvn/standard-repos
                 }
                [])

  (make-ver
    (s/conform ::spec/coord
               {:git/url       "https://github.com/green-labs/superlifter.git",
                :git/sha       "e0df5b36b496c485c75f38052a71b18f02772cc0",
                :deps/manifest :deps,
                :deps/root     "/Users/namenu/.gitlibs/libs/superlifter/superlifter/e0df5b36b496c485c75f38052a71b18f02772cc0",
                :dependents    ['green-labs/gosura],
                :parents       #{['green-labs/gosura]},
                :paths         ["/Users/namenu/.gitlibs/libs/superlifter/superlifter/e0df5b36b496c485c75f38052a71b18f02772cc0/src"]}
               )))
