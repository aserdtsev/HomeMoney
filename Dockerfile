FROM openjdk:11.0.2-jre-slim

MAINTAINER Andrey Serdtsev <andrey.serdtsev@gmail.com>

ARG VERSION=$VERSION
ENV jarFile=HomeMoney.war

# Fonts for JavaMelody
RUN apt-get update && apt install -y --no-install-recommends fontconfig
RUN fc-cache -r

RUN mkdir -p /opt/homemoney
COPY target/${jarFile} /opt/homemoney

EXPOSE 8080
CMD  cd /opt/homemoney; java -jar ${jarFile}