#!/usr/bin/env bash

set -Eeuo pipefail

readonly APP_DIR="${APP_DIR:-/home/ubuntu/dulpick}"
readonly MYSQL_CONTAINER="${MYSQL_CONTAINER:-mysql}"
readonly DOCKER_NETWORK="${DOCKER_NETWORK:-short-net}"
readonly ENV_FILE="${APP_DIR}/.env"

if ! docker container inspect "${MYSQL_CONTAINER}" >/dev/null 2>&1; then
    echo "MySQL container not found: ${MYSQL_CONTAINER}" >&2
    exit 1
fi

if ! docker network inspect "${DOCKER_NETWORK}" >/dev/null 2>&1; then
    echo "Docker network not found: ${DOCKER_NETWORK}" >&2
    exit 1
fi

mkdir -p "${APP_DIR}"
chmod 700 "${APP_DIR}"
mkdir -p "${APP_DIR}/secrets"
chmod 700 "${APP_DIR}/secrets"

if [[ -f "${ENV_FILE}" ]]; then
    db_password="$(sed -n 's/^DB_PASSWORD=//p' "${ENV_FILE}")"
else
    db_password="$(openssl rand -hex 32)"
fi

if [[ -z "${db_password}" ]]; then
    echo "DB_PASSWORD is missing in ${ENV_FILE}" >&2
    exit 1
fi

printf '%s\n' \
    "CREATE DATABASE IF NOT EXISTS dulpick_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;" \
    "CREATE USER IF NOT EXISTS 'dulpick'@'%' IDENTIFIED BY '${db_password}';" \
    "ALTER USER 'dulpick'@'%' IDENTIFIED BY '${db_password}';" \
    "GRANT ALL PRIVILEGES ON dulpick_db.* TO 'dulpick'@'%';" \
    | docker exec --interactive "${MYSQL_CONTAINER}" \
        sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"'

umask 077
{
    echo "DB_URL=jdbc:mysql://mysql:3306/dulpick_db?serverTimezone=Asia/Seoul&characterEncoding=UTF-8"
    echo "DB_USERNAME=dulpick"
    echo "DB_PASSWORD=${db_password}"
    echo "JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75.0"
} >"${ENV_FILE}"

chmod 600 "${ENV_FILE}"

echo "Server setup completed: ${APP_DIR}"
