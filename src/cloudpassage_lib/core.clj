(ns cloudpassage-lib.core
  (:require
   [clojure.string :as str]
   [clojure.math.numeric-tower :as math]
   [aleph.http :as http]
   [environ.core :refer [env]]
   [manifold.deferred :as md]
   [manifold.time :as mt]
   [byte-streams :as bs]
   [taoensso.carmine :as car :refer (wcar)]
   [taoensso.timbre :as timbre :refer [info error]]
   [base64-clj.core :as base64]
   [clj-time.core :as time]
   [clj-time.format :as f]
   [cloudpassage-lib.fernet :as fernet]
   [cheshire.core :as json]))

;; the url from which new auth-tokens can be obtained.
(def auth-uri "https://api.cloudpassage.com/oauth/access_token?grant_type=client_credentials")
(def events-uri "https://api.cloudpassage.com/v1/events?")

(defn redis-connection
  []
  (let [{:keys [redis-url redis-timeout]} env]
    {:pool {}
     :spec {:uri redis-url
            :timeout (read-string redis-timeout)}}))

(defmacro wcar* [& body] `(car/wcar (redis-connection) ~@body))

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

(defn exponentially
  "Returns a function that when evaluated will produce the initial wait
  raised to the number of failures.

  This is intended to be used in conjunction with other 2-arg combinators,
  e.g., `up-to`."
  [wait]
  (fn [failures]
    (math/expt wait (count failures))))

(defn up-to
  "Returns a function that when evaluated will either

  1) return a number to use to wait until retrying a function again. Or,
  2) throw an exception because the maximum number of retries, `stop` has
  been reached.

  This is intended to be used in conjunction with other 2-arg combinators,
  e.g., `up-to`."
  [stop retry?]
  (fn [failures]
    (if (< (count failures) stop)
      (retry? failures)
      (throw (last failures)))))

(defn retry
  "Takes a function that returns a `manifold.deferred/deferred`. Retries that
  function until it succeeds or the number of failures equal the stop value.

  `retry` expects to encounter exceptions when retrying `f`. As such,
  it will catch all exceptions that `f` might throw and continue retrying.

  f - a function that should be retried; must return a
      `manifold.deferred/deferred'
  p - an int representing the initial number of seconds to wait before retrying.
      This will grow exponentially for each attempt.
  stop - an int representing the number of tries that the api should make before
         giving up and returning `:cloudpassage-lib.core/retry-failure`.

  Returns a deferred wrapping the results of `f`."
  [f p stop]
  (let [try-after (->> (exponentially p)
                       (up-to stop))]
    (md/loop [failures []]
      (md/catch
       (f)
       Exception
        (fn [exc]
          (let [all-failures (conj failures exc)
                wait (try-after all-failures)]
            (mt/in (mt/seconds wait) #(md/recur all-failures))))))))

(defn get-auth-token!
  "Using the secret key and an ID, fetch a new auth token.

  client-key - a string representing the key provided by cloudpassage.
  client-id - a string representing an customer.

  returns a `manifold.deferred/deferred' wrapping an authentication
  token hash map"
  [client-id client-key]
  (info "fetching new auth token for" client-id)
  (let [sent-at (time/now)
        auth-header (->basic-auth-header client-id client-key)
        starting-retry 4
        stop-after 3]
    (md/chain
     (retry
      #(http/post auth-uri {:headers auth-header})
      starting-retry
      stop-after)
     #(json/parse-stream (bs/to-reader (:body %)) true))))

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
      bs/to-reader
      #(json/parse-stream % true))
     (md/catch
      Exception
      (fn [exc]
        (error "error fetching events page:" (.getMessage exc))
        ::fetch-error)))))

(defn page-response-ok?
  "Returns false if there was an error retrieving a page."
  [response]
  (not= ::fetch-error response))

(defn fetch-token!
  "Fetch an access token for the cloudpassage api that belongs to the client-id/secret pair.
   If a token doesn't exist in Redis, then this will hit the cloudpassage api to obtain one.
   If the token exists in the cache, it will be returned.

  Returns a string representing an access-token."
  [client-id client-secret fernet-key]
  (let [account-key (str "account-" client-id)
        token (wcar* (car/get account-key))]
    (if (some? token)
      ;; a token is in redis
      (fernet/decrypt fernet-key token)
      ;; no token is present, fetch a new one
      (let [new-token @(get-auth-token! client-id client-secret)
            ;; this will cause the token to expire 100 seconds earlier than expiration
            ;; it is a simple fudge factor.
            {:keys [access_token expires_in]} new-token
            ttl (- expires_in 100)
            encrypted-token (fernet/encrypt fernet-key access_token)]
        (wcar* (car/setex account-key ttl encrypted-token))
        access_token))))
