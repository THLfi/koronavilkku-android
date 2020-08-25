FROM ubuntu:20.04

RUN apt update && apt install -y curl jq

COPY entrypoint.sh /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
