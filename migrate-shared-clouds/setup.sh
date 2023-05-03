#!/usr/bin/env bash
if [[ -z "$JENKINS_API_TOKEN" ]]; then echo "JENKINS_API_TOKEN must be set"; exit; fi
if [[ -z "$JENKINS_USER_ID" ]]; then echo "JENKINS_USER_ID must be set"; exit; fi
if [[ -z "$JENKINS_URL" ]]; then echo "JENKINS_URL must be set"; exit; fi
