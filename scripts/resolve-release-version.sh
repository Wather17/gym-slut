#!/usr/bin/env bash
set -Eeuo pipefail

if [[ $# -ne 1 || -z "${1:-}" ]]; then
  printf 'Uso: %s <tag-semver> (ex.: v1.0.0 ou v1.0.0-rc.1)\n' "$0" >&2
  exit 1
fi

TAG="$1"
REGEX='^v([0-9]+)\.([0-9]+)\.([0-9]+)(-([0-9A-Za-z.-]+))?$'

if [[ ! "$TAG" =~ $REGEX ]]; then
  printf 'Erro: tag "%s" inválida. Use o formato SemVer vMAJOR.MINOR.PATCH[-prerelease] (ex.: v1.0.0).\n' "$TAG" >&2
  exit 1
fi

MAJOR_RAW="${BASH_REMATCH[1]}"
MINOR_RAW="${BASH_REMATCH[2]}"
PATCH_RAW="${BASH_REMATCH[3]}"
PRERELEASE="${BASH_REMATCH[5]:-}"

MAJOR=$((10#$MAJOR_RAW))
MINOR=$((10#$MINOR_RAW))
PATCH=$((10#$PATCH_RAW))

if (( MAJOR > 2099 || MINOR > 99 || PATCH > 99 )); then
  printf 'Erro: componentes da versão fora do limite suportado (major<=2099, minor<=99, patch<=99): %s\n' "$TAG" >&2
  exit 1
fi

BASE_CODE=$(( MAJOR * 1000000 + MINOR * 10000 + PATCH * 100 ))

if [[ -z "$PRERELEASE" ]]; then
  OFFSET=99
  IS_PRERELEASE="false"
  VERSION_NAME="${MAJOR}.${MINOR}.${PATCH}"
else
  IS_PRERELEASE="true"
  VERSION_NAME="${MAJOR}.${MINOR}.${PATCH}-${PRERELEASE}"

  PRE_NUM=0
  if [[ "$PRERELEASE" =~ ([0-9]+)$ ]]; then
    PRE_NUM=$((10#${BASH_REMATCH[1]}))
    if (( PRE_NUM > 28 )); then
      PRE_NUM=28
    fi
  fi

  case "$PRERELEASE" in
    rc*)
      OFFSET=$((70 + PRE_NUM))
      ;;
    beta*)
      OFFSET=$((40 + PRE_NUM))
      ;;
    alpha*)
      OFFSET=$((10 + PRE_NUM))
      ;;
    *)
      OFFSET=1
      ;;
  esac
fi

VERSION_CODE=$((BASE_CODE + OFFSET))

printf 'VERSION_NAME=%s\n' "$VERSION_NAME"
printf 'VERSION_CODE=%s\n' "$VERSION_CODE"
printf 'IS_PRERELEASE=%s\n' "$IS_PRERELEASE"
