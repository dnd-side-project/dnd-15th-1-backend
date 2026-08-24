#!/usr/bin/env bash

set -Eeuo pipefail

readonly APP_DIR="${APP_DIR:-/home/ubuntu/dulpick}"
readonly ENV_FILE="${ENV_FILE:-${APP_DIR}/.env}"
readonly SECRETS_DIR="${APP_DIR}/secrets"
readonly APPLE_PRIVATE_KEY_FILE="${SECRETS_DIR}/Dulpick_SIWA_AuthKey_6F3A6ZCY7J.p8"
readonly FIREBASE_CREDENTIALS_FILE="${SECRETS_DIR}/firebase-service-account.json"
readonly CONTENT_IMAGE_DIR="${APP_DIR}/content-images"
readonly CONTAINER_NAME="${CONTAINER_NAME:-dulpick-content-image-backfill}"
readonly DOCKER_NETWORK="${DOCKER_NETWORK:-short-net}"
readonly FCM_ENABLED_VALUE="$(grep -E '^FCM_ENABLED=' "${ENV_FILE}" | tail -n 1 | cut -d '=' -f 2- || true)"

if [[ $# -eq 0 ]]; then
    readonly FULL_IMAGE="$(docker container inspect --format '{{.Config.Image}}' dulpick-backend)"
elif [[ $# -eq 2 ]]; then
    readonly FULL_IMAGE="${1}:${2}"
else
    echo "Usage: backfill-content-images.sh [image-name image-tag]" >&2
    exit 1
fi

if [[ ! -f "${ENV_FILE}" || ! -f "${APPLE_PRIVATE_KEY_FILE}" ]]; then
    echo "Production environment or Apple secret is missing." >&2
    exit 1
fi

mkdir -p "${CONTENT_IMAGE_DIR}"

secret_volumes=(
    --volume
    "${APPLE_PRIVATE_KEY_FILE}:/run/secrets/Dulpick_SIWA_AuthKey_6F3A6ZCY7J.p8:ro"
    --volume
    "${CONTENT_IMAGE_DIR}:/var/lib/dulpick/content-images"
)

if [[ "${FCM_ENABLED_VALUE}" == "true" ]]; then
    if [[ ! -f "${FIREBASE_CREDENTIALS_FILE}" ]]; then
        echo "Firebase credentials are required when FCM_ENABLED=true." >&2
        exit 1
    fi
    secret_volumes+=(
        --volume
        "${FIREBASE_CREDENTIALS_FILE}:/run/secrets/firebase-service-account.json:ro"
    )
fi

docker run --rm \
    --name "${CONTAINER_NAME}" \
    --network "${DOCKER_NETWORK}" \
    --env-file "${ENV_FILE}" \
    --env SPRING_PROFILES_ACTIVE=prod \
    --env SPRING_MAIN_WEB_APPLICATION_TYPE=none \
    --env CONTENT_IMAGE_BACKFILL_ENABLED=true \
    --env CONTENT_THUMBNAIL_STORAGE_PATH=/var/lib/dulpick/content-images \
    "${secret_volumes[@]}" \
    "${FULL_IMAGE}"

echo "Content image backfill finished: ${FULL_IMAGE}"
