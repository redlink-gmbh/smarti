ARG SOLR_VERSION=7-alpine
FROM solr:$SOLR_VERSION

LABEL maintainer=jakob.frank@redlink.co

# Add Cores required by smarti
ADD --chown=solr:0 target/solr-cores/smarti-application-*-solrcores.tar.gz \
    /opt/solr/server/solr/configsets

RUN mkdir -p /opt/solr/server/solr/conversation/data && \
    echo -e "name=conversation\nconfigSet=conversation\n" >/opt/solr/server/solr/conversation/core.properties;

# Fix file-permissions for openshift
USER root
RUN chown -R solr:0 /opt/solr && chmod -R g=u /opt/solr
USER solr

# Expose Solr-Data dir
VOLUME ['/opt/solr/server/solr/conversation/data']
