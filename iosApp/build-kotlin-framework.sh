#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

if [ "YES" = "$OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED" ]; then
  echo "Skipping Gradle build task invocation due to OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED environment variable set to \"YES\""
  exit 0
fi

cd "$SRCROOT/.."

# Run the Gradle task - the "set -e" above will make the script exit with the same code
# if the Gradle command fails
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode

# Exit with success - if we got here, the Gradle build was successful
exit 0
