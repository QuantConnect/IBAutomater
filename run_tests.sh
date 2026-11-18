#!/usr/bin/env bash
# Builds the IBAutomater Java agent and runs the C# and Java tests, no IB Gateway or credentials needed
#
# Requires a JDK, Ant, the .NET 10 SDK and, without a display, xvfb-run: the Java tests create Swing windows
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAVA_PROJECT="$ROOT/java/IBAutomater"
JUNIT_VERSION="1.11.4"
JUNIT_SHA256="b016ef6b1c3454d6d7c2c88ce081dabf289699686af6622d6e4e2e1b54b4a2fc"
JUNIT_JAR="$JAVA_PROJECT/build/test-lib/junit-platform-console-standalone-$JUNIT_VERSION.jar"
TEST_CLASSES="$JAVA_PROJECT/build/test/classes"

echo "Building Java agent"
"${ANT:-ant}" -f "$JAVA_PROJECT/build.xml" clean jar

echo "Running C# tests"
dotnet test "$ROOT/QuantConnect.IBAutomater.Tests/QuantConnect.IBAutomater.Tests.csproj" --configuration Release

echo "Running Java tests"
mkdir -p "$(dirname "$JUNIT_JAR")" "$TEST_CLASSES"
curl -sfL -o "$JUNIT_JAR" "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/$JUNIT_VERSION/junit-platform-console-standalone-$JUNIT_VERSION.jar"
echo "$JUNIT_SHA256  $JUNIT_JAR" | sha256sum --check --quiet
# the tests live outside the Ant project's test folder (nbproject: test.src.dir=test), so its default build doesn't need JUnit
javac -d "$TEST_CLASSES" -cp "$JAVA_PROJECT/dist/IBAutomater.jar:$JUNIT_JAR" $(find "$JAVA_PROJECT/tests" -name "*.java")

# the automater writes IBAutomater.log to the working directory
cd "$JAVA_PROJECT/build/test"
RUN=(java -jar "$JUNIT_JAR" execute --class-path "$TEST_CLASSES:$JAVA_PROJECT/dist/IBAutomater.jar" --scan-class-path "$TEST_CLASSES" --fail-if-no-tests)
if [[ -z "${DISPLAY:-}" ]]; then
    xvfb-run --auto-servernum "${RUN[@]}"
else
    "${RUN[@]}"
fi
