FROM openjdk:8-jre
LABEL maintainer="jakob.frank@redlink.co"
LABEL description="smarti - the 'smart' in assistify"

EXPOSE 8080/tcp

ENV HOME=/var/lib/smarti

HEALTHCHECK --start-period=30s \
    CMD curl -sf http://localhost:8080/system/health || exit 1

#ADD [\
#    "https://repo1.maven.org/maven2/edu/stanford/nlp/stanford-corenlp/3.6.0/stanford-corenlp-3.6.0.jar", \
#    "https://repo1.maven.org/maven2/edu/stanford/nlp/stanford-corenlp/3.6.0/stanford-corenlp-3.6.0-models-german.jar", \
#    "/opt/ext/"]
ADD target/ext-dependency/ /opt/ext/
ADD target/classes/docker/ ${HOME}/
ADD target/dependency/smarti.jar /opt/smarti.jar

RUN groupadd -r smarti && \
    useradd -r -g smarti -d ${HOME} -s /sbin/nologin -c "smarti docker user" smarti && \
    chown -R smarti:smarti ${HOME}
USER smarti:smarti

VOLUME ${HOME}
WORKDIR ${HOME}
ENTRYPOINT [ "java", "-Dloader.path=BOOT-INF/lib,/opt/ext", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/opt/smarti.jar" ]
