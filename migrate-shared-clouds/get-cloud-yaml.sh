#!/usr/bin/env bash
source setup.sh

item_name="$@"
if [[ -z "$item_name" ]]; then
  echo "Must provide a name! (e.g. './get-cloud-yaml.sh kubernetes-shared-cloud')";
  exit 1;
fi

curl -sSX POST \
  --data-urlencode "name=${item_name}" \
  --data-urlencode "script=$(<getCloudYaml.groovy)" \
  -u "${JENKINS_USER_ID}:${JENKINS_API_TOKEN}" \
  "${JENKINS_URL}/scriptText"
