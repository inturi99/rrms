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
            [clj-time.core :as t])
  (:gen-class))

(def content-type  "application/json; charset=utf-8")

(defmulti parse-int type)
(defmethod parse-int java.lang.Integer [n] n)
(defmethod parse-int java.lang.String [s] (Integer/parseInt s))

(defroutes app-routes

  (GET "/documents/title/:title" [title]
       (rr/content-type
        (rr/response  (db/get-documents-by-title
                       {:title title}))
        content-type))

  (GET "/documents/all" []
       (rr/content-type
        (rr/response (db/get-all-documents))
        content-type))

  (GET "/documents/id/:id" [id]
       (rr/content-type
        (rr/response (db/get-documents-by-id
                      {:id (parse-int id)}))
        content-type))

  (POST "/documents/add" {body :body}
        (let [{dn "documentname" title "title"
               en "employeename" d "date"
               lc "location"
               br "barcode" ia "isactive"}
              body]
          (db/insert-documents {:documentname dn
                                :title title
                                :employeename en
                                :date d
                                :location lc
                                :barcode br
                                :isactive ia})
          (rr/content-type (rr/response "") content-type)))

  (POST "/documents/update"  {body :body}
        (let [{id "id"
               dn "documentname" title "title"
               en "employeename" d "date"
               lc "location" ia "isactive"}
              body]
          (db/update-documents {:id id
                                :documentname dn
                                :title title
                                :employeename en
                                :date d
                                :location lc
                                :isactive ia})
          (rr/content-type (rr/response "") content-type)))

  (GET "/documents/delete/:id" [id]
       (rr/content-type
        (rr/response (db/delete-documents-by-id
                      {:id (parse-int id)}))
        content-type))
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
