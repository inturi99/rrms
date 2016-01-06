(ns rrms.core
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.json :as ring-json]
            [ring.util.response	:as rr]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.cors :refer [wrap-cors]]
            [rrms.db.core :as db]
            [bouncer.core :as b]
            [bouncer.validators :as v]
            [clj-time.core :as t]
            [compojure.response :refer [render]]
            [clojure.java.io :as io]
            [clj-time.format :as f]
            [clj-time.coerce :as c])
  (:gen-class))

(defn home
  ""
  [req]
  (render (io/resource "index.html") req))

(def content-type  "application/json; charset=utf-8")

(defmulti parse-int type)
(defmethod parse-int java.lang.Integer [n] n)
(defmethod parse-int java.lang.String [s] (Integer/parseInt s))

(defroutes app-routes
  (GET "/" [] home)

  (GET "/documents/all" []
       (rr/content-type
        (rr/response (db/get-all-documents))
        content-type))

  (GET "/documents/title/:title" [title]
       (rr/content-type
        (rr/response  (db/get-documents-by-title
                       {:title title}))
        content-type))
  (GET "/documents/id/:id" [id]
       (rr/content-type
        (rr/response (db/get-documents-by-id
                      {:id (parse-int id)}))
        content-type))

  (POST "/documents/add" {body :body}
        (let [{dn "documentname" title "title"
               en "employeename" d "date"
               lc "location" br "barcode" }
              body]
          (rr/content-type
           (rr/response
            (db/insert-documents {:documentname dn
                                  :title title
                                  :employeename en
                                  :date (c/to-sql-time (f/parse d))
                                  :location lc
                                  :barcode br}))
           content-type)))

  (POST "/documents/update"
        {body :body}
        (let [{id "id"
               dn "documentname" title "title"
               en "employeename" d "date"
               lc "location" }
              body]
          (rr/content-type
           (rr/response
            (db/update-documents {:id  (parse-int id)
                                  :documentname dn
                                  :title title
                                  :employeename en
                                  :date (c/to-sql-time (f/parse d))
                                  :location lc}))
           content-type)))

  (DELETE "/documents/delete/:id" [id]
          (db/delete-documents-by-id
           {:id (parse-int id)})
          (rr/content-type
           (rr/response (db/get-all-documents))
           content-type))
  (route/resources "/static")
  (route/not-found "<h1>Page not found</h1>"))

(def app
  (-> app-routes
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))
      (wrap-cors :access-control-allow-origin [#".*"]
                 :access-control-allow-methods [:get :put :post :delete])
      (ring-json/wrap-json-body)
      (ring-json/wrap-json-response)))

(defn -main
  "Record Room Management System "
  [& args]
  (jetty/run-jetty app {:port 8193
                        :join? false}))
