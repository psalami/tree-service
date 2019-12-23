FROM maven:3.6.3-jdk-14

COPY src /usr/src/app/src
COPY pom.xml /usr/src/app
RUN mvn -f /usr/src/app/pom.xml clean package -DskipTests
EXPOSE 8080
CMD mvn -f /usr/src/app/pom.xml spring-boot:run