#!/usr/bin/env bash
source setup.sh

curl -sSX POST \
  --data-urlencode "script=$(<listClouds.groovy)" \
  -u "${JENKINS_USER_ID}:${JENKINS_API_TOKEN}" \
  "${JENKINS_URL}/scriptText"
