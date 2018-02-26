FROM debian:stable-slim

ENV DEBIAN_FRONTEND noninteractive
RUN apt-get update -qq \
  && apt-get install -qq -y --no-install-recommends \
  python \
  python-pip \
  && rm -rf /var/lib/apt/lists/*

RUN pip install requests && \
  pip install setuptools && \
  pip install wheel && \
  pip install unittest2 && \
  pip install argparse && \
  pip install loremipsum

ADD RC-Smarti-API-Dataset.py /tmp

ENTRYPOINT ["python", "/tmp/RC-Smarti-API-Dataset.py"]