(ns namenu.deps-diff.test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest testing is]]
            [namenu.deps-diff.core :refer [diff*]]
            [namenu.deps-diff.output :refer [make-ver]]
            [namenu.deps-diff.spec :as spec]))

#_
(deftest diff-test
  (testing "without aliases"
    (let [d (diff* {:base    "test-resources/base/deps.edn"
                    :target  "test-resources/target/deps.edn"})]
      (is (empty? (:removed d)))
      (is (empty? (:added d)))
      (is (not-empty (:modified d)))))

  (testing "aliases"
    (let [d (diff* {:base    "test-resources/base/deps.edn"
                    :target  "test-resources/target/deps.edn"
                    :aliases [:dev]})]
      (is (= (update-vals d count)
             {:removed 1, :added 11, :modified 31}))))

  (testing "local/root test"
    (let [d (diff* {:base    "test-resources/base/deps.edn"
                    :target  "test-resources/target/deps.edn"
                    :aliases [:poly]})]
      (is (contains? (:modified d)
                     'com.github.seancorfield/next.jdbc)))))

(deftest make-ver-test
  (testing "git deps"
    (is (= "e0df5b36b496c485c75f38052a71b18f02772cc0"
           (make-ver
             (s/conform ::spec/coord
                        {:git/url       "https://github.com/green-labs/superlifter.git",
                         :git/sha       "e0df5b36b496c485c75f38052a71b18f02772cc0",
                         :deps/manifest :deps,
                         :deps/root     "/Users/namenu/.gitlibs/libs/superlifter/superlifter/e0df5b36b496c485c75f38052a71b18f02772cc0",
                         :dependents    ['green-labs/gosura],
                         :parents       #{['green-labs/gosura]},
                         :paths         ["/Users/namenu/.gitlibs/libs/superlifter/superlifter/e0df5b36b496c485c75f38052a71b18f02772cc0/src"]}
                        ))))))

(clojure.test/run-tests)
