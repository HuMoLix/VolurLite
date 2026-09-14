FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-21 AS maven_source

FROM --platform=$BUILDPLATFORM eclipse-temurin:25-jdk AS builder

COPY --from=maven_source /usr/share/maven /usr/share/maven
ENV MAVEN_HOME=/usr/share/maven
ENV PATH="${MAVEN_HOME}/bin:${PATH}"

WORKDIR /build

COPY pom.xml ./
COPY volur-framework/pom.xml volur-framework/
COPY volur-logging/pom.xml volur-logging/
COPY volur-system/pom.xml volur-system/
COPY volur-user/pom.xml volur-user/
COPY volur-verification/pom.xml volur-verification/
COPY volur-auth/pom.xml volur-auth/
COPY volur-challenge/pom.xml volur-challenge/
COPY volur-container/pom.xml volur-container/
COPY volur-competition/pom.xml volur-competition/
COPY volur-notification/pom.xml volur-notification/
COPY volur-server/pom.xml volur-server/

COPY . .

RUN mvn clean package -DskipTests

FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

ENV TZ=Asia/Shanghai

RUN apk add --no-cache docker-cli

COPY --from=builder /build/volur-server/target/volur-server.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseZGC", "-Duser.timezone=Asia/Shanghai", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]
