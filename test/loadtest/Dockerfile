FROM debian:stable-slim

ENV DEBIAN_FRONTEND noninteractive
RUN apt-get update -qq \
    && apt-get install -qq -y --no-install-recommends \
    locales \
	jq \
	curl \
	fortune-mod \
	fortunes-de \
    && rm -rf /var/lib/apt/lists/*
RUN echo "de_DE.UTF-8 UTF-8" > /etc/locale.gen && \
    locale-gen de_DE.UTF-8 && \
    dpkg-reconfigure locales && \
    /usr/sbin/update-locale LANG=de_DE.UTF-8

ENV LC_ALL de_DE.UTF-8
ENV FORTUNE=/usr/games/fortune

ADD loadTest.sh /

ENTRYPOINT ["/loadTest.sh"]
