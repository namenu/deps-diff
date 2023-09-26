(ns namenu.deps-diff
  (:require [clojure.data :as data]
            [clojure.edn :as edn]
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
    (-> (run-git "show" (str path ":" "deps.edn"))
        :out
        (edn/read-string))))

(defn- resolve-deps [deps]
  (-> (merge {:mvn/repos mvn/standard-repos} deps)
      (deps/resolve-deps nil)
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

(defn diff
  "
  opts
    :base - git sha
    :target - file path
    :format - #{:edn, :markdown}
  "
  [{:keys [base target] :as opts}]
  (let [deps-from (resolve-deps (read-edn base))
        deps-to   (resolve-deps (read-edn target))
        [removed-deps added-deps modified-deps] (data/diff (key-set deps-from) (key-set deps-to))]
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

  (read-edn "e0f4689c07bc652492bf03eba7edac20ab2bee0f")
  (read-edn "bd130472de267c280d1bd04cb696fb127d0e731c")
  (read-edn "deps.edn")

  (diff "test/resources/base.edn"
        "test/resources/target.edn"))