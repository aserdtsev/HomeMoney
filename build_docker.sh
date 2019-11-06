#!/usr/bin/env bash

source .env
VERSION=$VERSION
mvn -Dmaven.test.skip=true install
docker build --build-arg VERSION=${VERSION} -t $IMAGE:${VERSION} .
