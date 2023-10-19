(ns namenu.deps-diff.test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest testing is]]
            [namenu.deps-diff.core :refer [diff* parse-resolved-tree]]
            [namenu.deps-diff.output :refer [make-ver]]
            [namenu.deps-diff.spec :as spec]))

(deftest diff-test
  (let [base   (parse-resolved-tree "test-resources/__base.edn")
        target (parse-resolved-tree "test-resources/__target.edn")]
    (testing "without aliases"
      (let [d (diff* base target)]
        (is (empty? (:removed d)))
        (is (empty? (:added d)))
        (is (not-empty (:modified d)))))

    (testing "aliases"
      (let [d (diff* base target)]
        (is (= (update-vals d count)
               {:removed 1, :added 11, :modified 31}))))

    (testing "local/root test"
      (let [d (diff* base target)]
        (is (contains? (:modified d)
                       'com.github.seancorfield/next.jdbc))))))

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
