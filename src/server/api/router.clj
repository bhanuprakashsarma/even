(ns server.api.router
  (:require
    [compojure.api.sweet :refer :all]
    [compojure.api.middleware :refer :all]
    [compojure.api.routes :as routes]
    [clojure.tools.logging :as log]
    [schema.core :as s]
    [server.pubkey-provider.ldap :refer [get-public-key ldap-auth?]]
    [server.ssh :refer [execute-ssh]]
    [clojure.data.codec.base64 :as b64]
    [ring.util.http-response :as http]
    ))

(def user-name-pattern
  "A valid POSIX user name (e.g. 'jdoe')"
  #"^[a-z][a-z0-9-]{0,31}$")
(def host-name-pattern
  "A valid FQDN or IP address"
  #"^[a-z0-9.-]{0,255}$")

(defn matches-user-name-pattern [s] (re-matches user-name-pattern s))
(defn matches-host-name-pattern [s] (re-matches host-name-pattern s))
(defn non-empty [s] (not (clojure.string/blank? s)))

(s/defschema AccessRequest
             {:user-name (s/both String (s/pred matches-user-name-pattern))
              :host-name (s/both String (s/pred matches-host-name-pattern))
              :reason (s/both String (s/pred non-empty))
              })

(defrecord Router [ldap ssh])


(defmethod compojure.api.meta/restructure-param :auth
  [_ authorization {:keys [parameters lets body middlewares] :as acc}]
  "Parse Authorization"
  (-> acc
      (update-in [:lets] into [{{authorization "authorization"} :headers} '+compojure-api-request+])
      (assoc :body `((if (string? ~authorization)
                       (do ~@body)
                       (http/unauthorized "Auth required"))))))

(defn serve-public-key [name ldap]
  (if (re-matches user-name-pattern name)
    (or (get-public-key name ldap)
        (http/not-found "User not found"))
    (http/bad-request "Invalid user name")))

(defn request-access [auth {:keys [host-name user-name] :as req} ssh ldap]
  (log/info "Requesting access for " req)
  (if (ldap-auth? auth ldap)
      (let [result (execute-ssh host-name (str "grant-ssh-access " user-name) ssh)]
        (if (= (:exit result) 0)
            (http/ok "Access granted")
            (http/bad-request (str "Failed: " result))))
      (http/forbidden "Login failed")))

(defn parse-authorization [authorization]
  "Parse HTTP Basic Authorization header"
  (-> authorization
      (clojure.string/replace-first "Basic " "")
      .getBytes
      b64/decode
      String.
      (clojure.string/split #":" 2)
      (#(zipmap [:username :password] %))))

(defn- api-routes [{:keys [ldap ssh]}]
  (routes/with-routes
    (swaggered
      "System"
      :description "Basic system operations"

      (GET*
        "/health" []
        :summary "Performs a health check"
        (log/info "Checking health")
        ; TODO perform a database connection
        (http/ok "OK")))

  (swaggered
    "Access Requests"
    :description "Manage access requests"
    (POST* "/access-requests" []
           :summary "Request SSH access to a single host"
           :return String
           :body [request AccessRequest]
           ;:header-params [authorization :- String]
           :auth authorization
           (request-access (parse-authorization authorization) request ssh ldap)))
  (swaggered
    "Public Keys"
    :description "Expose SSH public keys"
    (GET* "/public-keys/:name/sshkey.pub" [name]
          :summary "Download the user's SSH public key"
          (serve-public-key name ldap)))))


(defn- exception-logging [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log/error e "Caught exception in web-tier")
        (throw e)))))

(defn new-app [router]
  (api-middleware
    (routes/with-routes
      (swagger-ui)
      (swagger-docs :title "SSH Access Granting Service")
      (exception-logging (api-routes router)))))


(defn ^Router new-router []
  (log/info "Configuring router")
  (map->Router {}))