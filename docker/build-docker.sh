#!/usr/bin/env bash

source .env
docker build --build-arg VERSION=${VERSION} -t $IMAGE:${VERSION} .
