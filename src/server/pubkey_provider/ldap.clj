(ns server.pubkey-provider.ldap
    (:require
      [clojure.tools.logging :as log]
      [com.stuartsierra.component :as component]
      [clj-ldap.client :as ldap]
      [server.config :as config])
    )

(defrecord Ldap [config pool])

(defn get-ldap-user-dn [name {:keys [base-dn]}]
      "Build LDAP DN for a given user name"
      (str "uid=" name "," base-dn))

(defn ldap-config [{:keys [host bind-dn password ssl connect-timeout]}]
      {:host host
       :bind-dn bind-dn
       :password password
       :ssl? (Boolean/parseBoolean ssl)
       :connect-timeout (Integer/parseInt (or connect-timeout "10000"))})

(defn ldap-connect [{:keys [config pool] :as ldap-server}]
      (if pool
        pool
        (let [ldap-config (ldap-config config)]
             (log/info "Connecting to LDAP server " (config/mask ldap-config) + " ..")
             (let [conn (ldap/connect ldap-config)]
                  (assoc ldap-server :pool conn)
                  conn)
             )))

(defn get-public-key [name {:keys [config] :as ldap-server}]
      "Get a user's public SSH key"
      (let [conn (ldap-connect ldap-server)]
           (:sshPublicKey (ldap/get conn (get-ldap-user-dn name config) [:sshPublicKey]))))

(defn ^Ldap new-ldap [config]
      (log/info "Configuring LDAP with" (config/mask config))
      (map->Ldap {:config config}))