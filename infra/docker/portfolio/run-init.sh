#!/bin/sh
set -eu

# Configurable defaults (can be overridden via environment)
PGHOST="${PGHOST:-mintrade-portfolio}"
PGUSER="${PGUSER:-postgres}"
PGDATABASE="${PGDATABASE:-mintrade-portfolio}"
PGPASSWORD="${PGPASSWORD:-password}"
SCRIPTS_DIR="${SCRIPTS_DIR:-/scripts}"

export PGPASSWORD

echo "Waiting for Postgres at ${PGHOST}..."
until pg_isready -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" >/dev/null 2>&1; do
  sleep 1
done

echo "Postgres is up running scripts in ${SCRIPTS_DIR}"

# Iterate files in lexicographic order (shell globbing order)
for filepath in "${SCRIPTS_DIR}"/*; do
  # If directory is empty the glob remains literal on some shells; guard against that
  [ -e "${filepath}" ] || { echo "No scripts found in ${SCRIPTS_DIR}"; break; }
  [ -f "${filepath}" ] || continue

  filename="$(basename -- "${filepath}")"
  case "${filename##*.}" in
    sh)
      echo "-> running ${filename}"
      /bin/sh "${filepath}"
      ;;
    sql)
      echo "-> psql -f ${filename}"
      psql -v ON_ERROR_STOP=1 -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" -f "${filepath}"
      ;;
    gz)
      echo "-> gunzip -c ${filename} | psql"
      gunzip -c "${filepath}" | psql -v ON_ERROR_STOP=1 -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}"
      ;;
    *)
      echo "-> skipping ${filename} (unknown extension)"
      ;;
  esac
done

echo "Runner finished."
exit 0

