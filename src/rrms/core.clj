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
            [bouncer.validators :as v])
  (:gen-class))

(def content-type  "application/json; charset=utf-8")

(defroutes app-routes
  (GET "/documents/title/:title" [title] (rr/content-type
                                          (rr/response  (db/get-documents-by-title
                                                         {:title title}))
                                          content-type))
  (POST "/documents/add" {body :body}
        (let [{dn "documentname" t "title"
               en "employeename" lc "location"
               br "barcode" ia "isactive"}
              body]
          (db/insert-documents {:documentname dn
                                :title t
                                :employeename en
                                :location lc
                                :barcode br
                                :isactive ia})
          (rr/content-type (rr/response "") content-type)))
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
