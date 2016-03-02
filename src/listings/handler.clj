(ns listings.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [clojure.java.jdbc :refer :all]
            [csv-map.core :as csv]
            [cheshire.core :as json]
            [ring.util.response :refer [response header]]
            [clojure.tools.logging :as log]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

(def db
  {:subprotocol "sqlite"
   :subname     ":memory"})

(defn create-db []
  (try (db-do-commands db
                       (create-table-ddl :listings
                                         [:id :int]
                                         [:street :text]
                                         [:status :text]
                                         [:price :double]
                                         [:bedrooms :double]; Maybe there's half a bedrooms or something
                                         [:bathrooms :double] ; In case we have x.5 bathrooms
                                         [:sq_ft :double]
                                         [:lat :double]
                                         [:lng :double]))
                       (catch Exception e (println e))))

(defn load-data []
  (create-db)
  (doseq [listing (csv/parse-csv (slurp "listing-details.csv") :key :keyword)]
    (insert! db :listings listing)))

(load-data)

(defn listing->geojson
  [listing]
  {:type "Feature"
   :geometry {:type "Point"
              :coordinates [(:lng listing) (:lat listing)]}
   :properties (select-keys listing
                              [:id :price :street :bedrooms :bathrooms :sq_ft])})

(defn listings->geojson
  [listings]
  {:type "FeatureCollection",
   :features (mapv listing->geojson listings)})

(defn query-listings
  [min_price max_price min_bed max_bed min_bath max_bath limit offset]
  (let [min_price (or min_price 0)
        max_price (or max_price Double/MAX_VALUE)
        min_bed (or min_bed 0)
        max_bed (or max_bed Double/MAX_VALUE)
        min_bath (or min_bath 0)
        max_bath (or max_bath Double/MAX_VALUE)]
    (query db (format "SELECT * from listings where
                       price >= %s AND price <= %s
                       AND bedrooms >= %s AND bedrooms <=  %s
                       AND bathrooms >= %s AND bathrooms <= %s
                       LIMIT %s, %s"
                      min_price
                      max_price
                      min_bed
                      max_bed
                      min_bath
                      max_bath
                      offset
                      limit))))

(defn parse-double
  [d]
  (try (Double/parseDouble d)
       (catch Exception e
         nil)))

(defn params-valid?
  [params]
  (not (some nil? (map parse-double (remove nil? params)))))

(defn json-response
  [resp]
  (-> resp
      json/generate-string
      response
      (header "Content-type" "application/json")))

(defn paginate-link [link limit offset rel]
  (format "<%s&limit=%s&offset=%s>; rel=\"%s\"" link limit offset rel))

(defn paginate-headers
  [resp link limit offset]
  (header resp "Link" (paginate-link link limit offset "next")))

(defroutes app-routes
  (GET "/listings"
       [min_price
        max_price
        min_bed
        max_bed
        min_bath
        max_bath
        limit offset :as {:keys [self-link]}]
       (if-not (params-valid? [min_price
                               max_price
                               min_bed
                               max_bed
                               min_bath
                               max_bath
                               limit
                               offset])
         {:status 400, :headers {}, :body "One of the params isn't valid"}
         (let [limit (or limit 100)
               offset (if (nil? offset) 0 (Integer/parseInt offset))]
           (-> (query-listings min_price
                               max_price
                               min_bed
                               max_bed
                               min_bath
                               max_bath
                               limit
                               offset)
               listings->geojson
               json-response
               (paginate-headers self-link limit (+ 100 offset))))))
  (route/not-found "Not Found"))

(defn add-self-link [handler]
  (fn [{:keys [scheme server-name server-port uri query-string] :as r}]
    (let [link (str (name scheme) "://" server-name ":" server-port uri "?")]
      (handler (assoc r :self-link link)))))

(def app
  (-> app-routes
      add-self-link
      (wrap-defaults site-defaults)))
