# Test roles app
> Security framework experiment implemented in kotlin and Spring Boot.

[![Build Status](https://travis-ci.com/gubanov/test-roles.svg?branch=master)](https://travis-ci.com/gubanov/test-roles)

## Structure
There are 3 modules:
* security-core
* security-persistent
* roles-app

### security-core
Contains framework interfaces and minimum set of default implementations to illustrate then approach 

### security-persistence 
Contains simple persistence layer for default implementations. 
Postgres SQL database is used for persistence storage

Schema is rolled out on application start via Flyway migration scripts.
Along with schema default admin user is also created, to login under the default admin use following credentials:
* username: admin@roles.org
* password: admin

Testcontainers is used in testing, therefore docker is required to run it.
Postgres alpine image is used in tests, it's automatically downloaded (~70MB) prior to running tests    

No ORM is used for persistence layer, spring-jdbc is used instead

### roles-app
Simple Spring MVC based web app for demonstration purposes.

Defines following security roles:
* ADMIN
* REVIEWER
* USER

Exports following endpoints:
* /auth, accessible to all
  * POST /auth - perform authentication
  * GET /auth - get authentication status
  * DELETE /auth - perform logout
  * PUT /auth - change password for authenticated user
* /users
  * POST /users/{email} - create new user, accessible to ADMIN role
  * GET /users/{email} - get user info, accessible to USER role
  * PUT /users/{email}/password - change user password, accessible to ADMIN
* /business
  * GET /admin-endpoint - dummy endpoint accessible by ADMIN role
  * GET /reviewer-endpoint - dummy endpoint accessible by REVIEWER role
  * GET /user-endpoint - dummy endpoint accessible by USER role

RolesAppMainTest - main test starting the whole thing and running basic test scenario in parallel
Testcontainers is also used here, so the same considerations as in security-persistence case apply here

## Building
To build app use provided gradle wrapper
```bash
./gradlew build
```
To build app without running tests use:
```bash
./gradlew build -x test
```

## Running
Application is packaged as single jar via Spring Boot. 
It can be run using docker-compose or as a standalone java application

### Docker Compose
Before starting docker compose check and if necessary update .env file.
After .env is configured run:
```bash
docker-compose up -d
``` 
Two images are required: alpine versions of jdk-8 and postgres-12

### Standalone
Java 8 or later is required to run the application
Also up and running instance of postgres SQL DB server is also required.
To run app:
```bash
java -DDATABASE_URL=<jdbc URL for Postgres instance> -DDATABASE_USERNAME=<username> -DDATABASE_PASSWORD=<password>  -jar roles-app/build/libs/roles-app-1.0-SNAPSHOT.jar
```
For example, to run app against Postgres instance from docker compose with default .env settings:
```bash
java -DDATABASE_URL=jdbc:postgresql://localhost:5432/test_roles_db -DDATABASE_USERNAME=test_roles -DDATABASE_PASSWORD=test_roles_pwd  -jar roles-app/build/libs/roles-app-1.0-SNAPSHOT.jar
```