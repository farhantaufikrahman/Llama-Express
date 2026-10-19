# syntax=docker/dockerfile:1
FROM jenkins/jenkins:lts-jdk21

USER root

# JDK 25 untuk BUILD backend (Jenkins sendiri tetap berjalan di JDK 21 bawaan image)
COPY --from=eclipse-temurin:25-jdk-noble /opt/java/openjdk /opt/jdk-25

# Node 24 + npm untuk build & test frontend (MSW 3 butuh Node >= 22)
COPY --from=node:24-bookworm-slim /usr/local/bin/node /usr/local/bin/node
COPY --from=node:24-bookworm-slim /usr/local/lib/node_modules /usr/local/lib/node_modules
RUN ln -s /usr/local/lib/node_modules/npm/bin/npm-cli.js /usr/local/bin/npm \
 && ln -s /usr/local/lib/node_modules/npm/bin/npx-cli.js /usr/local/bin/npx

# Docker CLI + plugin buildx → build image lewat Docker Engine host (socket di-mount)
COPY --from=docker:cli /usr/local/bin/docker /usr/local/bin/docker
COPY --from=docker:cli /usr/local/libexec/docker/cli-plugins /usr/local/libexec/docker/cli-plugins

# kubectl versi stabil terbaru
RUN KUBECTL_VERSION="$(curl -fsSL https://dl.k8s.io/release/stable.txt)" \
 && curl -fsSLo /usr/local/bin/kubectl "https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/amd64/kubectl" \
 && chmod +x /usr/local/bin/kubectl

USER jenkins

# Plugin dipasang saat build image → Jenkins siap pakai, versi plugin ikut terkunci di image
COPY --chown=jenkins:jenkins plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt