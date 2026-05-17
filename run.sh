#!/usr/bin/env bash
# Launcher for the New2Canada server.
# Builds the project (skips if already built) and runs it on http://localhost:8080.
#
# Usage:
#   ./run.sh         # build (if needed) then run
#   ./run.sh build   # force-rebuild
#   ./run.sh clean   # mvn clean
set -e
cd "$(dirname "$0")"

# Java 17 from Homebrew (keg-only, so we point at it explicitly).
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

# IPv6-preferred for Maven + the runtime JVM. On some Wi-Fi/hotspot networks
# (e.g. iPhone Personal Hotspot), the IPv4 route to Cloudflare-fronted hosts
# is blocked; IPv6 still works.
export MAVEN_OPTS="-Djava.net.preferIPv6Addresses=true -Dmaven.wagon.http.connectionTimeout=60000 -Dmaven.wagon.http.readTimeout=120000 -Dmaven.wagon.http.retryHandler.count=5"

case "${1:-}" in
  clean) mvn clean ;;
  build) mvn clean package ;;
  *)
    if [ ! -f target/new2canada.jar ]; then
      echo "→ First run: building with Maven (1–2 min)…"
      mvn clean package -q
    fi
    echo "→ Starting New2Canada on http://localhost:8080"
    # Use Java's default address-family ordering so we can reach both IPv4-only
    # and IPv6-only targets (Cloudflare-fronted vs Wikipedia, etc.).
    exec java -jar target/new2canada.jar
    ;;
esac
