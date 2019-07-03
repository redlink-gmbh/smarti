FROM openjdk:8-jre
LABEL maintainer="jakob.frank@redlink.co"
LABEL description="smarti - the 'smart' in assistify"

# we're runnin on port 8080
EXPOSE 8080/tcp

ENV HOME=/var/lib/smarti

# healthcheck --start-period=30s
HEALTHCHECK \
    CMD curl -sf http://localhost:8080/system/health || exit 1

RUN mkdir -p /opt/ext/
# Optional, currently deactivated, add some external libraries...
#ADD [\
#    "https://repo1.maven.org/maven2/edu/stanford/nlp/stanford-corenlp/3.8.0/stanford-corenlp-3.8.0.jar", \
#    "https://repo1.maven.org/maven2/edu/stanford/nlp/stanford-corenlp/3.8.0/stanford-corenlp-3.8.0-models-german.jar", \
#    "/opt/ext/"]
ADD target/classes/docker/*.properties target/classes/docker/*.xml ${HOME}/
ADD target/classes/docker/smarti.sh /opt/smarti.sh
RUN chmod a+x /opt/smarti.sh
ADD target/dependency/smarti.jar /opt/smarti.jar
ADD target/dependency/db-migrator.jar /opt/db-migrator.jar
ADD target/classes/db-migration/ /opt/db-migration/

# smarti shoud run as it's own user
RUN useradd -r -g 0 -d ${HOME} -s /sbin/nologin -c "smarti docker user" smarti && \
    chown -R smarti:0 ${HOME} && \
    chmod -R g=u ${HOME}

RUN chown -R smarti:0 /opt/ext/ && \
    chmod -R g=u /opt/ext/
USER smarti:0

VOLUME ${HOME}
WORKDIR ${HOME}

# inherit entrypoint to sub-images
ONBUILD ENTRYPOINT [ "/opt/smarti.sh" ]
ENTRYPOINT [ "/opt/smarti.sh" ]
# let's go!
CMD ["--smarti.required-providers.fail-on-missing=false"]
