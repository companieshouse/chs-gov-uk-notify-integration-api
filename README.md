# CHS Gov.uk Notify Integration

## 1.0) Introduction

This module accepts a call into a REST API endpoint, generates the payload that will be sent and then calls to [Gov.uk
Notify](https://www.notifications.service.gov.uk/)

The design for this module and the service it is a part of is
here : https://companieshouse.atlassian.net/wiki/spaces/IDV/pages/5146247171/EMail+Service

## 2.0) Prerequisites

This Microservice has the following dependencies:

- [Java 21](https://www.oracle.com/java/technologies/downloads/#java21)
- [Maven](https://maven.apache.org/download.cgi)

### 3.1) Running the Microservice

To run this Microservice in Tilt, the `platform` and `chs-notification-sender-api` modules and
`chs-notification-kafka-consumer` modules must be enabled.
These modules and services can be enabled by running the following commands in the `docker-chs-development` directory:

- `./bin/chs-dev modules enable platform`
- `./bin/chs-dev modules enable chs-notification-sender-api`
- `./bin/chs-dev modules enable chs-notification-kafka-consumer`

To run this Microservice in development mode, the following command can also be executed in the `docker-chs-development`
directory:

- `./bin/chs-dev development enable chs-notification-sender-api`

After all of the services are enabled, use the `tilt up` command.

To enable debugging, create a new `Remove JVM Debug` configuration, and set the port to `9095`.

### 3.3) Running the Endpoints

The Microservice is hosted at `http://api.chs.local:4001`, so all of the endpoints can be called by appending the
appropriate path to the end of this url.

All of the endpoints in this Microservice can either be run using `OAuth 2.0` or `API Key` authorisation headers. Eric
can use these headers to enrich the request with additional headers.

- If the endpoint is being called with `OAuth 2.0`, then in Postman, `Auth Type` must be set to `OAuth 2.0` and `token`
  must be set to a `token` from `account.oauth2_authorisations`.
- Otherwise, if the endpoint is being called with `API Key`, then in Postman, `Auth Type` must be set to `No Auth`, and
  an `Authorization` header needs to be added with a valid key e.g. `x9yZIA81Zo9J46Kzp3JPbfld6kOqxR47EAYqXbRX`

The High Level Design for the Microservice is available
at: [HLD](https://companieshouse.atlassian.net/wiki/spaces/IDV/pages/5146247171/EMail+Service)

# OWASP Dependency check

to run a check for dependency security vulnerabilities run the following command:

```shell
mvn dependency-check:check
```

# Listing dependencies

to get a list of all the libraries that are used in the project use the following command:

```shell
mvn dependency:tree
```

# Endpoints

The remainder of this section lists the endpoints that are available in this microservice, and provides links to
detailed documentation about these endpoints e.g. required headers, path variables, query params, request bodies, and
their behaviour.

| Method | Path                                                          | Description                                                | Documentation                                                                                                                                                                |
|--------|---------------------------------------------------------------|------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| POST   | /letter                                                       | This endpoint can be used to send a letter.                | [LLD - Gov.uk Notify Integration API](https://companieshouse.atlassian.net/wiki/spaces/IDV/pages/5162598548/Gov.uk+Notify+Integration+API+chs-gov-uk-notify-integration-api) |
| POST   | /email                                                        | This endpoint can be used to send an email.                | [LLD - Gov.uk Notify Integration API](https://companieshouse.atlassian.net/wiki/spaces/IDV/pages/5162598548/Gov.uk+Notify+Integration+API+chs-gov-uk-notify-integration-api) |
| GET    | http://127.0.0.1:8080/gov-uk-notify-integration/healthcheck   | this endpoint is used to check that the service is running |                                                                                                                                                                              |



