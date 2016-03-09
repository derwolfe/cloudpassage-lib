(ns cloudpassage-lib.core
  (:require
   [clojure.string :as str]
   [aleph.http :as http]
   [manifold.deferred :as md]
   [manifold.time :as mt]
   [byte-streams :as bs]
   [taoensso.timbre :as timbre :refer [info error]]
   [base64-clj.core :as base64]
   [clj-time.core :as time]
   [clj-time.format :as f]
   [cheshire.core :as json]))

;; the url from which new auth-tokens can be obtained.
(def auth-uri "https://api.cloudpassage.com/oauth/access_token?grant_type=client_credentials")
(def events-uri "https://api.cloudpassage.com/v1/events?")

(defn ^:private ->basic-auth-header
  [client-id client-key]
  (let [together (str client-id ":" client-key)
        encoded (base64/encode together)]
    {"Authorization" (str/join " " ["Basic" encoded])}))

(defn ^:private ->bearer-auth-header
  [auth-token]
  {"Authorization" (str/join " " ["Bearer" auth-token])})

(def cp-date-formatter
  (f/formatter "yyyy-MM-dd'T'HH:mm:ss'Z'"))

(defn ->cp-date
  [date]
  (f/unparse cp-date-formatter date))

(defn get-auth-token!
  "Using the secret key and an ID, fetch a new auth token.

  client-key - a string representing the key provided by cloudpassage.
  client-id - a string representing an customer.

  returns a new auth token hashmap"
  [client-id client-key]
  (info "fetching new auth token for" client-id)
  (let [sent-at (time/now)
        auth-header (->basic-auth-header client-id client-key)
        token @(md/chain
                (http/post auth-uri {:headers auth-header})
                :body
                bs/to-string
                (fn [response]
                  (json/parse-string response true)))]
    token))

(defn get-single-events-page!
  "get a page at `uri` using the provided `auth-token`.

  returns a `manifold.deferred/deferred` that when realized contains a clojure
  map representing the body of an http response."
  [auth-token uri]
  (info "fetching" uri)
  (let [auth-header (->bearer-auth-header auth-token)]
    (->
     (md/chain
      (http/get uri {:headers auth-header})
      :body
      bs/to-string
      (fn [body-bytes]
        (json/parse-string body-bytes true)))
     (md/catch
      Exception
      (fn [exc]
        (error "error fetching events page:" (.getMessage exc))
        ::fetch-error)))))
