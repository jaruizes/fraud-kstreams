#!/bin/bash

#INITIAL: FIRST REGISTRATION OF SCHEMAS
curl --location --request POST 'http://localhost:8090/movements' --header 'Content-Type: application/json' --data-raw '{"id":"case_0-m1","card":"c0","amount":100.0,"origin": 1,"device": "shop-1","site": "site1","createdAt": "2022-07-20 11:00:55 CEST"}'
sleep 30

#ATM & SHOP
curl --location --request POST 'http://localhost:8090/movements' --header 'Content-Type: application/json' --data-raw '{"id":"case_1-m1","card":"c1","amount":10.0,"origin": 1,"device": "atm-1","site": "xxx","createdAt": "2022-07-20 11:17:05 CEST"}'
curl --location --request POST 'http://localhost:8090/movements' --header 'Content-Type: application/json' --data-raw '{"id":"case_1-m2","card":"c1","amount":90.0,"origin": 1,"device": "atm-2","site": "xxx","createdAt": "2022-07-20 11:17:15 CEST"}'
curl --location --request POST 'http://localhost:8090/movements' --header 'Content-Type: application/json' --data-raw '{"id":"case_1-m3","card":"c1","amount":100.0,"origin": 2,"device": "shop-1","site": "xxx","createdAt": "2022-07-20 11:17:35 CEST"}'


#ONLINE
curl --location --request POST 'http://localhost:8090/movements' --header 'Content-Type: application/json' --data-raw '{"id":"case_2-m1","card":"c2","amount":10.0,"origin": 3,"device": "xxx","site": "site1","createdAt": "2022-07-20 11:27:05 CEST"}'
curl --location --request POST 'http://localhost:8090/movements' --header 'Content-Type: application/json' --data-raw '{"id":"case_2-m2","card":"c2","amount":90.0,"origin": 3,"device": "xxx","site": "site2","createdAt": "2022-07-20 11:27:15 CEST"}'
curl --location --request POST 'http://localhost:8090/movements' --header 'Content-Type: application/json' --data-raw '{"id":"case_2-m3","card":"c2","amount":200.0,"origin": 3,"device": "xxx","site": "site3","createdAt": "2022-07-20 11:27:35 CEST"}'
curl --location --request POST 'http://localhost:8090/movements' --header 'Content-Type: application/json' --data-raw '{"id":"case_2-m4","card":"c2","amount":100.0,"origin": 3,"device": "xxx","site": "site4","createdAt": "2022-07-20 11:27:55 CEST"}'


#CLOSING ALL WINDOWS
curl --location --request POST 'http://localhost:8090/movements' --header 'Content-Type: application/json' --data-raw '{"id":"case_3-m1","card":"c3","amount":100.0,"origin": 1,"device": "shop-1","site": "xxx","createdAt": "2022-07-20 11:37:55 CEST"}'
