#!/usr/bin/env bash

source .env
./build_docker.sh
docker push $IMAGE
