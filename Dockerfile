# syntax=docker/dockerfile:1.4
# Multi-stage Dockerfile to build a GraalVM native image and produce a small runtime image

# Stage 1: build jar with Maven (uses official Maven + JDK image)
FROM maven:3.9.4-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy only POMs first to take advantage of Docker layer caching for dependencies
COPY pom.xml ./
COPY simple-rag-server/pom.xml simple-rag-server/pom.xml
COPY simple-rag-nlp/pom.xml simple-rag-nlp/pom.xml

# Install system deps used during the build
RUN apt-get update && apt-get install -y libatomic1 && rm -rf /var/lib/apt/lists/*

# Prime the Maven cache using BuildKit cache mount
# Skipping priming dependency:go-offline here because it can fail when sibling module SNAPSHOTs
# are not available yet in the build context; the full build below will download dependencies
# using the same cache mount so subsequent builds will be faster.

# Copy remaining sources and perform the actual build using the cached Maven repo
COPY . /workspace
RUN --mount=type=cache,target=/root/.m2 \
	mvn -Pgraalvm-deps -pl simple-rag-server -am -DskipTests install \
	&& mvn -f simple-rag-server/pom.xml -Pgraalvm-deps -DskipTests spring-boot:process-aot package

# Stage 2: run GraalVM native-image to produce native executable
FROM ghcr.io/graalvm/native-image-community:21 AS native
WORKDIR /workspace
COPY --from=build /workspace/simple-rag-server/target/simple-rag-server-0.0.1-SNAPSHOT.jar /workspace/

# explode the Spring Boot fat jar and build native image using the image-provided native-image
# If the simple-rag-nlp module has downloaded WordNet files into its target/classes, copy them first
RUN mkdir -p exploded /workspace/wordnet \
 	&& if [ -d /workspace/simple-rag-nlp/target/classes/wordnet/dict ]; then \
 		cp -r /workspace/simple-rag-nlp/target/classes/wordnet/dict /workspace/wordnet/dict; \
 	fi \
 	&& (cd exploded && jar xf ../simple-rag-server-0.0.1-SNAPSHOT.jar) \
		&& for jar in exploded/BOOT-INF/lib/simple-rag-nlp-*.jar; do \
			 if [ -f "$jar" ]; then \
				 mkdir -p /workspace/wordnet-temp && (cd /workspace/wordnet-temp && jar xf "$jar"); \
				 if [ -d /workspace/wordnet-temp/wordnet/dict ]; then \
					 mkdir -p /workspace/wordnet && mv /workspace/wordnet-temp/wordnet/dict /workspace/wordnet/dict; \
				 fi; \
				 rm -rf /workspace/wordnet-temp; \
			 fi; \
		 done \
		&& mkdir -p /workspace/wordnet/dict \
	 && native-image --no-fallback --verbose --report-unsupported-elements-at-runtime -H:+ReportExceptionStackTraces \
		  -H:ClassInitialization=org.apache.commons.logging.LogFactory:build_time,org.apache.commons.logging.impl.SLF4JLogFactory:build_time \
		  -cp exploded:exploded/BOOT-INF/classes:exploded/BOOT-INF/lib/* \
	  io.github.jrohila.simpleragserver.SimpleRagServerApplication \
	  -H:Name=simple-rag-server-native

# Stage 3: small runtime image
FROM debian:stable-slim
RUN apt-get update \
	&& apt-get install -y --no-install-recommends zlib1g libssl3 libstdc++6 ca-certificates \
	&& rm -rf /var/lib/apt/lists/*
COPY --from=native /workspace/simple-rag-server-native /app/simple-rag-server
COPY --from=native /workspace/exploded /app/exploded
RUN chmod +x /app/simple-rag-server
# Copy OpenNLP model files included in the exploded classes
COPY --from=native /workspace/exploded/BOOT-INF/classes/models /app/models

# If WordNet dict was extracted in the native stage, copy it into /opt/wordnet/dict
COPY --from=native /workspace/wordnet/dict /opt/wordnet/dict
RUN if [ -d /opt/wordnet/dict ]; then chmod -R a+r /opt/wordnet/dict; fi

RUN chmod +x /app/simple-rag-server
ENV WNHOME=/opt/wordnet
EXPOSE 8080
ENTRYPOINT ["/app/simple-rag-server"]
