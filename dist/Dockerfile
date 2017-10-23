FROM openjdk:8-jre
LABEL maintainer="jakob.frank@redlink.co"
LABEL description="smarti - the 'smart' in assistify"

# we're runnin on port 8080
EXPOSE 8080/tcp

ENV HOME=/var/lib/smarti

# healthcheck
HEALTHCHECK --start-period=30s \
    CMD curl -sf http://localhost:8080/system/health || exit 1

RUN mkdir -p /opt/ext/
# Optional, currently deactivated, add some external libraries...
#ADD [\
#    "https://repo1.maven.org/maven2/edu/stanford/nlp/stanford-corenlp/3.6.0/stanford-corenlp-3.6.0.jar", \
#    "https://repo1.maven.org/maven2/edu/stanford/nlp/stanford-corenlp/3.6.0/stanford-corenlp-3.6.0-models-german.jar", \
#    "/opt/ext/"]
#RUN chmod -R a+r /opt/ext/
ADD target/classes/docker/ ${HOME}/
ADD target/dependency/smarti.jar /opt/smarti.jar

# smarti shoud run as it's own user
RUN groupadd -r smarti && \
    useradd -r -g smarti -d ${HOME} -s /sbin/nologin -c "smarti docker user" smarti && \
    chown -R smarti:smarti ${HOME}
USER smarti:smarti

VOLUME ${HOME}
WORKDIR ${HOME}

# inherit entrypoint to sub-images
ONBUILD ENTRYPOINT [ "java", "-Dloader.path=BOOT-INF/lib,/opt/ext", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/opt/smarti.jar" ]
ENTRYPOINT [ "java", "-Dloader.path=BOOT-INF/lib,/opt/ext", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/opt/smarti.jar" ]

# let's go!
CMD ["--smarti.required-providers.fail-on-missing=false"]
