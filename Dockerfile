FROM java:8

MAINTAINER Andrey Serdtsev <andrey.serdtsev@gmail.com>

ARG VERSION=$VERSION
ENV jarFile=HomeMoney.war

RUN mkdir -p /opt/homemoney
COPY target/${jarFile} /opt/homemoney

EXPOSE 8080
CMD  cd /opt/homemoney; java -jar ${jarFile}