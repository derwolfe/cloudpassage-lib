(ns cloudpassage-lib.core-test
  (:require [clojure.test :refer :all]
            [manifold.deferred :as md]
            [clj-time.core :as ct]
            [cheshire.core :as json]
            [cloudpassage-lib.core :as core]))

(deftest get-auth-token!-tests
  (testing "returns an authentication token"
    (let [sent-at (ct/now)
          expires-at (ct/plus sent-at (ct/seconds 900))
          response {:access_token "ffad76cc550110fc4c84a18397b6e104"
                    :expires_in 900
                    :token_type "bearer"}
          fake-post (fn [_addr _opts]
                      (let [foo (md/deferred)]
                        (md/success!
                         foo
                         {:body (json/generate-string response)})
                        foo))]
      (with-redefs [aleph.http/post fake-post
                    clj-time.core/now (fn [] sent-at)]
        (is (= response (core/get-auth-token! "secret-key" "id")))))))

(deftest iso-date-tests
  (testing "it actually formats dates"
    (let [a-date (ct/date-time 1986 10 14 4 3 27 456)
          formatted (core/->cp-date a-date)]
      (is (= formatted "1986-10-14T04:03:27Z")))))

(deftest get-single-events-page!-tests
  (testing "returns ::fetch-error on exception"
    (let [fake-get (fn [_addr _headers]
                     (let [d (md/deferred)]
                       (md/error! d (Exception. "kaboom"))
                       d))]
      (with-redefs [aleph.http/get fake-get]
        (is (= :cloudpassage-lib.core/fetch-error
               @(core/get-single-events-page! "" ""))))))
  (testing "returns deserialized json body"
    (let [fake-get (fn [_addr _headers]
                     (let [d (md/deferred)]
                       (md/success! d {:body (json/generate-string {:events []})})
                       d))]
      (with-redefs [aleph.http/get fake-get]
        (is (= {:events []}
               @(core/get-single-events-page! "" "")))))))
