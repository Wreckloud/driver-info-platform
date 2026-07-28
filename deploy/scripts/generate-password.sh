#!/bin/sh
set -eu

printf 'New administrator password: '
stty -echo
read password
stty echo
printf '\n'

if [ "${#password}" -lt 12 ]; then
  echo 'Password must contain at least 12 characters.' >&2
  exit 1
fi

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
export ADMIN_PASSWORD_PLAIN=$password
cd "$project_dir/backend"
mvn -q compile org.codehaus.mojo:exec-maven-plugin:3.5.1:java \
  -Dexec.mainClass=com.wreckloud.driver.tool.PasswordHashTool
unset ADMIN_PASSWORD_PLAIN
unset password
