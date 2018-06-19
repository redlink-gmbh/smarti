#!/usr/bin/env bash

export BRANCH=${CIRCLE_BRANCH}
export SMARTI_VERSION="$(mvn -B -q -N exec:exec -Dexec.executable=echo -Dexec.args='${project.version}')"
export BUILD_FILE=smarti-${SMARTI_VERSION}.noarch.rpm # replace slashes from the branch name (e. g. "feature/...")
export DEPLOY_PATH=/tmp/build/

echo "BRANCH: ${BRANCH}"
echo "SMARTI_VERSION: ${SMARTI_VERSION}"
echo "BUILD_FILE: ${BUILD_FILE}"
echo "DEPLOY_PATH: ${DEPLOY_PATH}"
echo "AWS_REGION: ${AWS_REGION}"
echo "AWS_BUCKET: ${AWS_BUCKET}"
echo "AWS_ACCESS_KEY: ${AWS_ACCESS_KEY}"

# Install AWS-CLI - if it's there, this will be done quickly
sudo apt-get -y -qq update
sudo apt-get -y -qq install python-dev
curl -O https://bootstrap.pypa.io/get-pip.py
python get-pip.py --user
export PATH=~/.local/bin:$PATH
pip install awscli --upgrade --user

# in Circle-Ci, the containers already got a aws-config, so if it exsists, assume it's ok
if [ ! -f ~/.aws/credentials ]
  then
    mkdir -p ~/.aws
    echo "Creating AWS credentials from environment variables"
    echo "[default]
region=${AWS_REGION}
aws_access_key_id=${AWS_ACCESS_KEY}
aws_secret_access_key=${AWS_SECRET_KEY}" > ~/.aws/config
fi

cd ${DEPLOY_PATH}
aws s3 cp ${BUILD_FILE} s3://${AWS_BUCKET}/redlink/ --region ${AWS_REGION} --acl bucket-owner-full-control
# For dedicated branches, we tag the artifacts - this should actually be based on git tags
TARGET_ENVIRONMENT=undefined
if [ ${BRANCH} = master ]
  then
      TARGET_ENVIRONMENT=production
      # publish a new "latest"-file in order to make new clients be created with it
      aws s3 cp ${BUILD_FILE} s3://${AWS_BUCKET}/redlink/assistify-smarti-latest.rpm --region ${AWS_REGION} --acl bucket-owner-full-control
  else
    if [[ ${BRANCH} == develop ]] || [[ ${BRANCH} == "release/"* ]]
      then
        TARGET_ENVIRONMENT=test
    fi
  fi
aws s3api put-object-tagging --region ${AWS_REGION} --bucket ${AWS_BUCKET} --key redlink/${BUILD_FILE} --tagging "{ \"TagSet\": [ { \"Key\": \"environment\", \"Value\": \"${TARGET_ENVIRONMENT}\" }, { \"Key\": \"nodejs_version\", \"Value\": \"${NODEJS_VERSION}\" }, { \"Key\": \"nodejs_checksum\", \"Value\": \"${NODEJS_CHECKSUM}\" }, { \"Key\": \"assets\", \"Value\": \"${ASSETS_URL}\" } ] }"