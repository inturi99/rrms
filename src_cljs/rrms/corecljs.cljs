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
            [cljs-time.coerce :as c]
            [cljs-time.predicates :as p]
            [cljsjs.react-bootstrap]
            [clojure.string :as st]
            [goog.dom :as dom]
            [goog.history.EventType :as EventType]
            [bouncer.core :as b]
            [bouncer.validators :as v])
  (:import goog.History
           goog.json.Serializer
           goog.date.Date))

(defonce storage (r/atom {:documents {}
                          :current-page 1
                          :total-pages 1
                          :page-location nil
                          :user nil}))


(defn set-key-value [k v]
  (reset! storage (assoc @storage k v)))

(defn http-get [url callback]
  (xhr/send url callback))

(defn get-value! [k]
  (k @storage))

(defn page []
  (get-value! :page-location))

(defn getinputvalue[id]
  (.-value (.getElementById js/document id)))


(defn getdata [res]
  (.getResponseJson (.-target res)))

(defn get-status [res]
  (.getStatus (.-target res)))

(defn http-post [url callback data]
  (xhr/send url callback "POST" data  (structs/Map. (clj->js {:Content-Type "application/json"}))))

(defn http-delete [url callback]
  (xhr/send url callback "DELETE"  (structs/Map. (clj->js {:Content-Type "application/json"}))))


;; login functions
(defn validator [data-set]
  (first (b/validate data-set
                     :username-email [[v/required :message "Filed is required"]
                                      [v/email :message "Enter valid email-id"]]
                     :password [[v/required :message "Filed is required"]
                                [v/string  :message "Enter valid password"]])))

(defn input-element [id ttype data-set placeholder in-focus]
  [:input.form-control {:id id
                        :type ttype
                        :value (@data-set id)
                        :placeholder placeholder
                        :on-change #(swap! data-set assoc id (-> % .-target .-value))
                        :on-blur  #(reset! in-focus "yes")
                        }])


(defn input-validate [id label span-class ttype data-set placeholder focus]
  (let [input-focus (r/atom nil)]
    (fn []
      [:div.form-group
       [:div.col-md-12
        [:label label]
        [:div.input-group.col-sm-10
         [:span {:class span-class}]
         [input-element id ttype data-set placeholder input-focus]]
        (if (or @input-focus @focus)
          (if (= nil (validator @data-set))
            [:div]
            [:div {:style  {:color "red"}}
             [:b
              (str (first ((validator @data-set) id)))]]  )
          [:div])]])))


(defn submit-login [data-set focus]
  (if (= nil (validator @data-set))
    (let [onresp (fn [json] (if (= (get-status json) 200) ((set-key-value :user (getdata json))
                                                          (secretary/dispatch! "/documents"))))]
      (http-post "http://localhost:8193/user/authenticate"
                 onresp (.serialize (Serializer.)
                                    (clj->js {:email (getinputvalue "username-email")
                                              :password (getinputvalue "password")} ))))
    (reset! focus "yes")))

(defn button [value ttype data-set focus]
  [:div.form-group
   [:div.col-md-6
    [:button.btn.btn-primary {:type ttype
                              :on-click #(submit-login data-set focus)} value]]])

(defn login []
  (let [my-data (r/atom  {})
        focus (r/atom nil)]
    (fn []
      [:div.container
       [:div.panel.panel-primary.modal-dialog
        [:div.panel-heading
         [:h2 "Log-in"]]
        [:div.panel-body
         [input-validate :username-email "Email"  "input-group-addon glyphicon glyphicon-user" "email" my-data "Email" focus]
         [input-validate :password "Password"  "input-group-addon glyphicon glyphicon-lock" "password" my-data "password" focus]
         [button "Sign-in" "button" my-data focus ]]]])))

(defn is-authenticated? []
  (not (nil? (get-value! :user))))

(defn set-page! [currnt-page]
  (if (is-authenticated?)(set-key-value :page-location
                                        currnt-page)
      (set-key-value :page-location [login])))

(defn get-total-rec-no [nos]
  (let [totrec (quot nos 10)]
    (if (zero? (mod nos 10))
      totrec
      (+ 1 totrec))))

(defn get-range-data [data date1 date2]
  (filter #(tt/within? (tt/interval date1 (tt/plus date2 (tt/days 1)))
                       (c/from-string (.-date %))) data))

(defn included? [s subs]
  (>= (.indexOf s subs) 0))


(defn filter-by-str [data lstr]
  (filter #(or (included? (st/lower-case (.-title %)) lstr)
               (included? (st/lower-case (.-documentname %)) lstr))
          data))

(defn filter-by-str-dates
  ([data lstr date1] (filter #(and (or (included? (st/lower-case (.-title %)) lstr)
                                       (included? (st/lower-case (.-documentname %)) lstr))
                                   (tt/= (c/from-string (.-date %)) date1)) data))
  ([data lstr date1 date2](filter #(and (or (included? (st/lower-case (.-title %)) lstr)
                                            (included? (st/lower-case (.-documentname %)) lstr))
                                        (tt/within? (tt/interval date1 (tt/plus date2 (tt/days 1)))
                                                    (c/from-string (.-date %)))) data)))


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

(defn filter-data [data date1 date2 search-str]
  (let [srcstrv (st/blank? search-str)
        lstr (st/lower-case search-str)]

    (cond (and (not (nil? date1)) (nil? date2) srcstrv) (filter #(tt/= date1 (c/from-string (.-date %))) data)
          (and (not (nil? date1)) (not (nil? date2)) srcstrv) (get-range-data data date1 date2)
          (and (nil? date1) (nil? date2) (not srcstrv)) (filter-by-str data lstr)
          (and (not (nil? date1)) (nil? date2) (not srcstrv)) (filter-by-str-dates data lstr date1)
          :else (filter-by-str-dates data lstr date1 date2))))

(def pager-elem (r/adapt-react-class (aget js/ReactBootstrap "Pagination")))

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
                             (do
                               (set-key-value :current-page i)
                               (set-key-value :page-location
                                              [render-documents (get-new-page-data (get-value! :documents)
                                                                                   (get-value! :current-page))]))))}])



(defn shared-state [totalRec]
  (let [val (r/atom 1)
        trec (r/atom totalRec)]
    [:div.row
     [pager val trec]]))


(declare render-documents)

(defn cancel [event]
  (secretary/dispatch! "/documents"))

(defn search [event]
  (let [dt1 (.-value (.getElementById js/document "dt1"))
        dt2 (.-value (.getElementById js/document "dt2"))
        dt  (.-value (.getElementById js/document "dt"))]

    (do (set-key-value :documents
                       (clj->js
                        (filter-data (get-value! :documents)
                                     (c/from-string dt1)
                                     (c/from-string dt2) dt)))
        (set-key-value :current-page 1)
        (set-key-value :page-location
                       [render-documents
                        (get-new-page-data (get-value! :documents)
                                           (get-value! :current-page))]))))


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

(defn get-documents-formdata []
  {:documentname (getinputvalue "documentname")
   :title (getinputvalue "title")
   :employeename (getinputvalue "employeename")
   :date (getinputvalue "date")
   :location (getinputvalue "location") })

(defn save [event]
  (let [onres (fn[json] (do
                         (set-key-value :documents (getdata json))
                         (set-key-value :page-location
                                        [render-documents (get-new-page-data (get-value! :documents)
                                                                             (get-value! :current-page))])))]
    (http-post "http://localhost:8193/documents/add"
               onres  (.serialize (Serializer.) (clj->js (get-documents-formdata))))))

(defn get-all-click [event]
  (let [onres (fn [json]
                (let [dt (getdata json)]
                  (set-key-value :documents dt)
                  (set-key-value :total-pages (get-total-rec-no dt))
                  (set-key-value :current-page 1)
                  (set-key-value :page-location
                                 [render-documents (get-new-page-data (get-value! :documents)
                                                                      (get-value! :current-page))])))]
    (http-get "http://localhost:8193/documents/all" onres)))

(defn document-template []
  [:div {:id "add" :class "form-group"}
   [:div#dn (input "Documentname" :text :documentname )]
   [:div#tl (input "Title" :text :title)]
   [:div#empn (input "EmployeeName" :text :employeename)]
   [:div#dt (input "Date":Date :date)]
   [:div#loc (input "Location":text :location)]
   [:input {:type "button" :value "Save"
            :class "btn btn-primary" :on-click save}]
   [:input {:type "button" :value "Cancel"
            :class "btn btn-primary" :on-click cancel}]])

(defn get-update-documents-formdata []
  {
   :id (getinputvalue "id")
   :documentname (getinputvalue "upd_documentname")
   :title (getinputvalue "upd_title")
   :employeename (getinputvalue "upd_employeename")
   :date (getinputvalue "upd_date")
   :location (getinputvalue "upd_location")})

(defn click-update[id]
  (secretary/dispatch! (str "#/documents/update/" id)))

(defn docupdate [event]
  (let [onres (fn[data]
                (secretary/dispatch! "/documents"))]
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
            :class "btn btn-primary" :on-click docupdate}]
   [:input {:type "button" :value "Cancel"
            :class "btn btn-primary" :on-click cancel}]])

(defn delete[id]
  (let [onres (fn [json]
                (do
                  (set-key-value :documents (getdata json))
                  (set-key-value :page-location
                                 [render-documents (get-new-page-data (get-value! :documents)
                                                                      (get-value! :current-page))])))]
    (http-delete (str "http://localhost:8193/documents/delete/" id)  onres)))

(defn add [event]
  (secretary/dispatch! "/documents/add"))

(defn render-documents [documents]
  [:div
   ;; [:div.padding]
   ;; [:div.page-header [:h1 "Record Room Management System"]]
   [:div#add]
   [:div#update]
   [:div {:class "box"}
    [:div {:class "box-header"}
     [:h3 "List of Documents"]]
    [:div.row
     [:div.col-md-12
      [:div.form-group
       [:div.col-sm-2 [:input.form-control {:id "dt1" :type "date"}]]
       [:div.col-sm-2 [:input.form-control {:id "dt2" :type "date"}]]
       [:div.col-sm-2 [:input.form-control {:id "dt" :type "text"
                                            :placeholder "Enter search text.."}]]
       [:input {:type "button" :value "Search"
                :class "btn btn-primary" :on-click search}]
       [:input {:type "button" :value "Add"
                :class "btn btn-primary" :on-click add}]
       ;; (url-format "#/documents/add" "Document")
       [:input {:id "getall" :type "button" :value "Refresh"
                :class "btn btn-primary" :on-click get-all-click}]]
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

                              ])]]
       [:div{:class "col-xs-6 col-centered col-max"} [shared-state 0]]
       ]
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
  (set-page! (get-value! :page-location)))

(defroute documents-list "/documents" []
  (if (is-authenticated?)(let [onres (fn [json]
                                       (let [dt (getdata json)]
                                         (set-key-value :documents dt)
                                         (set-key-value :total-pages (get-total-rec-no dt))
                                         (set-key-value :page-location  [render-documents (get-new-page-data (get-value! :documents)
                                                                                                             (get-value! :current-page))])))]
                           (http-get "http://localhost:8193/documents/all" onres))
      (set-key-value :page-location [login])))


(defroute documents-path "/documents/add" []
  (set-page! [document-template]))

(defroute documents-path1 "/documents/update/:id" [id]
 (set-page! [document-update-template id
              (first (filter (fn[obj]
                               (=(.-id obj) (.parseInt js/window id))) (get-value! :documents)))]))

(defroute "*" []
  (js/alert "<h1>Not Found Page</h1>"))


(defn main
  []
  (secretary/set-config! :prefix "#")
  (set-key-value :page-location [login])
  (r/render [page]
            (.getElementById js/document "app1"))
  (let [history (History.)]
    (events/listen history "navigate"
                   (fn [event]
                     (secretary/dispatch! (.-token event))))
    (.setEnabled history true)))

(defn nav! [token]
  (.setToken (History.) token))

(main)
