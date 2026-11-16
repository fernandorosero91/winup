#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

git pull --ff-only

docker compose --profile proxy up -d --build

sleep 15

docker compose ps
curl -fsS https://winup.site/api/v1/health
