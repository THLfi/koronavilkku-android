#!/bin/dash

set -e

JOB_STATUS=$(echo "$INPUT_STATUS" | tr '[:lower:]' '[:upper:]')
WORKFLOW_RUN_PATH="${GITHUB_SERVER_URL}/${GITHUB_REPOSITORY}/actions/runs/${GITHUB_RUN_ID}"

PAYLOAD=$(jq -n \
  --arg text "*$GITHUB_REPOSITORY: $INPUT_TEXT $JOB_STATUS*." \
  --arg wr_path "$WORKFLOW_RUN_PATH" \
  '{blocks:[{type:"section",text:{type:"mrkdwn",text: $text}},{type: "divider"},{type:"actions",elements:[{type:"button",text:{type:"plain_text",text:"View run",emoji:true},url:$wr_path}]}]}')

sh -c "curl -X POST -H 'Content-type: application/json' --data '$PAYLOAD' $SLACK_WEBHOOK_URL"
