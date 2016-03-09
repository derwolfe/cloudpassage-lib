(ns cloudpassage-lib.fernet-test
  (:require [clojure.test :refer :all]
            [fernet.core :as clj-fernet]
            [cloudpassage-lib.fernet :as cpfernet]))

(deftest encrypt-tests
  (testing "it encrypts messages"
    (let [key (clj-fernet/generate-key)
          message "my name is bob and you know it."
          ciphertext (cpfernet/encrypt key message)
          decrypted-bytes (clj-fernet/decrypt key ciphertext)
          decrypted-message (String. decrypted-bytes)]
      (is (not= message ciphertext))
      (is (= message decrypted-message)))))

(deftest decrypt-tests
  (testing "it decrypts encrypted ciphertexts"
    (let [key (clj-fernet/generate-key)
          message "my name is bob and you know it."
          message-bytes (byte-array (map byte message))
          ciphertext (clj-fernet/encrypt key message-bytes)
          decrypted-message (cpfernet/decrypt key ciphertext)]
      (is (not= message ciphertext))
      (is (= message decrypted-message)))))
