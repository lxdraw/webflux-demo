FROM adoptopenjdk/openjdk11:alpine-jre

WORKDIR /usr/src/app

COPY  build/libs/webflux-demo.jar .

EXPOSE 8080
ENTRYPOINT java -Djava.security-egd=/dev/./urandom -jar webflux-demo.jar
