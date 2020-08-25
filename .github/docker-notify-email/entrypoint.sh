#!/bin/dash

set -e

JOB_STATUS=$(echo "$INPUT_STATUS" | tr '[:lower:]' '[:upper:]')
WORKFLOW_RUN_PATH="${GITHUB_SERVER_URL}/${GITHUB_REPOSITORY}/actions/runs/${GITHUB_RUN_ID}"

SUBJECT="$GITHUB_REPOSITORY: OWASP Dependency Check ran with status: $JOB_STATUS"
BODY="<p><b>$SUBJECT</b>.</p><p>$INPUT_TEXT</p><p><a href=\"$WORKFLOW_RUN_PATH\">View run</a></p>"

RECIPIENTS_JSON=""

generate_recipients() {
  RECIPIENTS_ARR=$(awk -v r="$RECIPIENTS" 'BEGIN { split( r, array, ","); for (i=1;i<=length(array);i++) { print array[i]; } }')

  for I in $RECIPIENTS_ARR; do
    RECIPIENT=$(jq -n --arg r "$I" '{email: $r}')
    RECIPIENTS_JSON="$RECIPIENTS_JSON$RECIPIENT,"
  done

  RECIPIENTS_JSON="${RECIPIENTS_JSON%?}"
}

generate_recipients

PAYLOAD=$(jq -n \
  --argjson rec "[$RECIPIENTS_JSON]" \
  --arg from "$FROM" \
  --arg subject "$SUBJECT" \
  --arg body "$BODY" \
  '{personalizations: [{to: $rec}], from: {email: $from},subject: $subject,content: [{type: "text/html", value: $body}]}')

sh -c "curl --request POST --url $SEND_URL --header 'Authorization: Bearer $API_KEY' --header 'Content-Type: application/json' --data '$PAYLOAD'"
