(ns namenu.deps-diff
  (:require [clojure.data :as data]
            [clojure.edn :as edn]
            [clojure.set :as set]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.tools.deps :as deps]
            [clojure.tools.deps.specs :as deps.specs]
            [clojure.tools.deps.util.maven :as mvn]))

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
                 (str path ":" "deps.edn"))]
      (-> (run-git "show" path)
          :out
          (edn/read-string)))))

(defn- resolve-deps [deps aliases]
  (-> (deps/create-basis {:project (merge {:mvn/repos mvn/standard-repos} deps)
                          :aliases aliases})
      :libs
      (update-vals #(s/conform ::deps.specs/coord %))))

(defmulti make-output (fn [_ opts] (keyword (:format opts))))

(defmethod make-output :default
  [v _]
  (prn v))

(defn make-ver [[type coord]]
  (case type
    :mvn (:mvn/version coord)
    :local (:local/root coord)
    :git (cond-> coord
                 :git/tag
                 :git/sha)))

(defn make-row [[dep ver]]
  (prn ver)
  (str "| `" dep "` | " (make-ver ver) " |"))

(defmethod make-output :markdown
  [{:keys [removed added modified]} _]
  (let [table-header ["| Artifact            | version |"
                      "| ------------------- | ------  |"]
        lines        (concat
                       (when (seq removed)
                         (concat ["![Static Badge](https://img.shields.io/badge/Removed-red)"]
                                 table-header
                                 (map make-row removed)
                                 [""]))
                       (when (seq added)
                         (concat ["![Static Badge](https://img.shields.io/badge/Added-green)"]
                                 table-header
                                 (map make-row added)
                                 [""]))
                       (when (seq modified)
                         (concat ["![Static Badge](https://img.shields.io/badge/Modified-blue)"]
                                 table-header
                                 (map make-row modified))))]
    (println (str/join "\n" lines))))

(s/def ::aliases (s/* keyword?))

(defn diff
  "
  opts
    :base - git sha
    :target - file path
    :aliases - seq of aliases to be used creating basis
    :format - #{:edn, :markdown}
  "
  [{:keys [base target aliases] :as opts}]
  (assert (s/valid? ::aliases aliases))
  (let [deps-from     (resolve-deps (read-edn base) aliases)
        deps-to       (resolve-deps (read-edn target) aliases)

        key-set       (comp set keys)

        [removed-deps added-deps common-deps] (data/diff (key-set deps-from) (key-set deps-to))
        modified-deps (set/union (select-keys deps-from common-deps) (select-keys deps-to common-deps))]
    (make-output
      {:removed  (into (sorted-map) (select-keys deps-from removed-deps))
       :added    (into (sorted-map) (select-keys deps-to added-deps))
       :modified (into (sorted-map) (select-keys deps-to modified-deps))}
      opts)))

(comment
  ;; git show e0f4689c07bc652492bf03eba7edac20ab2bee0f:test/resources/base.edn > base.edn
  ;; clojure -X namenu.deps-diff/diff base.edn deps.edn

  (diff {:base   "bd130472de267c280d1bd04cb696fb127d0e731c" :target "deps.edn"
         :format :markdown})

  (read-edn "HEAD:deps.edn")
  (read-edn "HEAD:test/resources/base.edn")

  (resolve-deps (read-edn "HEAD:deps.edn") [])
  (resolve-deps {:deps      {'green-labs/gosura {:git/url "https://github.com/green-labs/gosura"
                                                 :git/sha "f1d586669f37a3ca99e14739f9abfb1a02128274"}}
                 :mvn/repos mvn/standard-repos
                 }
                [])

  (make-ver
    (s/conform ::deps.specs/coord
               {:git/url       "https://github.com/green-labs/superlifter.git",
                :git/sha       "e0df5b36b496c485c75f38052a71b18f02772cc0",
                :deps/manifest :deps,
                :deps/root     "/Users/namenu/.gitlibs/libs/superlifter/superlifter/e0df5b36b496c485c75f38052a71b18f02772cc0",
                :dependents    ['green-labs/gosura],
                :parents       #{['green-labs/gosura]},
                :paths         ["/Users/namenu/.gitlibs/libs/superlifter/superlifter/e0df5b36b496c485c75f38052a71b18f02772cc0/src"]}
               ))

  (defmethod make-output :repl
    [diff _]
    diff)

  (-> (diff {:base    "test-resources/base/deps.edn"
             :target  "test-resources/target/deps.edn"
             :aliases [:test]
             :format  :repl})
      ))
