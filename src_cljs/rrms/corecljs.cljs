(ns rrms.corecljs
  (:require-macros [secretary.core :refer [defroute]])
  (:require [goog.events :as events]
            [secretary.core :as secretary]
            [goog.net.XhrIo :as xhr]
            [reagent.core :as r]
            [cognitect.transit :as t]
            [goog.structs :as structs]
            [cljs-time.format :as f]
            [cljs-time.core :as tt]
            [cljsjs.react-bootstrap]
            [clojure.string :as st])
  (:import goog.History
           goog.json.Serializer))

(def storage (r/atom {:documents {}
                      :current-page 1
                      :total-pages 1}))

(defn set-key-value [k v]
  (reset! storage (assoc @storage k v)))

(defn get-value! [k]
  (k @storage))

(defn get-total-rec-no [nos]
  (let [totrec (quot nos 10)]
    (if (zero? (mod nos 10))
      totrec
      (+ 1 totrec))))

(defn get-new-page-data [data current-page]
  (let [total-pages (get-total-rec-no (count data))
        pag-start (* 10 (dec current-page))
        pag-end (+ pag-start 9)]
    (cond (<= total-pages 1) (do (set-key-value :current-page 1)
                                 (set-key-value :total-pages 1)
                                 (clj->js (keep-indexed #(if (< %1 10) %2) data)))
          :else (do (set-key-value :total-pages total-pages)
                    (clj->js (keep-indexed
                              #(if (and (>= %1 pag-start) (<= %1 pag-end)) %2) data))))))


(defn url-format [url title]
  [:a {:href url :class "btn btn-primary  glyphicon glyphicon-plus"} title])

(def w (t/writer :json-verbose))

(def pager-elem (r/adapt-react-class (aget js/ReactBootstrap "Pagination")))

(defn getd [indexNo]
  (let [onres (fn [json]
                (let [d (.-data (getdata json))]
                  (set-key-value :documents d)
                  (r/render [render-documents (get-value! :documents)]
                            (.getElementById js/document "app1"))))]
    (http-get (str (str "http://localhost:8193/documents/paging/" indexNo) "/10") onres)))

(defn pager [value total-rec]
  [pager-elem {:bsSize "large"
               :prev true
               :next true
               :first true
               :last true
               :ellipsis true
               :items (:total-pages @storage)
               :activePage (:current-page @storage)
               :maxButtons 5
               :onSelect (fn [s1 s2]
                           (let [i (.-eventKey s2)]
                             (getd i)
                             (set-key-value :current-page i)))}])


(defn shared-state [totalRec]
  (let [val (r/atom 1)
        trec (r/atom totalRec)]
    [:div.row
     [pager val trec]]))


(defn getdata [res]
  (.getResponseJson (.-target res)))

(defn http-get [url callback]
  (xhr/send url callback))

(defn http-post [url callback data]
  (xhr/send url callback "POST" data  (structs/Map. (clj->js {:Content-Type "application/json"}))))

(defn http-delete [url callback]
  (xhr/send url callback "DELETE"  (structs/Map. (clj->js {:Content-Type "application/json"}))))

(declare render-documents)

(defn search [event]
  (let [dt1 (.-value (.getElementById js/document "dt1"))
        dt2 (.-value (.getElementById js/document "dt2"))
        dt  (.-value (.getElementById js/document "dt"))
        onres (fn [json]
                ((set-key-value :documents (getdata json))
                 (r/render [render-documents (get-new-page-data (get-value! :documents)
                                                                (get-value! :current-page))]
                           (.getElementById js/document "app1"))))]
    (http-get (str "http://localhost:8193/documents/date/" (if (st/blank? dt1) "0000-00-00" dt1)
                   "/"(if (st/blank? dt2) "0000-00-00" dt2)
                   "/"(if(st/blank? dt) "0" dt)) onres)))

(defn row [label input]
  [:div.row
   [:div.col-md-2 [:label label]]
   [:div.col-md-5 input]])

(defn radio [label name value]
  [:div.radio
   [:label
    [:input {:field :radio :name name :value value}]
    label]])

(defn input
  ([label type id value]
   (row label [:input.form-control {:type type :id id :defaultValue value}]))
  ([label type id]
   (input label type id "")))

(defn getinputvalue[id]
  (.-value (.getElementById js/document id)))

(defn get-documents-formdata []
  {:documentname (getinputvalue "documentname")
   :title (getinputvalue "title")
   :employeename (getinputvalue "employeename")
   :date (getinputvalue "date")
   :location (getinputvalue "location") })

(defn save [event]
  (let [onres (fn[data] (set! (.-location js/window) "http://localhost:8193"))]
    (js/console.log (get-documents-formdata))
    (http-post "http://localhost:8193/documents/add"
               onres  (.serialize (Serializer.) (clj->js (get-documents-formdata))))))

(defn document-template []
  [:div {:id "add" :class "form-group"}
   [:div#dn (input "Documentname" :text :documentname )]
   [:div#tl (input "Title" :text :title)]
   [:div#empn (input "EmployeeName" :text :employeename)]
   [:div#dt (input "Date":Date :date)]
   [:div#loc (input "Location":text :location)]
   [:input {:type "button" :value "Save"
            :class "btn btn-primary" :on-click save}]])

(defn get-update-documents-formdata []
  {
   :id (getinputvalue "id")
   :documentname (getinputvalue "upd_documentname")
   :title (getinputvalue "upd_title")
   :employeename (getinputvalue "upd_employeename")
   :date (getinputvalue "upd_date")
   :location (getinputvalue "upd_location")})

(defn click-update[id]
  (.assign js/location (str "#/documents/update/" id)))

(defn docupdate [event]
  (let [onres (fn[data]
                (.assign js/location "/"))]
    (http-post "http://localhost:8193/documents/update"
               onres (.serialize (Serializer.) (clj->js (get-update-documents-formdata))))))

(defn document-update-template [id dmt]
  [:div.form-group {:id "update" :class "form-group"}
   [:div [:input {:type "hidden" :value id :id "id"}]]
   [:div (input "Documentname" :text :upd_documentname (.-documentname dmt))]
   [:div (input "Title" :text :upd_title (.-title dmt))]
   [:div (input "EmployeeName" :text :upd_employeename (.-employeename dmt))]
   [:div (input "Date":Date :upd_date  (f/unparse (f/formatter "yyyy-MM-dd")(f/parse (.-date dmt))))]
   [:div (input "Location":text :upd_location (.-location dmt))]
   [:input {:type "button" :value "Save"
            :class "btn btn-primary" :on-click docupdate}]])

(defn delete[id]
  (let [onres (fn [json]
                ((set-key-value :documents (getdata json))
                 (r/render [render-documents (get-new-page-data (get-value! :documents)
                                                                (get-value! :current-page))]
                           (.getElementById js/document "app1"))))]
    (http-delete (str "http://localhost:8193/documents/delete/" id)  onres)))

(defn render-documents [documents]
  [:div
   ;; [:div.padding]
   ;; [:div.page-header [:h1 "Record Room Management System"]]
   [:div#add]
   [:div#update]
   [:div {:class "box"}
    [:div {:class "box-header"}
     [:h8 ""]]
    ;; [:br]
    ;; [:h1.text-center "List of Documents"]
    [:div {:class "row"}
     [:div {:class "col-xs-12"}
      [:div.form-group
       [:div.col-sm-2 [:input.form-control {:id "dt1" :type "date"}]]
       [:div.col-sm-2 [:input.form-control {:id "dt2" :type "date"}]]
       [:div.col-sm-2 [:input.form-control {:id "dt" :type "text"
                                            :placeholder "Search By Title"}]]
       [:input {:type "button" :value "Search"
                :class "btn btn-primary" :on-click search}]
       (url-format "#/documents/add" "Document")]
      [:div {:class "box-body"}

       [:table {:class "table table-bordered table-striped dataTable"}
        [:thead
         [:tr
          [:th "DocumentName"]
          [:th "Title"]
          [:th "Employeename"]
          [:th "Date"]
          [:th "Location"]
          [:th " "]
          [:th " "]
          ]]
        [:tbody
         (for [dn documents]
           ^{:key (.-id dn)} [:tr
                              [:td (.-documentname dn)]
                              [:td (.-title dn)]
                              [:td (.-employeename dn)]
                              [:td  (f/unparse (f/formatter "dd-MMM-yyyy")(f/parse (.-date dn)))]
                              ;; [:td (.-date dn)]
                              [:td (.-location dn)]
                              ;; [:td [:input {:type "button" :on-click #(click-update(.-id dn))
                              ;;               :class "glyphicon glyphicon-edit" :value "Update"}
                              ;;       ]]
                              [:td [:a {:href "javascript:;" :on-click  #(click-update(.-id dn))  :class "btn btn-success btn-sm glyphicon glyphicon-edit"}]]
                              ;; [:td [:input {:type "button" :on-click #(delete(.-id dn))
                              ;;               :class "glyphicon glyphicon-remove"  :value "Delete"}]]
                              [:td  [:a {:href "javascript:;" :on-click #(delete(.-id dn))  :class "btn btn-danger btn-sm glyphicon glyphicon-remove"}] ]

                              ])]]]
      ]]]
   ;; [:div.padding]
   ;;  [:div.page-footer [:h4 "Copyright All Rights Reserved © 2016 TechnoIdentity Solutions Pvt.Ltd"]]
   ])
(defn table-mount []
  (.ready (js/$ js/document)
          (fn []
            (.DataTable (js/$ "#example1")))))
(defn home [documents]
  (r/create-class {:reagent-render render-documents
                   :component-did-mount table-mount }))


(defroute home-path "/" []
  (let [onres (fn [json]
                (let [dt (getdata json)]
                  (set-key-value :documents (.-data dt))
                  (r/render [render-documents (get-value! :documents)]
                            (.getElementById js/document "app1"))
                  (r/render [shared-state 0]
                            (.getElementById js/document "pindex"))
                  (set-key-value :total-pages (get-total-rec-no
                                               (.-totaldocuments(first
                                                                 (.-totaldocuments dt)))))))]
    (http-get "http://localhost:8193/documents/paging/1/10" onres)))

(defroute documents-path "/documents/add" []
  (r/render-component [document-template] (js/document.getElementById "add")))

(defroute documents-path1 "/documents/update/:id" [id]
  (r/render [document-update-template id
             (first (filter (fn[obj]
                              (=(.-id obj) (.parseInt js/window id))) (get-value! :documents)))]
            (js/document.getElementById "update")))

(defroute "*" []
  (js/alert "<h1>Not Found Page</h1>"))

(defn main
  []
  (secretary/set-config! :prefix "#")
  (let [history (History.)]
    (events/listen history "navigate"
                   (fn [event]
                     (secretary/dispatch! (.-token event))))
    (.setEnabled history true)))

(defn nav! [token]
  (.setToken (History.) token))

(main)
