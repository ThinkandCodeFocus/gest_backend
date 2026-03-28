#!/bin/sh

if command -v mvn >/dev/null 2>&1; then
  exec mvn "$@"
fi

echo "Maven is not installed on this machine. Install Maven or add the full Maven Wrapper files." >&2
exit 1
