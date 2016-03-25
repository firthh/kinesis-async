(ns kineis-async.core
  (:require [gloss.io :as io]
            [gloss.core :refer :all]
            [clojure.core.async :as async]
            [amazonica.aws.kinesis :as kinesis])
  (:import [com.amazonaws.services.kinesis.clientlibrary.lib.worker
            InitialPositionInStream
            KinesisClientLibConfiguration
            Worker])
  (:gen-class))

(defcodec utf-8 (string :utf-8))

(def decode (partial io/decode utf-8))

(defn- unwrap [byte-buffer]
  (decode byte-buffer))

(defn processor [channel]
  (fn [records]
    (loop [[record & records] records]
      (if-not record
        true ; if we run out of records we return true so kinesis checkpoints
        (do
          (prn "pushing record onto first queue")
          (if (async/>!! channel record) ; will return false if channel is closed so we will return false so checkpointing does not occur
            (recur records)
            (println "Channel closed, returning false")))))))

(defn start-worker [{:keys [credentials
                            region
                            stream-name
                            application-name]
                     :or {credentials {}}}]
  (let [chan (async/chan)
        chan2 (async/chan)]
    (let [[^Worker worker uuid] (kinesis/worker :app application-name
                                                :credentials (assoc (or credentials {}) :endpoint region)
                                                :region-name region
                                                :endpoint (format "kinesis.%s.amazonaws.com" region)
                                                :stream stream-name
                                                :deserializer unwrap
                                                :initial-position-in-stream "TRIM_HORIZON"
                                                :checkpoint false
                                                :worker-id application-name
                                                :processor (processor chan))]
      (future (.run worker))
      (async/go-loop []
        (let [record (async/<! chan)]
          (prn "about to put record on second queue")
          (if (async/>!! chan2 record)
            (recur)
            (do (prn "closing second channel")
                (.shutdown worker)
                (async/close! chan)
                ))))
      chan2)))



