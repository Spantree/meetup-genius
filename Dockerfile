FROM java:openjdk-8-jre

MAINTAINER Cedric Hurst <cedric@spantree.net>

ENV MEETUP_API_KEY=setme
ENV NEO4J_URL=http://neo4j:7474

WORKDIR /usr/src/app

COPY gradle /usr/src/app/gradle
COPY gradlew /usr/src/app/gradlew
COPY build.gradle /usr/src/app/build.gradle
COPY src /usr/src/app/src

RUN /usr/src/app/gradlew build -x test

CMD ["/usr/src/app/gradlew", "run", "-Dmeetup.api.key=${MEETUP_API_KEY}", "-Dneo4j.url=${NEO4J_URL}"]