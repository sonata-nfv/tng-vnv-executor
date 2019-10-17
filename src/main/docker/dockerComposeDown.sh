#!/bin/sh

testId=$1
executionHost=$2
dockerPort=2375
echo TestId: ${testId} - executionHost: ${executionHost}

if [ ! -z "${executionHost}" -a "null" != "${executionHost}" ]
then
    echo Executing DOCKER_HOST="tcp://${executionHost}:${dockerPort}" docker-compose -f /executor/compose_files/${testId}-docker-compose.yml -p ${testId} down -v
    result="$(DOCKER_HOST="tcp://${executionHost}:${dockerPort}" docker-compose -f /executor/compose_files/${testId}-docker-compose.yml -p ${testId} down -v)"

else
    echo Executing docker-compose -f /executor/compose_files/${testId}-docker-compose.yml -p ${testId} down -v
    result="$(docker-compose -f /executor/compose_files/${testId}-docker-compose.yml -p ${testId} down -v)"
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