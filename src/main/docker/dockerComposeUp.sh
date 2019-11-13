#!/bin/sh

testId=$1
executionHost=$2
dockerPort=2375
echo TestId: ${testId} - executionHost: ${executionHost}

if [ ! -z "${executionHost}" -a "null" != "${executionHost}" ]
then
    echo Executing DOCKER_HOST="tcp://${executionHost}:${dockerPort}" docker-compose -f /executor/compose_files/${testId}-docker-compose.yml -p ${testId} up
    result="$(DOCKER_HOST="tcp://${executionHost}:${dockerPort}" docker-compose -f /executor/compose_files/${testId}-docker-compose.yml -p ${testId} up)"

else
    echo docker-compose -f /executor/compose_files/${testId}-docker-compose.yml -p ${testId} up
    result="$(docker-compose -f /executor/compose_files/${testId}-docker-compose.yml -p ${testId} up)"
fi

echo "Result=$result"
exit_result="$(echo $result | grep -o exit | wc -l)"
echo "exit_result=$exit_result"

if [ "$exit_result" -gt 0 ]
then
    exit_code=$(printf ${result#*exited with code })
    echo "exit_code=${exit_code}"
    exit "$exit_code"
fi
exit 0