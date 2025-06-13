# chs-gov-uk-notify-integration-api

```mermaid
flowchart LR
    ExternalApp["External CHS App"] -->|REST| Module1
    Module1["sender-api"] -->|Kafka| Module2
    Module2["kafka-consumer"] -->|REST| Module3
    Module3["📌 govuk-notify-api"] -->|REST| GovUKNotify
    GovUKNotify["GovUK Notify"]
    
    subgraph PoseidonSystem["🔱 chs-notification"]
        Module1
        Module2
        Module3
    end
    
    %% Styling for all elements - light/dark mode compatible
    classDef normal fill:#f8f8f8,stroke:#666666,stroke-width:1px,color:#333333,rx:4,ry:4
    classDef current fill:#0099cc,stroke:#007799,stroke-width:2px,color:white,rx:4,ry:4
    classDef external fill:#e6e6e6,stroke:#999999,stroke-width:1px,color:#333333,rx:4,ry:4
    classDef system fill:transparent,stroke:#0077b6,stroke-width:1.5px,stroke-dasharray:3 3,color:#00a8e8,rx:10,ry:10
    
    class Module1 normal;
    class Module2 normal;
    class Module3 current;
    class ExternalApp external;
    class GovUKNotify external;
    class PoseidonSystem system;
    %% Adding clickable links to GitHub repos
    click Module1 "https://github.com/companieshouse/chs-notification-sender-api" _blank
    click Module2 "https://github.com/companieshouse/chs-notification-kafka-consumer" _blank
```

## Overview

This service:
- Receives notification requests from chs-notification-kafka-consumer (Module 2)
- Sends email and letter notifications to GovUK Notify via their API
- Is Module 3 of 3 in the [chs-notification system](https://companieshouse.atlassian.net/wiki/spaces/IDV/pages/5146247171/EMail+Service)

## Related Services

- [chs-notification-sender-api](https://github.com/companieshouse/chs-notification-sender-api) (Module 1, accepts email/letter requests via REST and publishes to Kafka topics consumed by Module 2)
- [chs-notification-kafka-consumer](https://github.com/companieshouse/chs-notification-kafka-consumer) (Module 2, consumes from Kafka topics published by Module 1 and sends requests to Module 3 via REST)

## Endpoints

The service exposes the following endpoints:

- **Main API endpoints**: See [API Documentation](https://github.com/companieshouse/private.api.ch.gov.uk-specifications/tree/master/generated_sources/docs/chs-gov-uk-notify-integration-api)
- **Service health**: `GET /gov-uk-notify-integration/healthcheck`

## Prerequisites

- Java 21
- Maven

## Running Locally

### Prerequisites
Sign up to [GovUK Notify](https://www.notifications.service.gov.uk/) or get a API key from a teammate.

Set environment variables for GovUK Notify integration:
```bash
export CHS_GOV_UK_NOTIFY_INTEGRATION_API_KEY=your_api_key_here
```

### Running the Application

#### Option 1: Using IntelliJ IDEA
1. Open the project in IntelliJ
2. Set Project SDK to Java 21
3. Locate the main application class: [ChsGovUkNotifyIntegrationService.java](src/main/java/uk/gov/companieshouse/chs/gov/uk/notify/integration/api/ChsGovUkNotifyIntegrationService.java)
4. Right-click and select "Run" or "Debug"

#### Option 2: Using Maven CLI
```bash
mvn spring-boot:run
```

## Repository Structure

```
chs-gov-uk-notify-integration-api/
│── src/                    
│   ├── main/               # Main application code
│   └── test/               # Test code
│── pom.xml                 # Dependencies
│── api-collections/
│   └── postman/            # Postman API collections
│── ecs-image-build/        # ECS Dockerfile
│── terraform/              # Infrastructure code
│── ...                     # Other files/folders
└── README.md               # This file
```

## Adding a New Letter

### Steps to add a new template

1. Identify the client service in question, and create or reuse the letter assets directory for that client. We currently have the `assets/templates/letters/chips` directory for our CHIPS client application.
2. Start with a letter design - probably a PDF, and evolve HTML and CSS resources to reproduce the letter as a web document. Bear in mind that some of the formatting of these letters will be print media specific, so it is not until you are producing PDFs that you will know exactly what the resulting letter will look like.
3. Place common resources in the `assets/templates/letters/common` directory and those resources specific to the letter you are adding in the appropriate client directory (for example in `assets/templates/letters/chips`).
4. Set up context validation to ensure that the required variable values are present to be able to create, print and send valid letters out. Extend `TemplateContextValidator.VALID_CONTEXTS` with an additional entry to do so.
5. Test the result (of course!). 

### Background

Template resources for letters are to be grouped by the client service or app that originates the request to send a letter. In the first case we have developed so far, we are dealing with a "direction" letter request made by the CHIPS application. Its template resources thus reside in the `chips` letter assets directory.

We have a number of HTML and CSS resources starting with a "root" HTML template file named `directionLetter_v1.html` that brings in the other files to create the final template.  The `_v1` suffix tells us that these letters are versioned and we are currently working on version 1.

Resources that may be reused across various types of letter should be placed in the `common` directory.

The letter template that results from the resolving of the root template and its dependencies is a Thymeleaf HTML/CSS template with a number of substitution variables in it, covering those values that must change from letter to letter.

The application uses a Thymeleaf templating engine to perform the required substitutions. However, this will not validate for missing values for these substitutions. Hence, we build a validation model which essentially checks that the required values for the letter to be printed and sent have been provided. If we didn't do this validation, there would be the risk of printing and sending letters with essential information missing from them. 

