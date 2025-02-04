# CHS Gov.uk Notify Integration

## 1.0) Introduction

An ACSP is an organisation that can file on behalf of their clients. 
The acsp-manage-users-api is a service for managing the memberships of users with ACSPs.
This Microservice exposes endpoints for creating memberships, changing a user's membership type, removing memberships, and retrieving data about memberships.

## 2.0) Prerequisites

This Microservice has the following dependencies:
- [Java 21](https://www.oracle.com/java/technologies/downloads/#java21)
- [Maven](https://maven.apache.org/download.cgi)

## 3.0) Tilt

This section explains how the endpoints in this Microservice can be executed in Tilt. In particular, Section 3.1 describes the steps that can be taken to run the Microservice in Tilt, Section 3.2 explains how the database can be populated with test data, and Section 3.3 describes the endpoints that can be executed based on this test data. 

### 3.1) Running the Microservice

To run this Microservice in Tilt, the `platform` and `chs-notification-sender-api` modules and `chs-notification-kafka-consumer` modules must be enabled.
These modules and services can be enabled by running the following commands in the `docker-chs-development` directory: 
- `./bin/chs-dev modules enable platform`
- `./bin/chs-dev modules enable chs-notification-sender-api`
- `./bin/chs-dev services enable chs-notification-kafka-consumer`

To run this Microservice in development mode, the following command can also be executed in the `docker-chs-development` directory:
- `./bin/chs-dev development enable chs-notification-sender-api`

After all of the services are enabled, use the `tilt up` command.

To enable debugging, create a new `Remove JVM Debug` configuration, and set the port to `9095`.

### 3.2) Populating MongoDB with Test Data 

This Microservice draws on four MongoDB collections:
1. `account.users`: This collection contains data relating to users. The pertinent data in a given document in this collection includes `_id`, `email`, and `display_name`. The Microservice uses `_id`  to uniquely identify a user, and `email` and `display_name` are items of data that are returned by some of the endpoints.
2. `account.oauth2_authorisations`: This collection contains data relating to user sessions. The pertinent data in this collection includes `token` and `user_details.user_id`, which are used to uniquely identify the session and user respectively. After upstream dependencies have been satisfied and this Microservice has been updated, `token_permissions` will also be important, because it will contain information about the actions that the user is permitted to perform in this Microservice.
3. `acsp_members.acsp_data`: This collection contains data relating to ACSPs. The pertinent data in a given document in this collection is `_id`, `acsp_name`, and `acsp_status`. `_id` uniquely identifies the ACSP, and `acsp_name` and `acsp_status` are data that returned by some of the endpoints.  This is a temporary MongoDB collection, and will be replaced by calls to the `acsp-profile-api`, after it has been built.
4. `acsp_members.acsp_members`: This collection contains data relating to memberships. A given document defines the association between a user and an ACSP. The most pertinent data in this collection include `_id`, `user_id`, `acsp_number`, which are used to uniquely identify the membership, user, and ACSP respectively. `user_role` and `status` are also important. `user_role` denotes the users membership type, which can be `owner`, `admin` or `standard`, and determines actions that the user is permitted to perform. `status` can be `active` and `removed`, which communicates whether the membership is currently active or not.

There is a Confluence page with MongoDB scripts that can be executed in `MONGOSH`, to populate the `account.users`, `acsp_members.acsp_data`, and `acsp_members.acsp_members` tables with test data. One could copy the MongoDB document from the `account.oauth2_authorisations` table in the Phoenix1 environment, where the `token=wEusf3QyFlp2ckUTPxIJLloWaLGRFq7H5PO1iynU465s9NmU0lngu5sCWbWjZxeBmv0WmVs6oQg` to their local `account.oauth2_authorisations` table; doing so will enable one to make calls as the demo user, which is one of the users in the test dataset. 
The Confluence page can be found at: [Inugami Test Data](https://companieshouse.atlassian.net/wiki/spaces/IDV/pages/4517724334/Inugami+Test+Data)

### 3.3) Running the Endpoints

The Microservice is hosted at `http://api.chs.local:4001`, so all of the endpoints can be called by appending the appropriate path to the end of this url.

All of the endpoints in this Microservice can either be run using `OAuth 2.0` or `API Key` authorisation headers. Eric can use these headers to enrich the request with additional headers. 
- If the endpoint is being called with `OAuth 2.0`, then in Postman, `Auth Type` must be set to `OAuth 2.0` and `token` must be set to a `token` from `account.oauth2_authorisations`.
- Otherwise, if the endpoint is being called with `API Key`, then in Postman, `Auth Type` must be set to `No Auth`, and an `Authorization` header needs to be added with a valid key e.g. `x9yZIA81Zo9J46Kzp3JPbfld6kOqxR47EAYqXbRX`

The High Level Design for the Microservice is available at: [HLD](https://companieshouse.atlassian.net/wiki/spaces/IDV/pages/5146247171/EMail+Service)

The remainder of this section lists the endpoints that are available in this Microservice, and provides links to detailed documentation about these endpoints e.g. required headers, path variables, query params, request bodies, and their behaviour.

| Method | Path                                    | Description                                                                                                                                                  | Documentation                                                                                                                                                |
|--------|-----------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------|
| POST   | /letters        | This endpoint can be used to form an association between a user and an ACSP.                                                                                 | [LLD - addMemberForAcsp](https://companieshouse.atlassian.net/wiki/spaces/IDV/pages/4748771441/LLD+-+POST+acsps+acsp_number+memberships)                     |
| POST   | /emails        | This endpoint can be used to form an association between a user and an ACSP.                                                                                 | [LLD - addMemberForAcsp](https://companieshouse.atlassian.net/wiki/spaces/IDV/pages/4748771441/LLD+-+POST+acsps+acsp_number+memberships)                     |

