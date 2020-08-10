FROM openjdk:11.0.6-jre-slim

MAINTAINER Andrey Serdtsev <andrey.serdtsev@gmail.com>

ARG VERSION=$VERSION
ENV jarFile=HomeMoney.war

RUN mkdir -p /opt/homemoney
COPY target/${jarFile} /opt/homemoney

EXPOSE 8080
CMD  cd /opt/homemoney; java -jar ${jarFile}