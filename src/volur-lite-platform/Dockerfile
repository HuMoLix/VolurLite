FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-21 AS maven_source

FROM --platform=$BUILDPLATFORM eclipse-temurin:25-jdk AS builder

COPY --from=maven_source /usr/share/maven /usr/share/maven
ENV MAVEN_HOME=/usr/share/maven
ENV PATH="${MAVEN_HOME}/bin:${PATH}"

WORKDIR /build

COPY settings.xml /usr/share/maven/conf/settings.xml
COPY settings.xml /root/.m2/settings.xml

COPY . .

RUN --mount=type=cache,target=/root/.m2/repository mvn package -DskipTests -T 1C

FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

ENV TZ=Asia/Shanghai

RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g' /etc/apk/repositories && \
    apk add --no-cache docker-cli

COPY --from=builder /build/volur-server/target/volur-server.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseZGC", "-Djava.awt.headless=true", "-Dsun.java2d.xrender=false", "-Dsun.java2d.pmoffscreen=false", "-Duser.timezone=Asia/Shanghai", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]
