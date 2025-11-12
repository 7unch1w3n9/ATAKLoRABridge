# syntax=docker/dockerfile:1.7
ARG CMDLINE_TOOLS_VERSION=11076708_latest
ARG NDK_VERSION=27.0.12077973
ARG CMAKE_VERSION=3.22.1
ARG BUILD_TOOLS=34.0.0
ARG PLATFORM=29
ARG USERNAME=builder
ARG UID=1000
ARG GID=1000

FROM eclipse-temurin:11-jdk-jammy AS jdk11
FROM mirror.gcr.io/library/debian:bookworm AS build
ARG CMDLINE_TOOLS_VERSION NDK_VERSION CMAKE_VERSION BUILD_TOOLS PLATFORM USERNAME UID GID

RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates curl unzip git bash openssh-client \
    file make cmake ninja-build python3 pkg-config \
    gcc g++ libc6 libstdc++6 rsync \
    openjdk-17-jdk-headless \
    clang libclang1 \
 && rm -rf /var/lib/apt/lists/*


RUN groupadd -g ${GID} ${USERNAME} && useradd -m -u ${UID} -g ${GID} -s /bin/bash ${USERNAME}
USER ${USERNAME}
WORKDIR /home/${USERNAME}

ENV HOME=/home/${USERNAME}
ENV ANDROID_HOME=${HOME}/Android/Sdk
ENV ANDROID_SDK_ROOT=${ANDROID_HOME}
RUN mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools ${ANDROID_SDK_ROOT}/licenses

RUN curl -fsSL https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}.zip -o /tmp/cmdline-tools.zip \
 && unzip -q /tmp/cmdline-tools.zip -d /tmp/cmdline-tools \
 && mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools/latest \
 && mv /tmp/cmdline-tools/cmdline-tools/* ${ANDROID_SDK_ROOT}/cmdline-tools/latest/ \
 && rm -rf /tmp/cmdline-tools*

RUN export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && export PATH="$JAVA_HOME/bin:${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools:$PATH" \
 && yes | sdkmanager --licenses >/dev/null || true

RUN export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && export PATH="$JAVA_HOME/bin:${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools:$PATH" \
 && yes | sdkmanager "platform-tools" "platforms;android-${PLATFORM}" "build-tools;${BUILD_TOOLS}" "ndk;${NDK_VERSION}" "cmake;${CMAKE_VERSION}"


COPY --from=jdk11 /opt/java/openjdk /opt/java-11
ENV JAVA_HOME=/opt/java-11
ENV PATH=$JAVA_HOME/bin:${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools:$PATH


ENV ANDROID_NDK_HOME=${ANDROID_SDK_ROOT}/ndk/${NDK_VERSION}
ENV TOOLCHAIN_FILE=${ANDROID_NDK_HOME}/build/cmake/android.toolchain.cmake
ENV INSTALL_ROOT=/home/${USERNAME}/toolchain
ENV ABIS="arm64-v8a armeabi-v7a x86"
ENV BUILD_ROOT=/home/${USERNAME}/build
ENV DEPS_ROOT=/home/${USERNAME}/deps

COPY --chown=${USERNAME}:${USERNAME} . /app
WORKDIR /app

COPY --chown=${USERNAME}:${USERNAME} atak-docker/third_party/libusb/     /src/libusb/
COPY --chown=${USERNAME}:${USERNAME} atak-docker/third_party/hackrf/     /src/hackrf/
COPY --chown=${USERNAME}:${USERNAME} atak-docker/third_party/SoapySDR/   /src/SoapySDR/
COPY --chown=${USERNAME}:${USERNAME} atak-docker/third_party/SoapyHackRF/ /src/SoapyHackRF/

# build libusb (ndk-build) for all ABIs
RUN bash -lc 'set -euo pipefail; \
for ABI in ${ABIS}; do \
  echo "=== ndk-build libusb for $ABI ==="; \
  rm -rf /tmp/libusb_build && mkdir -p /tmp/libusb_build; \
  cd /src/libusb/android; \
  NDK_PROJECT_PATH=. \
  APP_PLATFORM=android-21 \
  APP_ABI=$ABI \
  NDK_LIBS_OUT=/tmp/libusb_build/libs \
  NDK_OUT=/tmp/libusb_build/obj \
  ${ANDROID_NDK_HOME}/ndk-build -j$(nproc); \
  mkdir -p ${INSTALL_ROOT}/$ABI/lib ${INSTALL_ROOT}/$ABI/include; \
  cp -v /tmp/libusb_build/libs/$ABI/libusb1.0.so ${INSTALL_ROOT}/$ABI/lib/; \
  rsync -a --delete /src/libusb/libusb/ ${INSTALL_ROOT}/$ABI/include/libusb/; \
done'



RUN bash -lc 'set -euo pipefail; \
for ABI in ${ABIS}; do \
  echo "=== build libhackrf for $ABI ==="; \
  mkdir -p ${BUILD_ROOT}/hackrf-$ABI; \
  cmake -S /src/hackrf/host/libhackrf -B ${BUILD_ROOT}/hackrf-$ABI \
    -DCMAKE_TOOLCHAIN_FILE=${TOOLCHAIN_FILE} \
    -DANDROID_ABI=$ABI \
    -DANDROID_PLATFORM=android-21 \
    -DCMAKE_BUILD_TYPE=Release \
    -DBUILD_SHARED_LIBS=ON \
    -DTHREADS_PREFER_PTHREAD_FLAG=ON \
    -DTHREADS_PTHREAD_ARG=-pthread \
    -DTHREADS_HAVE_PTHREAD_ARG=TRUE \
    -DLIBUSB_INCLUDE_DIR=${INSTALL_ROOT}/$ABI/include/libusb \
    -DLIBUSB_INCLUDE_DIRS=${INSTALL_ROOT}/$ABI/include/libusb \
    -DLIBUSB_LIBRARIES=${INSTALL_ROOT}/$ABI/lib/libusb1.0.so; \
  cmake --build ${BUILD_ROOT}/hackrf-$ABI -j; \
  cmake --install ${BUILD_ROOT}/hackrf-$ABI --prefix ${INSTALL_ROOT}/$ABI; \
done'


RUN bash -lc 'set -euo pipefail; \
for ABI in ${ABIS}; do \
  echo "=== build SoapySDR for $ABI ==="; \
  mkdir -p ${BUILD_ROOT}/soapysdr-$ABI; \
  cmake -S /src/SoapySDR -B ${BUILD_ROOT}/soapysdr-$ABI \
    -DCMAKE_TOOLCHAIN_FILE=${TOOLCHAIN_FILE} \
    -DANDROID_ABI=$ABI \
    -DANDROID_PLATFORM=android-24 \
    -DBUILD_SHARED_LIBS=ON \
    -DCMAKE_BUILD_TYPE=Release \
    -DTHREADS_PREFER_PTHREAD_FLAG=ON \
    -DTHREADS_PTHREAD_ARG=-pthread \
    -DTHREADS_HAVE_PTHREAD_ARG=TRUE \
    -DCMAKE_INSTALL_PREFIX=${INSTALL_ROOT}/$ABI; \
  cmake --build ${BUILD_ROOT}/soapysdr-$ABI -j; \
  cmake --install ${BUILD_ROOT}/soapysdr-$ABI; \
done'

RUN bash -lc 'set -euo pipefail; \
for ABI in ${ABIS}; do \
  echo "=== build SoapyHackRF for $ABI ==="; \
  mkdir -p ${BUILD_ROOT}/soapyhackrf-$ABI; \
  cmake -S /src/SoapyHackRF -B ${BUILD_ROOT}/soapyhackrf-$ABI \
    -DCMAKE_TOOLCHAIN_FILE=${TOOLCHAIN_FILE} \
    -DANDROID_ABI=$ABI \
    -DANDROID_PLATFORM=android-24 \
    -DCMAKE_BUILD_TYPE=Release \
    -DTHREADS_PREFER_PTHREAD_FLAG=ON \
    -DTHREADS_PTHREAD_ARG=-pthread \
    -DTHREADS_HAVE_PTHREAD_ARG=TRUE \
    -DHACKRF_LIBRARY=${INSTALL_ROOT}/$ABI/lib/libhackrf.so \
    -DHACKRF_INCLUDE_DIR=/src/hackrf/host/libhackrf/src \
    -DSoapySDR_DIR=${INSTALL_ROOT}/$ABI/share/cmake/SoapySDR \
    -DCMAKE_INSTALL_PREFIX=${INSTALL_ROOT}/$ABI; \
  cmake --build ${BUILD_ROOT}/soapyhackrf-$ABI -j; \
  cmake --install ${BUILD_ROOT}/soapyhackrf-$ABI; \
done'

# 用 BuildKit secret 挂载私钥来克隆私库（不依赖 SSH agent）
RUN --mount=type=secret,id=git_key,uid=1000,gid=1000,mode=0400 \
    bash -c 'set -euo pipefail; \
      mkdir -p ${DEPS_ROOT} ~/.ssh; \
      # 将私钥复制到用户目录并设置正确权限 \
      cp /run/secrets/git_key ~/.ssh/id_ed25519; \
      chmod 600 ~/.ssh/id_ed25519; \
      ssh-keyscan -H dev.seemoo.tu-darmstadt.de >> ~/.ssh/known_hosts 2>/dev/null; \
      if [ ! -d ${DEPS_ROOT}/futuresdr ]; then \
        GIT_SSH_COMMAND="ssh -o IdentitiesOnly=yes -o StrictHostKeyChecking=yes -i ~/.ssh/id_ed25519" \
        git clone -b atak-local --single-branch \
          git@dev.seemoo.tu-darmstadt.de:stud-theses/bsc-junchi-weng-futuresdr.git \
          ${DEPS_ROOT}/futuresdr; \
        cd ${DEPS_ROOT}/futuresdr; \
        git -c core.sshCommand="ssh -o IdentitiesOnly=yes -o StrictHostKeyChecking=yes -i ~/.ssh/id_ed25519" \
          submodule update --init --recursive || true; \
      fi; \
      # 清理私钥 \
      rm -f ~/.ssh/id_ed25519'


# === Rust toolchain for building libatak.so from the private repo ===
RUN curl -sSf https://sh.rustup.rs | sh -s -- -y --default-toolchain stable
ENV CARGO_HOME=/home/${USERNAME}/.cargo RUSTUP_HOME=/home/${USERNAME}/.rustup PATH=/home/${USERNAME}/.cargo/bin:$PATH
RUN cargo install cargo-ndk
RUN rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android

ENV RUST_CRATE_DIR=${DEPS_ROOT}/futuresdr/examples/atak

# .cargo/config.toml 
RUN mkdir -p "${RUST_CRATE_DIR}/.cargo" && \
    printf '%s\n' \
    '[target.aarch64-linux-android]' \
    'linker = "${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android21-clang"' \
    'ar     = "${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-ar"' \
    '' \
    '[target.armv7-linux-androideabi]' \
    'linker = "${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64/bin/armv7a-linux-androideabi21-clang"' \
    'ar     = "${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-ar"' \
    '' \
    '[target.i686-linux-android]' \
    'linker = "${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64/bin/i686-linux-android21-clang"' \
    'ar     = "${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-ar"' \
    '' \
    '[env]' \
    'PKG_CONFIG_ALLOW_CROSS = "1"' \
    > "${RUST_CRATE_DIR}/.cargo/config.toml"

# === Build libatak.so from /deps/futuresdr/examples/atak and copy into public plugin jniLibs ===
RUN bash -lc 'set -euo pipefail; \
for ABI in ${ABIS}; do \
  case "$ABI" in \
    arm64-v8a)   TRIPLE=aarch64-linux-android ; API=21 ;; \
    armeabi-v7a) TRIPLE=armv7-linux-androideabi ; API=21 ;; \
    x86)         TRIPLE=i686-linux-android ; API=21 ;; \
    *) echo "Unknown ABI: $ABI" >&2; exit 1 ;; \
  esac; \
  echo "=== cargo-ndk build libatak.so for $ABI (from private repo) ==="; \
  export SOAPYSDR_NO_PKG_CONFIG=1; \
  export SoapySDR_DIR=${INSTALL_ROOT}/$ABI; \
  export SOAPY_SDR_ROOT=${INSTALL_ROOT}/$ABI; \
  export SOAPYSDR_LIB_DIR=${INSTALL_ROOT}/$ABI/lib; \
  export SOAPYSDR_INCLUDE_DIR=${INSTALL_ROOT}/$ABI/include; \
  export BINDGEN_EXTRA_CLANG_ARGS="--sysroot=${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64/sysroot \
    -I${INSTALL_ROOT}/$ABI/include -target ${TRIPLE}${API} -fsigned-char"; \
  cd ${RUST_CRATE_DIR}; \
  cargo ndk -t $ABI build --release --lib -p atak; \
  ${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip -s target/${TRIPLE}/release/libatak.so || true; \
  mkdir -p /app/src/main/jniLibs/$ABI; \
  cp -v target/${TRIPLE}/release/libatak.so /app/src/main/jniLibs/$ABI/; \
done'


WORKDIR /app

RUN bash -lc 'set -euo pipefail; \
for ABI in ${ABIS}; do \
  mkdir -p app/src/main/jniLibs/$ABI; \
  cp -v ${INSTALL_ROOT}/$ABI/lib/libusb1.0.so   app/src/main/jniLibs/$ABI/ || true; \
  cp -v ${INSTALL_ROOT}/$ABI/lib/libhackrf.so   app/src/main/jniLibs/$ABI/ || true; \
  cp -v ${INSTALL_ROOT}/$ABI/lib/libSoapySDR.so app/src/main/jniLibs/$ABI/ || true; \
  shopt -s nullglob; \
  for m in ${INSTALL_ROOT}/$ABI/lib/SoapySDR/modules*/libHackRFSupport.so; do \
    cp -v "$m" app/src/main/jniLibs/$ABI/; \
  done; \
  shopt -u nullglob; \
done'

RUN mkdir -p /home/builder/.gradle && \
    printf '%s\n' \
    'org.gradle.unsafe.watch-fs=false' \
    'org.gradle.daemon=false' \
    'org.gradle.configureondemand=false' \
    'org.gradle.parallel=true' \
    'org.gradle.caching=true' \
    'org.gradle.vfs.watch=false' \
    > /home/builder/.gradle/gradle.properties

ENV GRADLE_USER_HOME=/home/builder/.gradle

ENV GRADLE_OPTS="-Dorg.gradle.unsafe.watch-fs=false -Dorg.gradle.daemon=false"

RUN chmod +x gradlew
RUN mkdir -p /home/${USERNAME}/.gradle && \
    printf '%s\n' \
    'org.gradle.java.home=/opt/java-11' \
    'org.gradle.java.installations.auto-detect=false' \
    'org.gradle.java.installations.paths=/opt/java-11' \
    'org.gradle.unsafe.watch-fs=false' \
    'org.gradle.daemon=false' \
    'org.gradle.configureondemand=false' \
    > /home/${USERNAME}/.gradle/gradle.properties

RUN ./gradlew assembleDebug --no-daemon --no-watch-fs --no-configure-on-demand

FROM scratch AS artifacts
COPY --from=build /app/app/build/outputs /outputs