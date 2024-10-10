FROM quay.io/quarkus/ubi8/openjdk-21-runtime AS build
COPY . /project
WORKDIR /project

# Fix potential line ending issues
RUN tr -d '\r' < mvnw > mvnw.unix && mv mvnw.unix mvnw
RUN chmod +x mvnw

# Display Maven and Java versions
RUN ./mvnw --version
RUN java -version

# Run Maven build step by step
RUN ./mvnw clean -X
RUN ./mvnw compile -X
RUN ./mvnw test -X
RUN ./mvnw package -Pnative -DskipTests -X

FROM registry.access.redhat.com/ubi8/ubi-minimal:8.5

WORKDIR /work/
COPY --from=build /project/target/*-runner /work/application

RUN chmod 775 /work

EXPOSE 8080
CMD ["./application", "-Dquarkus.http.host=0.0.0.0"]
