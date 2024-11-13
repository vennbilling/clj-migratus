(ns clj-migratus
  (:require [migratus.core :as migratus]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string :as s]))

(defn- load-edn-config [path]
  (when (.exists (io/file path))
    (-> (slurp path)
        edn/read-string)))

(defn- load-clj-config [path]
  (when (.exists (io/file path))
    (load-file path)))

(defn- load-config-from-property []
  (when-let [path (System/getProperty "migratus.config.path")]
    (let [uri (.toURI (io/resource path))
          rawPath (.getRawPath uri)]
      (cond
        (s/ends-with? path ".edn") (load-edn-config rawPath)
        (s/ends-with? path ".clj") (load-clj-config rawPath)))))

(defn load-config []
  (if-let [config (load-config-from-property)]
    config
    (if-let [default-edn (load-edn-config "migratus.edn")]
      default-edn
      (if-let [default-clj (load-clj-config "migratus.clj")]
        default-clj
        (throw (Exception. "Neither migratus.edn nor migratus.clj were found"))))))

(defn init [config]
  (migratus/init config))

(defn create [config name]
  (migratus/create config name))

(defn migrate [config]
  (migratus/migrate config))

(defn rollback [config]
  (migratus/rollback config))

(defn reset [config]
  (migratus/reset config))

(defn rollback-until-just-after [config id]
  (migratus/rollback-until-just-after config (Long/parseLong id)))

(defn up [config ids]
  (->> ids
       (map #(Long/parseLong %))
       (apply migratus/up config)))

(defn down [config ids]
  (->> ids
       (map #(Long/parseLong %))
       (apply migratus/down config)))

(defn pending-list [config]
  (migratus/pending-list config))

(defn migrate-until-just-before [config id]
  (migratus/migrate-until-just-before config (Long/parseLong id)))

(defn -main [& args]
  (let [command (first args)
        config (load-config)]
    (condp = command
      "init" (init config)
      "create" (create config (second args))
      "migrate" (migrate config)
      "rollback" (rollback config)
      "reset" (reset config)
      "rollback-until-just-after" (rollback-until-just-after config (second args))
      "up" (up config (rest args))
      "down" (down config (rest args))
      "pending-list" (println (pending-list config))
      "migrate-until-just-before" (migrate-until-just-before config (second args)))))
