ARG SOLR_VERSION=7.2-alpine
FROM solr:$SOLR_VERSION

LABEL maintainer=jakob.frank@redlink.co

# Add Cores required by smarti
ADD --chown=solr:solr target/solr-cores/smarti-application-*-solrcores.tar.gz \
    /opt/solr/server/solr/configsets

RUN mkdir -p /opt/solr/server/solr/conversation/data && \
    echo -e "name=conversation\nconfigSet=conversation\n" >/opt/solr/server/solr/conversation/core.properties;

# Expose Solr-Data dir
VOLUME ['/opt/solr/server/solr/conversation/data']
