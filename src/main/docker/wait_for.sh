#!/bin/sh

service_name=$1
project_name=$2
docker_compose_file=$3

docker_container_name=$project_name"_"$service_name

echo Checking $docker_container_name containers status

# getting number of instances
instances="$(docker-compose -f ${docker_compose_file} -p ${project_name} ps | grep ${docker_container_name}| wc -l)"
i=0

echo "docker-compose -f ${docker_compose_file} -p ${project_name} ps | grep ${docker_container_name}| wc -l"
echo Instances=$instances

while [ $i -lt $instances ]
do
	container_name=$docker_container_name"_"$((i+1))
	exitTrue=false

	while [ "$exitTrue" != "true" ]
	do
		echo Checking $container_name

        echo "docker-compose -f ${docker_compose_file} -p ${project_name} ps | grep ${container_name}"
		result="$(docker-compose -f ${docker_compose_file} -p ${project_name} ps | grep ${container_name})"
		exit_occurrences="$(echo $result | grep -o Exit | wc -l)"
                echo "exit_occurrences=$exit_occurrences"

		if [ "$exit_occurrences" -gt 0 ]
		then
			exit_code=$(echo ${result#*Exit })
			echo "exit_code=$exit_code"
			if [ "$exit_code" -eq 0 ]
			then
				exitTrue=true
				i=$((i+1))
			fi
			if [ "$exit_code" -ne 0 ]
			then
			    exit "$exit_code"
			fi
		fi
		sleep 5
	done
done

exit 0
