# quarkus-dynamodb project

This project uses Quarkus and DynamoDB.

## Running the application

You can run this application by following below steps:

* mvn package

* docker build -f src/main/docker/Dockerfile.jvm -t quarkus/dynamodb-quickstart-jvm .

* docker-compose up

## Application Structure

This service is able to securely store session data from multiple microservices. This application is able share data between multiple authorized microservices.

This application becomes aware of applications that can and cannot share data among them via external variables:

* share.allow=A,B ( A and B can share data between them)
* share.notAllow=C (C can only access data that it itself stored)

When application comes up, it checks if the table structure to store the data is already in place, and if not, it creates the tables and loads initial data into it. 

Data is structured in the following way:

`{
    "Items": [
        {
            "serviceName": {
                "S": "C"
            },
            "sharedData": {
                "M": {
                    "four": {
                        "S": "40"
                    }
                }
            }
        }
     ]
 }`
 
 Each service will get it's own document identified by "serviceName" attribute in the database and data that can be shared will be stored under sharedData attribute as a Map. 
 
 ## Request Structure
 
 Different microservices can call this application by sending a JSON in a REST request. Following data can be sent:
 
 POST http://localhost:8090/storesessiondata 
 
 `{
    "serviceName":"B",
    "sharedData": {
        "KEY1": "VALUE",
        "KEY2": "VALUE"
    }

}`

GET http://localhost:8090/storesessiondata
`{
    "serviceName":"A",
    "key":"KEY1"
}`
