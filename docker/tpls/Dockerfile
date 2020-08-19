FROM openjdk:11.0.6-jre-slim

MAINTAINER Andrey Serdtsev <andrey.serdtsev@gmail.com>

ARG VERSION=@version@
ENV jarFile=HomeMoney-@version@.war

RUN mkdir -p /opt/homemoney
COPY jars/${jarFile} /opt/homemoney

EXPOSE 8080
CMD  cd /opt/homemoney; java -Dspring.profiles.active=$PROFILE -jar ${jarFile}