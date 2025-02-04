#!/bin/bash
#
# Start script for accounts-association-service

PORT=8080

exec java -jar -Dserver.port="${PORT}" "chs-gov-uk-notify-integration-api.jar"