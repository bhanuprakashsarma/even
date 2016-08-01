FROM registry.opensource.zalan.do/stups/openjdk:8-30

MAINTAINER Zalando SE

COPY target/even.jar /
COPY target/scm-source.json /

EXPOSE 8080

VOLUME /tmp

CMD java $JAVA_OPTS $(java-dynamic-memory-opts) $(newrelic-agent) $(appdynamics-agent) -jar /even.jar

