# Build image for the Woodpecker pipelines: JDK 21 and the Android SDK the project compiles against.
# Built once on the CI host: docker build -t health-journal-ci:1 -f .woodpecker/Dockerfile .woodpecker
FROM eclipse-temurin:21-jdk

ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=$PATH:/opt/android-sdk/cmdline-tools/latest/bin

RUN apt-get update \
 && apt-get install -y --no-install-recommends curl unzip git ca-certificates \
 && rm -rf /var/lib/apt/lists/*

# Command-line tools: take the current download name from https://developer.android.com/studio#command-line-tools-only
ARG CMDLINE_TOOLS=11076708
RUN mkdir -p $ANDROID_HOME/cmdline-tools \
 && curl -fsSL -o /tmp/tools.zip https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS}_latest.zip \
 && unzip -q /tmp/tools.zip -d $ANDROID_HOME/cmdline-tools \
 && mv $ANDROID_HOME/cmdline-tools/cmdline-tools $ANDROID_HOME/cmdline-tools/latest \
 && rm /tmp/tools.zip \
 && yes | sdkmanager --licenses > /dev/null \
 && sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"

# Gradle asks for build-tools 35 as well (AGP default for part of the build). Without it Gradle tries to
# download it during the build, which fails on a flaky connection.
RUN sdkmanager "build-tools;35.0.0"
