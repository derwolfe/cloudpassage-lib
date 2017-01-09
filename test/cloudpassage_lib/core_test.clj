(ns cloudpassage-lib.core-test
  (:require
   [clojure.test :refer :all]
   [clojure.core.cache :as cache]
   [manifold.deferred :as md]
   [manifold.time :as mt]
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
                      (md/success-deferred {:body (json/generate-string response)}))]
      (with-redefs [aleph.http/post fake-post
                    clj-time.core/now (fn [] sent-at)]
        (is (= response @(core/get-auth-token! "secret-key" "id"))))))
  (testing "retries getting the token and fails with the last exception"
    (let [sent-at (ct/now)
          attempts (atom 0)
          fake-post (fn [_addr _opts]
                      (swap! attempts inc)
                      (md/error-deferred (Exception. "401")))
          c (mt/mock-clock)]
      (with-redefs [aleph.http/post fake-post
                    clj-time.core/now (fn [] sent-at)]
        (mt/with-clock c
          (let [result (core/get-auth-token! "secret-key" "id")]
            (is (= 1 @attempts))

            (mt/advance c (mt/seconds 4))
            (is (= 2 @attempts))

            (mt/advance c (mt/seconds 16))
            (is (= 3 @attempts))
            (is (thrown-with-msg? Exception #"401" @result))))))))

(deftest iso-date-tests
  (testing "it actually formats dates"
    (let [a-date (ct/date-time 1986 10 14 4 3 27 456)
          formatted (core/->cp-date a-date)]
      (is (= "1986-10-14T04:03:27Z" formatted)))))

(deftest get-single-events-page!-tests
  (testing "returns ::fetch-error on exception"
    (let [fake-get (fn [_addr _headers]
                     (md/error-deferred (Exception. "kaboom")))]
      (with-redefs [aleph.http/get fake-get]
        (is (= :cloudpassage-lib.core/fetch-error
               @(core/get-single-events-page! "" ""))))))
  (testing "returns deserialized json body"
    (let [fake-get (fn [_addr _headers]
                     (md/success-deferred {:body (json/generate-string {:events []})}))]
      (with-redefs [aleph.http/get fake-get]
        (is (= {:events []}
               @(core/get-single-events-page! "" "")))))))

(deftest fetch-token!-tests
  (testing "when cached, a token is returned"
    (let [client-id 123456
          as-key (str client-id)
          client-secret "unused"
          expected "a-token"
          c (#'cloudpassage-lib.core/build-cache)]
          ;; update the cache atom to include the values we need
      (with-redefs [cloudpassage-lib.core/cache-state c]
        (swap! c cache/miss as-key expected)
        (is (cache/has? @c as-key)
            "the cache has an account for the cid")
        (is (= expected (core/fetch-token! client-id client-secret))))))
  (testing "the cache evicts tokens after a specific cache-ttl-milliseconds"
    (with-redefs [cloudpassage-lib.core/cache-ttl-milliseconds 100]
      (let [c (#'cloudpassage-lib.core/build-cache)]
        ;; sadly, the TTLCache doesn't expose any sort of fake clock like manifold
        (swap! c cache/miss ::bose "speaker")
        (is (cache/has? @c ::bose))
        (Thread/sleep 200) ;; wait extra long
        (is (not (cache/has? @c ::bose))))))
  (testing "returns the newly fetched token and caches it"
    (let [client-id 123456
          as-key (str client-id)
          client-secret "unused"
          expected "a-token"
          c (#'cloudpassage-lib.core/build-cache)
          the-token "my voice is my password, verify me"
          fake-get-auth-token (fn [client-id client-secret]
                                (md/success-deferred {:access_token the-token}))]
      (with-redefs [cloudpassage-lib.core/cache-state c
                    cloudpassage-lib.core/get-auth-token! fake-get-auth-token]
        (is (empty? @c) "nothing is in the cache")
        (is (= the-token (core/fetch-token! client-id client-secret))
            "the token is returned")
        (is (= the-token (cache/lookup @c as-key))
            "the new token has been cached")))))
