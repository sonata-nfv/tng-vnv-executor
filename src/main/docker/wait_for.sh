#!/bin/sh

service_name=$1
path=$2

#getting last folder from path
path=$(echo ${path##*\\})
path=$(echo ${path##*/})

docker_container_name=$path"_"$service_name

#echo Checking $docker_container_name containers status

# getting number of instances
instances="$(docker-compose ps | grep ${docker_container_name}| wc -l)"
i=0

while [ $i -lt $instances ]
do
	container_name=$docker_container_name"_"$((i+1))
	exit=false

	while [ "$exit" != "true" ]
	do

		#echo Checking $container_name

		result="$(docker-compose ps | grep ${container_name})"
		exit_occurrences="$(echo $result | grep -o Exit | wc -l)"

		if [ "$exit_occurrences" -gt 0 ]
		then
			exit_code=$(echo ${result#*Exit })
			if [ "$exit_code" -eq 0 ]
			then
				exit=true
				i=$((i+1))
			fi
		fi
	done
done