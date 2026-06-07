#
# build
#

FROM maven:3.9.9-eclipse-temurin-21 as build
WORKDIR /opt/spring_boot_todo_api
COPY ./../pom.xml /opt/spring_boot_todo_api
COPY ./../src /opt/spring_boot_todo_api/src
RUN mvn clean package -DskipTests

#
# package
#

FROM eclipse-temurin:21
WORKDIR /opt/spring_boot_todo_api
COPY --from=build /opt/spring_boot_todo_api/target/spring_boot_todo_api-0.0.1-SNAPSHOT.jar ./api.jar
EXPOSE 8080
CMD ["java", "-jar", "/opt/spring_boot_todo_api/api.jar"]
