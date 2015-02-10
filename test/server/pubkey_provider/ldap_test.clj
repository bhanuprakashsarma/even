(ns server.pubkey-provider.ldap-test
  (:require
    [clojure.test :refer :all]
    [server.pubkey-provider.ldap :refer :all]
    [clj-ldap.client :as ldap]))


(deftest test-get-ldap-user-dn
  (is (= "uid=jdoe,ou=users,dc=example,dc=org" (get-ldap-user-dn "jdoe" {:base-dn "ou=users,dc=example,dc=org"}))))

(deftest test-ldap-config
  (is (= 123 (:connect-timeout (ldap-config {:connect-timeout "123"})))))

(deftest test-get-public-key
  (with-redefs [ldap/connect (constantly "conn")
                ldap/get (constantly {:sshPublicKey "public-key"})]
    (is (= "public-key" (get-public-key "jdoe" {:config {}})))))

(deftest test-get-networks
  (with-redefs [ldap/connect (constantly "conn")
                get-groups (constantly ["cn=group1,dc=example,dc=org", "cn=group2,dc=example,dc=org"])
                ldap/get (constantly {:ipHostNumber ["10.0.0.0/8"]})]
    (is (= [{:cidr ["10.0.0.0/8"]} {:cidr ["10.0.0.0/8"]}] (get-networks "jdoe" {:config {}})))))