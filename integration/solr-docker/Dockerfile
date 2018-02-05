ARG SOLR_VERSION=7.2-alpine
FROM solr:$SOLR_VERSION

LABEL maintainer=jakob.frank@redlink.co

# Add Cores required by smarti
ADD --chown=solr:solr target/solr-cores/smarti-application-*-solrcores.tar.gz \
    /opt/solr/server/solr/configsets

RUN for c in conversation chatpal; do \
    mkdir -p /opt/solr/server/solr/${c}/data && \
    echo -e "name=${c}\nconfigSet=${c}\n" >/opt/solr/server/solr/${c}/core.properties; \
  done

# Expose Solr-Data dir
VOLUME ['/opt/solr/server/solr/conversation/data', '/opt/solr/server/solr/chatpal/data']
