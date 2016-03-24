(ns cloudpassage-lib.scans
  "Access to Halo scans."
  (:require
   [cemerick.url :as u]
   [clojure.string :as str]
   [manifold.stream :as ms]
   [aleph.http :as http]
   [manifold.deferred :as md]
   [manifold.stream :as ms]
   [environ.core :refer [env]]
   [cloudpassage-lib.core :as cpc]
   [taoensso.timbre :as timbre :refer [error info spy]]
   [clj-time.core :as t :refer [hours ago]]
   [clj-time.format :as tf]
   [camel-snake-kebab.core :as cskc]
   [camel-snake-kebab.extras :as cske]
   [clojure.java.io :as io]
   [clojure.string :refer [blank?]]))

(def ^:private base-scans-url
  "https://api.cloudpassage.com/v1/scans/")

(defn ^:private maybe-flatten-list
  [maybe-list]
  (if (or (string? maybe-list) (nil? maybe-list))
    maybe-list
    (str/join "," maybe-list)))

(defn ^:private scans-url
  ([opts]
   (scans-url base-scans-url opts))
  ([url opts]
   (let [opts (update opts "modules" maybe-flatten-list)]
     (-> (u/url url)
         (update :query merge opts)
         str))))

(defn get-page!
  "Gets a page, and handles auth for you."
  [client-id client-secret url]
  (let [token (cpc/fetch-token! client-id client-secret (:fernet-key env))]
    (cpc/get-single-events-page! token url)))

(defn ^:private stream-paginated-resources!
  "Returns a stream of resources coming from a paginated list."
  [client-id client-secret initial-url resource-key]
  (let [urls-stream (ms/stream 10)
        resources-stream (ms/stream 20)]
    (ms/put! urls-stream initial-url)
    (ms/connect-via
     urls-stream
     (fn [url]
       (md/chain
        (get-page! client-id client-secret url)
        (fn [response]
          (if (cpc/page-response-ok? response)
            (let [resource (resource-key response)
                  pagination (:pagination response)
                  next-url (:next pagination)]
              (if (blank? next-url)
                (do (info "no more urls to fetch")
                    (ms/close! urls-stream))
                (ms/put! urls-stream next-url))
              (ms/put-all! resources-stream resource))
            (do (error "Error getting scans for url: " url)
                (ms/close! urls-stream)
                (Exception. "Error fetching scans."))))))
     resources-stream)
    resources-stream))

(defn scans!
  "Returns a stream of historical scan results matching opts."
  [client-id client-secret opts]
  (stream-paginated-resources! client-id client-secret (scans-url opts) :scans))

(defn scans-with-details!
  "Returns a stream of historical scan results with their details.

  Because of the way the CloudPassage API works, you need to first
  query the scans, and then you need to fetch the details for each
  scan, and then you need to fetch the FIM scan details for the
  details (iff the details are FIM). See CloudPassage API docs for
  more illustration."
  [client-id client-secret scans-stream]
  (let [scans-with-details-stream (ms/stream 10)]
    (ms/connect-via
     scans-stream
     (fn [scan]
       (md/chain
        (get-page! client-id client-secret (:url scan))
        (fn [response]
          (ms/put! scans-with-details-stream (assoc scan :scan (:scan response))))))
     scans-with-details-stream)
    scans-with-details-stream))

(defn ^:private report-for-module!
  "Get recent report data for a certain client, and filter based on module."
  [client-id client-secret module-name]
  ;; The docs say we can use "module" as a query parameter but it does
  ;; not work for FIM or SVM, so we have to filter out those items instead.
  (let [opts {"since" (cpc/->cp-date (-> 3 hours ago))}]
    (->> (scans! client-id client-secret opts)
         (ms/filter (fn [{:keys [module]}] (= module module-name)))
         (scans-with-details! client-id client-secret)
         (ms/map #(cske/transform-keys cskc/->kebab-case-keyword %))
         ms/stream->seq)))

(defn fim-report!
  "Get the current (recent) FIM report for a particular client."
  [client-id client-secret]
  (report-for-module! client-id client-secret "fim"))

(defn svm-report!
  "Get the current (recent) SVM report for a particular client."
  [client-id client-secret]
  (report-for-module! client-id client-secret "svm"))

(defn sca-report!
  "Get the current (recent) sca report for a particular client."
  [client-id client-secret]
  (report-for-module! client-id client-secret "sca"))
