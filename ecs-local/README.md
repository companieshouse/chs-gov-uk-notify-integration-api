# `ecs-local` README

## What is this?

* Directory created to run the app locally using resources nested within its own jar, just as it does when the app is 
deployed as docker container deployed to AWS ECS environments.
* Created to reproduce and troubleshoot an [issue](https://companieshouse.atlassian.net/browse/DEEP-369) seen when the
app is run from an ECS image built container, but not when it is run from the container we build locally using JIB.

## How to use this to reproduce the issue locally

* This set up really runs the app locally, outside of a container, but executing the app as a jar, just as the ECS 
container does. 

### Steps to run app locally from its jar and subject it to a fair weather letter sending request

#### Set up

1. Make sure you have set the Gov Notify key in `local_start.sh` to a valid 
value:

`export CHS_GOV_UK_NOTIFY_INTEGRATION_API_KEY=TODO_PROVIDE_THE_KEY_HERE`

2. Make sure you are not running `chs-gov-uk-integration-api` elsewhere on the same host (eg in docker) to avoid port clashes

`session 1`

Run the app from here.

1. `mvn clean package`
2. `cd ecs-local`
3. `. ./local_start.sh`

`session 2`

Send the fair weather send letter request from here.

1. `cd ecs-local`
2. `. ./send_letter_request.sh`


### Evidence seen of the issue occurring

#### Response seen

```
ABM-D3P9J7VK7N:ecs-local host$ . ./send_letter_request.sh
HTTP/1.1 403
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: DENY
Content-Length: 0
Date: Fri, 20 Jun 2025 08:16:26 GMT
```

#### Issue reported in app logs

```
{"data":{"message":"Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception 
[Request processing failed: java.lang.IllegalArgumentException: 
.../docker-chs-development/repositories/chs-gov-uk-notify-integration-api/target/
chs-gov-uk-notify-integration-api-unversioned.jar/!BOOT-INF/classes must be a regular file] with root cause"},
"created":"2025-06-20T08:34:42.971+01:00","event":"error"}
```