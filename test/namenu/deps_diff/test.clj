(ns namenu.deps-diff.test
  (:require [clojure.test :refer [deftest testing is]]
            [namenu.deps-diff :refer [diff*]]))

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

(clojure.test/run-tests)
