#!/usr/bin/env bash
set -euo pipefail

# Usage: DOCKER_HOST=unix:///path/to/podman.sock ANDROID_HOME=/sdk
#        GRADLE_USER_HOME=/cache ACT_BIN=act check-local-release.sh /private/secrets.env [run-number]
# act checkout optimization copies unpublished sources; hosted checkout uses event SHA.
fail() { printf '%s\n' "$*" >&2; exit 1; }
[[ $# -ge 1 && $# -le 2 ]] || fail 'Usage: check-local-release.sh /private/secrets.env [run-number, default 4]'
secret_file=$(realpath -- "$1")
repo_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd -P)
[[ $secret_file != "$repo_root/"* ]] || fail 'Secret file must be outside the repository.'
run_number=${2:-4}
[[ $run_number =~ ^[1-9][0-9]*$ && ${#run_number} -le 9 ]] || fail 'Run number must be a positive integer of at most 9 digits.'
[[ -f $secret_file && -r $secret_file ]] || fail 'Secret file must exist and be readable.'
secret_mode=$(stat -c '%a' -- "$secret_file")
(( (8#$secret_mode & 077) == 0 )) || fail 'Secret file must have no group or other permissions (use chmod 600).'
[[ ${DOCKER_HOST:-} == unix://* && -S ${DOCKER_HOST#unix://} ]] || fail 'DOCKER_HOST must point to a running Podman Unix socket.'
[[ -n ${ANDROID_HOME:-} && -n ${GRADLE_USER_HOME:-} ]] || fail 'Set ANDROID_HOME and GRADLE_USER_HOME to existing SDK and cache directories.'
sdk=$(realpath -- "$ANDROID_HOME")
gradle_cache=$(realpath -- "$GRADLE_USER_HOME")
[[ -x $sdk/cmdline-tools/latest/bin/sdkmanager && -d $gradle_cache ]] || fail 'SDK must include executable sdkmanager; Gradle cache must be a directory.'
# Docker option parsing requires these bind paths to contain no quotes or escapes.
[[ $sdk$gradle_cache != *[\"\\$'\n']* ]] || fail 'SDK/cache paths cannot contain quotes, backslashes or newlines.'
act_bin=${ACT_BIN:-act}
command -v "$act_bin" >/dev/null || fail 'act is missing; install it or set ACT_BIN.'
command -v podman >/dev/null || fail 'Podman is missing.'
image=localhost/gym-act-local:ubuntu24
if ! podman image exists "$image"; then
  podman build -t "$image" -f "$repo_root/.github/local/Containerfile" "$repo_root/.github/local"
fi
act_cache=$(mktemp -d "${TMPDIR:-/tmp}/gym-act-local-cache.XXXXXX")
trap 'rm -rf -- "$act_cache"' EXIT
printf -v container_options -- '-v "%s:/opt/android-sdk" -v "%s:/root/.gradle"' "$sdk" "$gradle_cache"
"$act_bin" push -C "$repo_root" -W .github/workflows/android-release.yml -j release \
  -P "ubuntu-24.04=$image" --pull=false --container-daemon-socket=- --no-cache-server \
  --action-cache-path "$act_cache/actions" --cache-server-path "$act_cache/cache" \
  --env-file /dev/null --var-file /dev/null --input-file /dev/null \
  --secret-file "$secret_file" --env GYM_RELEASE_DRY_RUN=1 --env "GITHUB_RUN_NUMBER=$run_number" \
  --container-options "$container_options"
