# Use an official openjdk runtime as a parent image
FROM openjdk:8

# Define versions to install
ENV ANDROID_COMPILE_SDK 29
ENV ANDROID_BUILD_TOOLS 29.0.3
ENV ANDROID_SDK_TOOLS 4333796

# Define ANDROID_HOME directory
ENV ANDROID_HOME /usr/local/android-sdk-linux

# Install android SDK
RUN apt-get --quiet update --yes
RUN apt-get --quiet install --yes wget tar unzip lib32stdc++6 lib32z1
RUN wget --quiet --output-document=android-sdk.zip https://dl.google.com/android/repository/sdk-tools-linux-${ANDROID_SDK_TOOLS}.zip
RUN unzip -qq android-sdk.zip -d ${ANDROID_HOME}

RUN echo y | ${ANDROID_HOME}/tools/bin/sdkmanager --update | grep -v = || true

RUN echo y | ${ANDROID_HOME}/tools/bin/sdkmanager 'tools' | grep -v = || true
RUN echo y | ${ANDROID_HOME}/tools/bin/sdkmanager 'platform-tools' | grep -v = || true
RUN echo y | ${ANDROID_HOME}/tools/bin/sdkmanager 'build-tools;'${ANDROID_BUILD_TOOLS} | grep -v = || true
RUN echo y | ${ANDROID_HOME}/tools/bin/sdkmanager 'platforms;android-'${ANDROID_COMPILE_SDK} | grep -v = || true
RUN echo y | ${ANDROID_HOME}/tools/bin/sdkmanager 'extras;android;m2repository' | grep -v = || true
RUN echo y | ${ANDROID_HOME}/tools/bin/sdkmanager 'extras;google;google_play_services' | grep -v = || true
RUN echo y | ${ANDROID_HOME}/tools/bin/sdkmanager 'extras;google;m2repository' | grep -v = || true

ENV PATH "$PATH:${ANDROID_HOME}/tools/"
