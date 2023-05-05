#!/usr/bin/env bash
# Required values for Jenkins API
#JENKINS_API_TOKEN=
#JENKINS_USER_ID=
#JENKINS_URL=

function require_tool() {
  if ! command -v $@ &> /dev/null; then echo "$@ is required but not available in PATH!"; exit 1; fi
}

function verify_env() {
    if [[ -z $(printenv $@) ]]; then echo "$@ must be set!"; exit 2; fi
}

verify_env "JENKINS_API_TOKEN"
verify_env "JENKINS_USER_ID"
verify_env "JENKINS_URL"

# Script variables
bundles_dir="bundles"
jcasc_subdir="jcasc"
items_subdir="items"
clouds_list="clouds-list.txt"
yaml_extension="yml"

# Verify tool installation
require_tool "curl"
require_tool "awk"
require_tool "wc"

# Generate a list of names of all shared Kubernetes configuration items
curl -sSX POST \
  --data-urlencode "script=$(<listClouds.groovy)" \
  -o "${clouds_list}" \
  -u "${JENKINS_USER_ID}:${JENKINS_API_TOKEN}" \
  "${JENKINS_URL}/scriptText"

# Ensure that items were found
count=$(wc -l < ${clouds_list})
if [[ ${count} < 1 ]]; then
  echo "No shared configuration items found in Operations Center at ${JENKINS_URL}"
  exit 3
else
  echo "Identified ${count} shared Kubernetes configurations from Operations Center"
fi
echo

# For each shared configuration item, export the CasC YAML for that cloud
for cloud in $(<${clouds_list}); do
  echo "Shared configuration '${cloud}':"

  # Identify the item and parent folder name from the item's path
  item_name=$(awk -F'[/]' '{print $NF}' <<< $cloud)
  project_name=$(awk -F'[/]' '{print $1}' <<< $cloud)
  availability_pattern="${project_name}\/.*"

  if [[ $project_name == kubernetes-shared-cloud ]]; then echo -e "- Skipping default cloud '${cloud}'\n"; continue; fi
  if [[ $project_name == $cloud ]]; then
    echo "- Cloud is at Jenkins root! YAML will be placed in a bundle called 'root'"
    project_name="root";
    availability_pattern=".*"
  fi

  # Set output file variables for the rest of this iteration
  bundle_dir="${bundles_dir}/${project_name}"
  bundle_file="${bundle_dir}/bundle.${yaml_extension}"
  jcasc_dir="${bundle_dir}/${jcasc_subdir}"
  jcasc_file="${jcasc_dir}/${item_name}.${yaml_extension}"
  items_dir="${bundle_dir}/${items_subdir}"
  items_file="${items_dir}/${item_name}.${yaml_extension}"

  if [[ ! -f "${bundle_dir}" ]]; then
    echo "- Creating configuration bundle for project '${project_name}'"
    mkdir -p "${bundle_dir}"
  fi

  if [[ -f "$bundle_file" ]]; then
    echo "  - Bundle manifest exists for bundle: '${project_name}'"
  else
    echo "  - Creating bundle manifest '${bundle_file}'"
    echo "apiVersion: 1
id: ${project_name}
description: Migrated clouds from project '${project_name}'
availabilityPattern: '${availability_pattern}'
version: 1
jcasc:
- ${jcasc_subdir}/
items:
- ${items_subdir}/" > "${bundle_file}"
  fi

  echo "- Creating entries for configuration '${item_name}' in bundle '${project_name}'"
  mkdir -p "${jcasc_dir}"
  mkdir -p "${items_dir}"

  echo "  - Exporting '${cloud}' configuration to CasC"
  echo "  - Creating JCasC YAML for this cloud (${jcasc_file})"
  if [[ -f "$jcasc_file" ]]; then
    echo "    - Overwriting existing file"
  fi
  curl -sSX POST \
    --data-urlencode "name=${item_name}" \
    --data-urlencode "script=$(<getCloudYaml.groovy)" \
    -o "${jcasc_file}" \
    -u "${JENKINS_USER_ID}:${JENKINS_API_TOKEN}" \
    "${JENKINS_URL}/scriptText"

  # Read the cloud name from the YAML file
  cloud_name=$(cat $jcasc_file | head -n 1 | cut -c3-)
  echo "  - Identified Kubernetes cloud name as '${cloud_name}'"
  echo "  - Creating item YAML for project folder '${project_name}' restricted to this cloud (${items_file})"
  if [[ -f "$items_file" ]]; then
    echo "    - Overwriting existing file"
  fi
  echo "removeStrategy:
  rbac: SYNC
  items: NONE
items:
- kind: folder
  name: ${project_name}
  properties:
  - kubernetes:
      permittedClouds:
      - ${cloud_name}" > "${items_file}"

  # Validate and format the YAML with yq, if available
  if command -v yq &> /dev/null; then
    echo "- Formatting YAML files with yq"
    yq -iY . $bundle_file
    yq -iY . $jcasc_file
    yq -iY . $items_file
  else
    echo "- yq is not available; skipping YAML formatting"
  fi

  echo
done

echo "Done!"
