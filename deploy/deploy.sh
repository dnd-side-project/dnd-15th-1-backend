#!/usr/bin/env bash

set -Eeuo pipefail

readonly USAGE="Usage: deploy.sh <image-name> <image-tag> <public-web-base-url>"
readonly IMAGE_NAME="${1:?${USAGE}}"
readonly IMAGE_TAG="${2:?${USAGE}}"
readonly PUBLIC_WEB_BASE_URL="${3:?${USAGE}}"
readonly FULL_IMAGE="${IMAGE_NAME}:${IMAGE_TAG}"
readonly APP_DIR="${APP_DIR:-/home/ubuntu/dulpick}"
readonly ENV_FILE="${ENV_FILE:-${APP_DIR}/.env}"
readonly APPLE_PRIVATE_KEY_FILE="${APP_DIR}/secrets/Dulpick_SIWA_AuthKey_6F3A6ZCY7J.p8"
readonly FIREBASE_CREDENTIALS_FILE="${APP_DIR}/secrets/firebase-service-account.json"
readonly CONTAINER_NAME="${CONTAINER_NAME:-dulpick-backend}"
readonly DOCKER_NETWORK="${DOCKER_NETWORK:-short-net}"
readonly HOST_PORT="${HOST_PORT:-8083}"
readonly HEALTH_URL="http://127.0.0.1:${HOST_PORT}/health"
readonly PUBLIC_BASE_URL="${PUBLIC_WEB_BASE_URL%/}"

if [[ ! -f "${ENV_FILE}" ]]; then
    echo "Environment file not found: ${ENV_FILE}" >&2
    exit 1
fi

if [[ ! -f "${APPLE_PRIVATE_KEY_FILE}" ]]; then
    echo "Apple private key not found: ${APPLE_PRIVATE_KEY_FILE}" >&2
    exit 1
fi

mkdir -p "${APP_DIR}"
exec 9>"${APP_DIR}/deploy.lock"

if ! flock -n 9; then
    echo "Another deployment is already running." >&2
    exit 1
fi

previous_image=""
if docker container inspect "${CONTAINER_NAME}" >/dev/null 2>&1; then
    previous_image="$(docker container inspect \
        --format '{{.Config.Image}}' "${CONTAINER_NAME}")"
fi

if [[ "${DEPLOY_PULL:-true}" == "true" ]]; then
    docker pull "${FULL_IMAGE}"
fi

run_container() {
    local image="$1"
    local -a secret_volumes=(
        --volume
        "${APPLE_PRIVATE_KEY_FILE}:/run/secrets/Dulpick_SIWA_AuthKey_6F3A6ZCY7J.p8:ro"
    )

    if [[ "${FCM_ENABLED:-false}" == "true" ]]; then
        if [[ ! -f "${FIREBASE_CREDENTIALS_FILE}" ]]; then
            echo "Firebase credentials not found: ${FIREBASE_CREDENTIALS_FILE}" >&2
            return 1
        fi
        secret_volumes+=(
            --volume
            "${FIREBASE_CREDENTIALS_FILE}:/run/secrets/firebase-service-account.json:ro"
        )
    fi

    docker run --detach \
        --name "${CONTAINER_NAME}" \
        --restart unless-stopped \
        --network "${DOCKER_NETWORK}" \
        --env-file "${ENV_FILE}" \
        --env SPRING_PROFILES_ACTIVE=prod \
        "${secret_volumes[@]}" \
        --publish "127.0.0.1:${HOST_PORT}:8080" \
        "${image}"
}

wait_until_healthy() {
    local attempts=30

    for ((attempt = 1; attempt <= attempts; attempt++)); do
        if curl --fail --silent --show-error "${HEALTH_URL}" >/dev/null; then
            return 0
        fi
        sleep 2
    done

    return 1
}

verify_public_endpoint() {
    local path="$1"
    local expected_content_type="$2"
    local response
    local status
    local content_type
    local redirect_url

    response="$(curl --silent --show-error --output /dev/null \
        --write-out '%{http_code}|%{content_type}|%{redirect_url}' \
        "${PUBLIC_BASE_URL}${path}")" || return 1
    IFS='|' read -r status content_type redirect_url <<< "${response}"

    if [[ "${status}" != "200" \
        || "${content_type}" != "${expected_content_type}"* \
        || -n "${redirect_url}" ]]; then
        echo \
            "Public endpoint verification failed: path=${path}, status=${status}, content-type=${content_type}" \
            >&2
        return 1
    fi
}

verify_universal_link_routes() {
    verify_public_endpoint \
        "/.well-known/apple-app-site-association" \
        "application/json"
    verify_public_endpoint "/connect?code=ABCDE" "text/html"
}

docker container rm --force "${CONTAINER_NAME}" >/dev/null 2>&1 || true
run_container "${FULL_IMAGE}"

if wait_until_healthy && verify_universal_link_routes; then
    echo "Deployment succeeded: ${FULL_IMAGE}"
    exit 0
fi

echo "Deployment failed readiness check: ${FULL_IMAGE}" >&2
docker logs --tail 100 "${CONTAINER_NAME}" >&2 || true
docker container rm --force "${CONTAINER_NAME}" >/dev/null 2>&1 || true

if [[ -n "${previous_image}" && "${previous_image}" != "${FULL_IMAGE}" ]]; then
    echo "Rolling back to: ${previous_image}" >&2
    run_container "${previous_image}"

    if wait_until_healthy; then
        echo "Rollback succeeded: ${previous_image}" >&2
    else
        echo "Rollback failed health check: ${previous_image}" >&2
        docker logs --tail 100 "${CONTAINER_NAME}" >&2 || true
    fi
fi

exit 1
