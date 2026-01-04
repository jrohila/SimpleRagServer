# syntax=docker/dockerfile:1.4
# Multi-stage Dockerfile to build a GraalVM native image and produce a small runtime image

# Stage 1: build jar with Maven (uses official Maven + JDK image)
FROM maven:3.9.4-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy only POMs first to take advantage of Docker layer caching for dependencies
COPY pom.xml ./
COPY simple-rag-server/pom.xml simple-rag-server/pom.xml

# Install system deps used during the build
RUN apt-get update && apt-get install -y libatomic1 && rm -rf /var/lib/apt/lists/*

# Prime the Maven cache using BuildKit cache mount
# Skipping priming dependency:go-offline here because it can fail when sibling module SNAPSHOTs
# are not available yet in the build context; the full build below will download dependencies
# using the same cache mount so subsequent builds will be faster.

# Copy remaining sources and perform the actual build using the cached Maven repo
COPY . /workspace
RUN --mount=type=cache,target=/root/.m2 \
    mvn -Pgraalvm-dev -DskipNativeBuild=true -Dexec.skip=true -pl simple-rag-server -am -DskipTests install \
    && mvn -f simple-rag-server/pom.xml -Pgraalvm-dev -DskipNativeBuild=true -Dexec.skip=true -DskipTests spring-boot:process-aot package

# Stage 2: run GraalVM native-image to produce native executable
FROM ghcr.io/graalvm/native-image-community:21 AS native
WORKDIR /workspace
COPY --from=build /workspace/simple-rag-server/target/simple-rag-server-0.0.1-SNAPSHOT.jar /workspace/
COPY --from=build /workspace/simple-rag-server/target/spring-aot /workspace/spring-aot

# Accept build args for exploded dir and binary name so Maven profiles can control destinations
ARG NATIVE_EXPLODED_DIR=/workspace/exploded
ARG NATIVE_BINARY_NAME=simple-rag-server-native

# explode the Spring Boot fat jar and build native image using the image-provided native-image
RUN mkdir -p ${NATIVE_EXPLODED_DIR} \
  && (cd ${NATIVE_EXPLODED_DIR} && jar xf ../simple-rag-server-0.0.1-SNAPSHOT.jar) \
  && native-image --no-fallback --verbose --report-unsupported-elements-at-runtime -H:+ReportExceptionStackTraces \
      -H:ClassInitialization=org.apache.commons.logging.LogFactory:build_time,org.apache.commons.logging.impl.SLF4JLogFactory:build_time \
      -cp /workspace/spring-aot/main/classes:${NATIVE_EXPLODED_DIR}:${NATIVE_EXPLODED_DIR}/BOOT-INF/classes:${NATIVE_EXPLODED_DIR}/BOOT-INF/lib/* \
    io.github.jrohila.simpleragserver.SimpleRagServerApplication \
    -H:Name=${NATIVE_BINARY_NAME}

# Stage 3: small runtime image
FROM debian:stable-slim

# Allow overriding model and binary destinations via build args (can be set from Maven profiles)
ARG NATIVE_EXPLODED_DIR=/workspace/exploded
ARG NATIVE_MODELS_DEST=/app/models
ARG NATIVE_BINARY_SRC=/workspace/simple-rag-server-native
ARG NATIVE_BINARY_DEST=/app/simple-rag-server

RUN apt-get update \
  && apt-get install -y --no-install-recommends zlib1g libssl3 libstdc++6 ca-certificates \
  && rm -rf /var/lib/apt/lists/*

COPY --from=native ${NATIVE_BINARY_SRC} ${NATIVE_BINARY_DEST}
COPY --from=native ${NATIVE_EXPLODED_DIR} /app/exploded
RUN chmod +x ${NATIVE_BINARY_DEST}
# Copy OpenNLP model files included in the exploded classes
COPY --from=native ${NATIVE_EXPLODED_DIR}/BOOT-INF/classes/models ${NATIVE_MODELS_DEST}

ENV WNHOME=/opt/wordnet
EXPOSE 8080
ENTRYPOINT ["/app/simple-rag-server"]
