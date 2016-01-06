(ns rrms.corecljs
  (:require-macros [secretary.core :refer [defroute]])
  (:require [goog.events :as events]
            [secretary.core :as secretary]
            [goog.net.XhrIo :as xhr]
            [reagent.core :as r]
            [cognitect.transit :as t]
            [goog.structs :as structs]
            [cljs-time.format :as f]
            [cljs-time.core :as tt])
  (:import goog.History
           goog.json.Serializer))

(def documents (r/atom nil))

(def document (r/atom {}))

(defn url-format [url title]
  [:a {:href url :class "btn btn-primary"} title])

(def w (t/writer :json-verbose))

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
  (let [stext (.-value (.getElementById js/document "sText"))
        onres (fn [json]
                (r/render [render-documents (getdata json)]
                          (.getElementById js/document "app1")))]
    (http-get (str "http://localhost:8193/documents/title/" stext) onres)))

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
                ((reset! documents (getdata json))
                 (r/render [render-documents @documents]
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
       [:div.col-sm-2 [:input.form-control {:id "sText" :type "text"
                                            :placeholder "search by title"}]]
       [:input {:type "button" :value "Search"
                :class "btn btn-primary" :on-click search}]
       (url-format "#/documents/add" "Add")]
      [:div {:class "box-body"}

       [:table {:id "example1" :class "table table-bordered table-striped dataTable"}
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
                              [:td [:a {:href "javascript:;" :on-click  #(click-update(.-id dn))  :class "glyphicon glyphicon-edit"}]]
                              ;; [:td [:input {:type "button" :on-click #(delete(.-id dn))
                              ;;               :class "glyphicon glyphicon-remove"  :value "Delete"}]]
                              [:td  [:a {:href "javascript:;" :on-click #(delete(.-id dn))  :class "glyphicon glyphicon-remove"}] ]

                              ])]]]]]]
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
                ((reset! documents (getdata json))
                 (r/render [home @documents]
                           (.getElementById js/document "app1"))))]
    (http-get "http://localhost:8193/documents/all" onres)))

(defroute documents-path "/documents/add" []
  (r/render-component [document-template](js/document.getElementById "add")))

(defroute documents-path1 "/documents/update/:id" [id]
  (r/render [document-update-template id
             (first (filter (fn[obj]
                              (=(.-id obj) (.parseInt js/window id))) @documents))]
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
