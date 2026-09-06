# Stage 1: build pomoci projektu vlasniho Gradle wrapperu
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew installDist --no-daemon

# Stage 2: runtime — JRE + curl pro healthcheck
FROM eclipse-temurin:17-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /app/build/install/backend/ ./backend
EXPOSE 8090
CMD ["./backend/bin/backend"]
