# build stage
FROM maven:latest AS build-env
ADD . /src
RUN cd /src && mvn -Dmaven.test.skip=true package

# final stage
FROM openjdk:14-jdk-alpine
WORKDIR /app
COPY --from=build-env /src/target/biddingapp-1.0-SNAPSHOT.jar /app/
COPY --from=build-env /src/biddingapp.yml /app/
RUN java -jar biddingapp-1.0-SNAPSHOT.jar db migrate biddingapp.yml
ENTRYPOINT java -jar biddingapp-1.0-SNAPSHOT.jar server biddingapp.yml
