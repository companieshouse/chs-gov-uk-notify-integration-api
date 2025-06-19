#!/bin/bash
#
# Start script for chs-gov-uk-notify-integration-api.

PORT=8080
export CHS_GOV_UK_NOTIFY_INTEGRATION_API_KEY=TODO_PROVIDE_THE_KEY_HERE
export API_URL=http://api.chs.local:4001
export MONGODB_DATABASE=notification
export MONGODB_URL=mongodb://localhost:27017
export CHS_URL=http://chs.local
export LOG_LEVEL=DEBUG
export JAVA_TOOL_OPTIONS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=*:17779"

exec java -jar -Dserver.port="${PORT}" "../target/chs-gov-uk-notify-integration-api-unversioned.jar"