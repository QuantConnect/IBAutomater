#!/usr/bin/env bash
# Builds the IBAutomater Java agent, packs the QuantConnect.IBAutomater NuGet package into ./artifacts
# with the <Version> in the csproj and pushes it to nuget.org when NUGET_API_KEY is set
#
# Requires a JDK, Ant and the .NET 10 SDK. Set JAVA_HOME and ANT to override the ones found on the PATH.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAVA_PROJECT="$ROOT/java/IBAutomater"
JAR="$JAVA_PROJECT/dist/IBAutomater.jar"
PROJECT="$ROOT/QuantConnect.IBAutomater/QuantConnect.IBAutomater.csproj"
OUTPUT="$ROOT/artifacts"

# Fall back to the JDK and Ant bundled with NetBeans when none are on the PATH (local Windows builds)
NETBEANS="/c/Program Files/Apache NetBeans"
if [[ -z "${JAVA_HOME:-}" ]] && ! command -v javac > /dev/null && [[ -d "$NETBEANS/jdk" ]]; then
    export JAVA_HOME="$NETBEANS/jdk"
fi
ANT="${ANT:-$(command -v ant || echo "$NETBEANS/extide/ant/bin/ant")}"

echo "Building Java agent"
"$ANT" -f "$JAVA_PROJECT/build.xml" clean jar
if [[ ! -f "$JAR" ]]; then
    echo "Java build did not produce $JAR" >&2
    exit 1
fi

echo "Packing NuGet package"
rm -rf "$OUTPUT"
# The csproj sets GeneratePackageOnBuild, which stops pack from building first and fails on a clean checkout
dotnet pack "$PROJECT" --configuration Release --output "$OUTPUT" -p:GeneratePackageOnBuild=false

ls "$OUTPUT"/*.nupkg

if [[ -n "${NUGET_API_KEY:-}" ]]; then
    echo "Pushing to nuget.org"
    dotnet nuget push "$OUTPUT"/*.nupkg --api-key "$NUGET_API_KEY" --source https://api.nuget.org/v3/index.json
fi
