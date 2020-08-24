# Use an official openjdk runtime as a parent image
FROM openjdk:8

# Define versions to install
ENV ANDROID_COMPILE_SDK 29
ENV ANDROID_BUILD_TOOLS 29.0.3
ENV ANDROID_CL_TOOLS 6609375

# Define ANDROID_HOME directory
ENV ANDROID_HOME /usr/local/android-sdk-linux
ENV GRADLE_USER_HOME /root/gradle_home

# Install android SDK
RUN apt-get --quiet update --yes
RUN apt-get --quiet install --yes wget tar unzip lib32stdc++6 lib32z1
RUN wget --quiet --output-document=android-cl.zip https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_CL_TOOLS}_latest.zip
RUN mkdir ${ANDROID_HOME}
RUN unzip -qq android-cl.zip -d ${ANDROID_HOME}/cmdline-tools

RUN echo y | ${ANDROID_HOME}/cmdline-tools/tools/bin/sdkmanager --update | grep -v = || true
RUN echo y | ${ANDROID_HOME}/cmdline-tools/tools/bin/sdkmanager 'tools' | grep -v = || true
RUN echo y | ${ANDROID_HOME}/cmdline-tools/tools/bin/sdkmanager 'platform-tools' | grep -v = || true
RUN echo y | ${ANDROID_HOME}/cmdline-tools/tools/bin/sdkmanager 'build-tools;'${ANDROID_BUILD_TOOLS} | grep -v = || true
RUN echo y | ${ANDROID_HOME}/cmdline-tools/tools/bin/sdkmanager 'platforms;android-'${ANDROID_COMPILE_SDK} | grep -v = || true
RUN echo y | ${ANDROID_HOME}/cmdline-tools/tools/bin/sdkmanager 'extras;android;m2repository' | grep -v = || true
RUN echo y | ${ANDROID_HOME}/cmdline-tools/tools/bin/sdkmanager 'extras;google;google_play_services' | grep -v = || true
RUN echo y | ${ANDROID_HOME}/cmdline-tools/tools/bin/sdkmanager 'extras;google;m2repository' | grep -v = || true
RUN yes | ${ANDROID_HOME}/cmdline-tools/tools/bin/sdkmanager --licenses

ENV PATH "$PATH:${ANDROID_HOME}/cmdline-tools/tools/"

COPY entrypoint.sh /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
