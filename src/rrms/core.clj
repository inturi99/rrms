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
            [clj-time.coerce :as c]
            [clojure.string :as st]
            )
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

  (GET "/documents/paging/:index/:pagesize" [index pagesize]
       (rr/content-type
        (rr/response {:totaldocuments (db/get-total-documents)
                      :data (db/get-all-documents-by-index-pagesize
                             {:index (parse-int index)
                              :pagesize (parse-int pagesize)
                              })})
        content-type))

  (GET "/documents/all" []
       (rr/content-type
        (rr/response (db/get-all-documents))
        content-type))

  (GET "/documents/searchby/title/:dt" [dt]
       (rr/content-type
        (rr/response  (db/get-documents-by-title
                       {:srcstr dt}))
        content-type))

  (GET "/documents/searchby/:date" [date]
       (rr/content-type
        (rr/response  (db/search-documents-by-date
                       {:date1 (c/to-sql-time (f/parse date))})) content-type))

  (GET "/documents/searchby/:date1/:date2" [date1 date2]
       (rr/content-type
        (rr/response  (db/search-documents-between-two-dates
                       {:date1 (c/to-sql-time (f/parse date1))
                        :date2 (c/to-sql-time (f/parse date2))}))
        content-type))

  (GET "/documents/searchby/:date1/:date2/:dt" [date1 date2 dt]
       (rr/content-type
        (rr/response  (db/search-documents
                       {:date1 (c/to-sql-time (f/parse date1))
                        :date2 (c/to-sql-time (f/parse date2))
                        :srcstr dt }))
        content-type))


  ;; (GET "/documents/date/:dt1/:dt2/:dt" [dt1 dt2 dt]
  ;;      (rr/content-type
  ;;       (rr/response (db/search-documents1
  ;;                     {:date1  (if (st/blank? dt1) "0000-00-00" (c/to-sql-time (f/parse dt1)))
  ;;                      :date2 (if (st/blank? dt2) "0000-00-00" (c/to-sql-time (f/parse dt2)))
  ;;                      :srcstr (if (st/blank? dt) "0" dt)
  ;;                      }))
  ;;       content-type))

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
          (do
            (db/insert-documents {:documentname dn
                                  :title title
                                  :employeename en
                                  :date (c/to-sql-time (f/parse d))
                                  :location lc
                                  :barcode br})
            (rr/content-type
             (rr/response (db/get-all-documents))
             content-type))))

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
