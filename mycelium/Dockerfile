FROM amazoncorretto:11
MAINTAINER Minh Duc Nguyen <minh.nguyen@ardc.edu.au>

COPY target/mycelium*.jar mycelium.jar
COPY src/main/resources/application.properties.sample application.properties
COPY src/main/resources/log4j2.xml.sample log4j2.xml

EXPOSE 8096

ENTRYPOINT ["java","-Dlogging.config=/log4j2.xml","-jar","/mycelium.jar"]