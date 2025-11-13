#!/bin/bash
#########################################################
# Script to invoke make through a latex docker container.
# This allows to build/clean the document without install
# of any latex distribution on your host. Has the benefit
# of being reproducible as well.

# Check whether docker image is present on machine. Pull
# the image if not.
pull_if_missing() {
	image=$1
	if [[ ! "$(docker images -q '${image}' 2> /dev/null)" == "" ]]; then
	 	docker pull "${image}"
	fi
}


# Run make through docker container to make given target.
docker_run() {
	image=$1
	target=$2
	docker run \
		--volume=$(pwd):/work \
		--workdir=/work \
		--user $(id -u):$(id -g) \
		"${image}" \
		make "${target}"
}


#########################################################
# Execute whatever make target was passed
target=${1:-"all"}
image="texlive/texlive"

pull_if_missing "${image}"
docker_run "${image}" "${target}"
