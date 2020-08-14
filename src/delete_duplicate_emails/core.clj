(ns delete-duplicate-emails.core
  (:require [clojure-mail.core :as mail]
            [clojure-mail.gmail :as gmail]
            [clojure-mail.message :refer (read-message) :as message]
            [crux.api :as crux]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [edn-query-language.core :as eql]
            [delete-duplicate-emails.config :as config])
  (:import [javax.mail Flags Flags$Flag UIDFolder]
           (com.sun.mail.imap IMAPStore)))

;; Start a Crux Node which will be used to store the emails in from the IMAP server which will keep the email.

(defn start-rocks-node [storage-dir]
  (crux/start-node {:crux.node/topology '[crux.standalone/topology
                                          crux.kv.rocksdb/kv-store]
                    :crux.standalone/event-log-dir (io/file storage-dir "event-log")
                    :crux.standalone/event-log-kv-store 'crux.kv.rocksdb/kv
                    :crux.kv/db-dir (str (io/file storage-dir "db"))}))

(def node (start-rocks-node "crux-store"))
#_ (.close node)


;; Utility functions.

(defn uuid [] (java.util.UUID/randomUUID))


;; Email functions (extending the functionality of https://github.com/owainlewis/clojure-mail)

(defn- custom-all-messages
  "Given a store and folder returns all messages
   reversed so the newest messages come first.
  If since-uid is provided, return all messages with newer or equal uid"
  #_([folder-name store] (custom-all-messages store folder-name))
  ([^IMAPStore store folder-name & {:keys [since-uid]}]
   (let [folder (mail/open-folder store folder-name :readwrite)]
     (->> (if-not since-uid
            (.getMessages folder)
            (.getMessagesByUID folder since-uid javax.mail.UIDFolder/LASTUID))
       reverse))))

(defn- custom-inbox
  "Get n messages from your inbox"
  ([store] (custom-all-messages store "inbox")))

(defn- mark-deleted
  "Set DELETED flag on a message"
  [msg]
  (.setFlag msg Flags$Flag/DELETED true))


(defn- contained-in-source?
  "A function to return true if the given parsed-message has a twin in the database."
  [parsed-message]
  (not= #{} (crux/q (crux/db node)
              {:find '[e]
               :where [['e :message/id (:id parsed-message)]]})))


;; Connecting to the inboxes

(def source-store (mail/store
                    "imaps"
                    (:host config/source)
                    (:email config/source)
                    (:password config/source)))

(def source-inbox (mail/inbox source-store))

(def destination-store (mail/store
                         "imaps"
                         (:host config/destination)
                         (:email config/destination)
                         (:password config/destination)))

;; Other than with the source, we got to load the inbox using a read-write mode with the destination.
(def destination-inbox (custom-inbox destination-store))


;; Functions doing the work.

(defn load-messages-from-source
  "Function that can be used from the REPL to ingest the emails from source into the local CRUX instance."
  []
  (time
    (doseq [message source-inbox]
      (let [parsed-message (read-message message)]
        (do
          #_(println "inserted" (:id parsed-message) "with subject" (:subject parsed-message))
          (crux/submit-tx node
            [[:crux.tx/put
              {:crux.db/id (keyword (:id parsed-message))
               :message/id (:id parsed-message)
               :message/from (:from parsed-message)
               :message/sender (:sender parsed-message)
               :message/to (:to parsed-message)
               :message/subject (:subject parsed-message)
               :message/date-sent (:date-sent parsed-message)
               :message/date-received (:date-received parsed-message)}]]))))))

(defn remove-duplicates-from-destination
  "Function that can be used from the REPL to iterate over the emails in the destination and delete the email if has an entry in the CRUX DB."
  []
  (let [number-of-emails (count destination-inbox)]
    (do (println "Found" number-of-emails "emails in the destination, starting to look and deleting duplicates..."))
    (->> destination-inbox
      (map (fn [message]
             [message (read-message message)]))
      (filter #(contained-in-source? (nth % 1)))
      #_((fn [output]
           (do
             (clojure.pprint/pprint (nth (first output) 1))
             (mark-deleted (nth (first output) 0)))))
      (map (fn [email]
             (do
               (mark-deleted (nth email 0))
               (let [parsed-message (nth email 1)]
                 (println "Deleted email with id" (:id parsed-message) "from" (:address (first (:from parsed-message))) "with subject \"" (:subject parsed-message) "\"received at" (:date-received parsed-message))))))
      doall)
    (println "done")))

