(ns ynab-convert.core
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clj-time.core :as time]
            [clj-time.format :as format])
  (:gen-class))

(def memo-date-formatter (format/formatter "dd MMM yy"))
(def date-formatter (format/formatter "dd/MM/yyyy"))

(defn read-csv
  [filename]
  (with-open [reader (io/reader filename)]
    (->> reader
         (slurp)
         (string/trim)
         (csv/read-csv)
         (doall))))

(defn write-csv
  [filename lines]
  (with-open [writer (io/writer filename)]
    (csv/write-csv writer lines)))

(defn has-date-in-payee?
  [payee]
  (try
    (do
      (format/parse memo-date-formatter (subs payee 0 9))
      true)
    (catch IllegalArgumentException e
      false)))

(defn parse-or-date
  [line memo]
  (try
    (format/parse memo-date-formatter memo)
    (catch IllegalArgumentException e
      (format/parse date-formatter (nth line 0)))))

(defn clean-payee
  [payee]
  (string/trim (if (has-date-in-payee? payee) (subs payee 9) payee)))

(defn amount
  [money-in money-out]
  (- (BigDecimal. money-in) (BigDecimal. money-out)))

(defn parse-line
  [line]
  (let [[_ payee _ money-in money-out] line
        memo (subs payee 0 9)
        date (parse-or-date line memo)
        formatted-date (format/unparse date-formatter date)
        formatted-payee (clean-payee payee)]
    [formatted-date formatted-payee "" (amount money-in money-out)]))

(defn -main
  [& args]
  (->> (read-csv (first args))
       (next)
       (map parse-line)
       (into [["DATE" "PAYEE" "MEMO" "AMOUNT"]])
       (write-csv (second args))))
