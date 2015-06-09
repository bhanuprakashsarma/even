(ns org.zalando.stups.even.api-test

  (:require [clojure.test :refer :all]
            [org.zalando.stups.even.api :refer :all]
            [schema.core :as s]
            [org.zalando.stups.even.pubkey-provider.ldap :as ldap]
            [org.zalando.stups.even.ssh :as ssh]
            [org.zalando.stups.even.sql :as sql]
            ))

(deftest test-access-request-validation-fails
  (are [req] (thrown? Exception (validate-request req))
             {}
             {:username "a"}
             {:hostname "a"}
             ))

(deftest test-access-request-validation-succeeds
  (are [req] (= req (validate-request req))
             ; username is optional
             {:hostname "b" :reason "a"}
             {:username "my-user" :hostname "some.host" :reason "test"}
             {:username "my-user" :hostname "1.2.3.4" :reason "test"}
             ))

(deftest test-parse-authorization
  (is (= {:username "a" :password "b"} (parse-authorization "Basic YTpi"))))

(deftest test-ensure-username
  (is (= {:username "a" :blub "b"} (ensure-username {:username "a"} {:blub "b"}))))

(deftest test-request-access-wrong-network
  (with-redefs [
                ldap/get-networks (constantly [{:cidr ["1.0.0.0/8"]}])
                sql/create-access-request (constantly [])
                sql/update-access-request! (constantly nil)]
    (is (= {:status 403 :headers {} :body "Forbidden. Host /2.3.4.5 is not in one of the allowed networks: [{:cidr [1.0.0.0/8]}]"}
           (request-access-with-auth {:username "user1"} {:hostname "2.3.4.5"} {} {} {})))))

(deftest test-request-access-success
  (with-redefs [
                sql/create-access-request (constantly [])
                sql/update-access-request! (constantly nil)
                ldap/get-networks (constantly [{:cidr ["10.0.0.0/8"]}])
                ssh/execute-ssh (constantly {:exit 0})]
    (is (= {:status 200 :headers {} :body "Access to host /10.1.2.3 for user user1 was granted."}
           (request-access-with-auth {:username "user1"} {:hostname "10.1.2.3" :username "user1"} {} {} {})))))

(deftest test-request-no-auth
  (is (= {:status 401 :headers {} :body "Unauthorized. Please authenticate with username and password."} (request-access {:request {}} {} {} {} {}))))