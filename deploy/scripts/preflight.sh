#!/bin/sh
set -eu

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
env_file="$project_dir/.env"
cert_file="$project_dir/deploy/certs/fullchain.pem"
key_file="$project_dir/deploy/certs/privkey.pem"

fail() {
  echo "Preflight failed: $1" >&2
  exit 1
}

command -v docker >/dev/null 2>&1 || fail "docker is not installed"
docker compose version >/dev/null 2>&1 || fail "docker compose is unavailable"
[ -f "$env_file" ] || fail ".env does not exist; copy .env.example and fill it first"
[ -s "$cert_file" ] || fail "deploy/certs/fullchain.pem is missing or empty"
[ -s "$key_file" ] || fail "deploy/certs/privkey.pem is missing or empty"

if grep -Eq '(^PUBLIC_DOMAIN=example\.com$|^PUBLIC_BASE_URL=https://example\.com$|replace_with_)' "$env_file"; then
  fail ".env still contains example or placeholder values"
fi
grep -Eq '^PUBLIC_BASE_URL=https://[^[:space:]]+$' "$env_file" \
  || fail "PUBLIC_BASE_URL must be a complete HTTPS address"

command -v openssl >/dev/null 2>&1 || fail "openssl is not installed"
openssl x509 -in "$cert_file" -checkend 86400 -noout >/dev/null 2>&1 \
  || fail "TLS certificate is invalid or expires within 24 hours"
openssl pkey -in "$key_file" -passin pass: -check -noout >/dev/null 2>&1 \
  || fail "TLS private key is invalid"
cert_public_key=$(openssl x509 -in "$cert_file" -pubkey -noout \
  | openssl pkey -pubin -outform DER 2>/dev/null | openssl dgst -sha256)
private_public_key=$(openssl pkey -in "$key_file" -passin pass: -pubout -outform DER 2>/dev/null \
  | openssl dgst -sha256)
[ "$cert_public_key" = "$private_public_key" ] || fail "TLS certificate and private key do not match"

cd "$project_dir"
docker compose --env-file "$env_file" config --quiet
echo "Preflight passed. Configuration, required values, and TLS files are ready."
