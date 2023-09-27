(ns namenu.deps-diff
  (:require [clojure.data :as data]
            [clojure.edn :as edn]
            [clojure.set :as set]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.tools.deps :as deps]
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
      (update-vals :mvn/version)))

(def ^:private key-set (comp set keys))

(defmulti make-output (fn [_ opts] (keyword (:format opts))))

(defmethod make-output :default
  [v _]
  (prn v))

"" "
table)
| dependency | version |
| ------- | -------- |
| Coke    | 100      |
| Fanta   | 10000000 |
| Lilt    | 1        |
" ""
(defmethod make-output :markdown
  [{:keys [removed added modified]} _]
  (let [table-header ["| dependency | version |"
                      "| ---------- | ------- |"]
        table-row    (fn [[dep ver]]
                       (str "| " dep " | " ver " |"))
        lines        (concat
                       (when (seq removed)
                         (concat ["![Static Badge](https://img.shields.io/badge/Removed-red)"]
                                 table-header
                                 (map table-row removed)
                                 [""]))
                       (when (seq added)
                         (concat ["![Static Badge](https://img.shields.io/badge/Added-green)"]
                                 table-header
                                 (map table-row added)
                                 [""]))
                       (when (seq modified)
                         (concat ["![Static Badge](https://img.shields.io/badge/Modified-blue)"]
                                 table-header
                                 (map table-row modified))))]
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
  (let [deps-from (resolve-deps (read-edn base) aliases)
        deps-to   (resolve-deps (read-edn target) aliases)
        [removed-deps added-deps common-deps] (data/diff (key-set deps-from) (key-set deps-to))
        modified-deps (set/union (select-keys deps-from common-deps) (select-keys deps-to common-deps))]
    (make-output
      {:removed  (select-keys deps-from removed-deps)
       :added    (select-keys deps-to added-deps)
       :modified (select-keys deps-to modified-deps)}
      opts)))

(comment
  ;; git show e0f4689c07bc652492bf03eba7edac20ab2bee0f:test/resources/base.edn > base.edn
  ;; clojure -X namenu.deps-diff/diff base.edn deps.edn

  (diff {:base   "bd130472de267c280d1bd04cb696fb127d0e731c" :target "deps.edn"
         :format :markdown})

  (read-edn "HEAD:deps.edn")
  (read-edn "HEAD:test/resources/base.edn")

  (resolve-deps (read-edn "HEAD:deps.edn") [])

  (defmethod make-output :repl
    [diff _]
    diff)

  (-> (diff {:base    "test-resources/base/deps.edn"
             :target  "test-resources/target/deps.edn"
             :aliases [:test]
             :format  :repl})
      ))
