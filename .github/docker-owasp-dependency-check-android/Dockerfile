# Use an official openjdk runtime as a parent image
FROM openjdk:8

# Install dependencies
RUN apt-get --quiet update --yes
RUN apt-get --quiet install --yes lib32stdc++6 lib32z1

COPY entrypoint.sh /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
