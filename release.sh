#!/usr/bin/env bash
# Releases master: bumps the version when master has commits the last published package lacks,
# publishes the NuGet package and opens the PR that updates it in the Interactive Brokers brokerage.
#
# Usage: release.sh <brokerage checkout>
# Publishes with NUGET_API_KEY and opens the PR with GH_TOKEN. Safe to rerun, finished steps are skipped.
# DRY_RUN=1 skips the NuGet wait and the PR creation but still pushes commits and branches:
# only use it with every remote pointed at a local mirror.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BROKERAGE="$(cd "$1" && pwd)"
BROKERAGE_REPO="QuantConnect/Lean.Brokerages.InteractiveBrokers"
CSPROJ="QuantConnect.IBAutomater/QuantConnect.IBAutomater.csproj"
FLAT_CONTAINER="https://api.nuget.org/v3-flatcontainer/quantconnect.ibautomater"
REFERENCE='Include="QuantConnect.IBAutomater"'
INDEXING_TIMEOUT_SECONDS=3600
INDEXING_POLL_SECONDS=30

step() {
    echo
    echo "=== $1"
}

fetch() {
    curl -fsSL --retry 5 --retry-all-errors "$@"
}

# Stops the release when the lookup fails: it must not read as "not published"
is_indexed() {
    local index
    index=$(fetch "$FLAT_CONTAINER/index.json") || exit 1
    grep -qF "\"$1\"" <<< "$index"
}

# The package records the commit it was built from.
# Saved to a file: a retry appends to captured output but starts a file over
shipped_commit() {
    local nuspec
    nuspec="$(mktemp)"
    fetch -o "$nuspec" "$FLAT_CONTAINER/$1/quantconnect.ibautomater.nuspec" || exit 1
    sed -n 's/.*<repository [^>]*commit="\([0-9a-f]*\)".*/\1/p' "$nuspec"
}

step "Bump version"
version=$(sed -n 's:.*<Version>\(.*\)</Version>.*:\1:p' "$ROOT/$CSPROJ")
head=$(git -C "$ROOT" rev-parse HEAD)
if ! is_indexed "$version"; then
    echo "$version is not on NuGet yet, releasing it as is"
else
    shipped=$(shipped_commit "$version")
    if [[ -z "$shipped" ]]; then
        echo "Can't tell which commit $version was built from" >&2
        exit 1
    fi

    if [[ "$shipped" == "$head" ]]; then
        echo "$version already ships master ($head), nothing to bump"
    else
        current="$version"
        IFS=. read -r major minor patch <<< "$current"
        version="$major.$minor.$((patch + 1))"
        echo "master has $(git -C "$ROOT" rev-list --count "$shipped..$head") commits since $current, bumping to $version"

        sed -i "s:<Version>${current//./\\.}</Version>:<Version>$version</Version>:" "$ROOT/$CSPROJ"
        git -C "$ROOT" commit -q -m "Update version to $version" -- "$CSPROJ"
        git -C "$ROOT" push origin HEAD:master
    fi
fi

step "Publish NuGet package"
if is_indexed "$version"; then
    echo "$version is already on NuGet"
else
    bash "$ROOT/build_push_nuget.sh"
fi

# Indexing lags behind the push, and the brokerage can't restore the package until it is done
step "Wait for NuGet indexing"
if [[ -n "${DRY_RUN:-}" ]]; then
    echo "Dry run, not waiting for $version"
else
    deadline=$((SECONDS + INDEXING_TIMEOUT_SECONDS))
    until is_indexed "$version"; do
        if ((SECONDS >= deadline)); then
            echo "$version is still not indexed after $INDEXING_TIMEOUT_SECONDS seconds" >&2
            exit 1
        fi
        echo "$version is not indexed yet, checking again in $INDEXING_POLL_SECONDS seconds"
        sleep "$INDEXING_POLL_SECONDS"
    done
    echo "$version is indexed"

    # A version pushed by an earlier run is never replaced, so it can be older than master
    shipped=$(shipped_commit "$version")
    head=$(git -C "$ROOT" rev-parse HEAD)
    if [[ "$shipped" != "$head" ]]; then
        echo "$version was built from ${shipped:-an unknown commit}, not from master ($head)." >&2
        echo "Run the release again to ship master as a new version." >&2
        exit 1
    fi
fi

# Lists the PRs merged after the given version was set
changes_since() {
    local from
    from=$(git -C "$ROOT" log --format=%H -S"<Version>$1</Version>" -- "$CSPROJ" | tail -1)
    git -C "$ROOT" log --reverse --format=%s "$from..HEAD" |
        sed -n 's/^\(.*\) (#\([0-9]*\))$/- QuantConnect\/IBAutomater#\2: \1/p'
}

pr_body() {
    cat <<EOF
#### Description

Updates the \`QuantConnect.IBAutomater\` package reference from $1 to $version.

IBAutomater changes since $1:
$(changes_since "$1")

#### Related Issue

N/A

#### Motivation and Context

Keeps the brokerage on the latest QuantConnect.IBAutomater.

#### Requires Documentation Change

No

#### How Has This Been Tested?

Version bump only. Each change is covered in its IBAutomater PR.

#### Types of changes

- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] Refactor (non-breaking change which improves implementation)
- [ ] Performance (non-breaking change which improves performance. Please add associated performance test and results)
- [ ] New feature (non-breaking change which adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to change)
- [ ] Non-functional change (xml comments/documentation/etc)

#### Checklist:

- [x] My code follows the code style of this project.
- [x] I have read the **CONTRIBUTING** [document](https://github.com/QuantConnect/Lean/blob/master/CONTRIBUTING.md).
- [ ] I have added tests to cover my changes.
- [x] All new and existing tests passed.
- [x] My branch follows the naming convention \`bug-<issue#>-<description>\` or \`feature-<issue#>-<description>\`
EOF
}

step "Open brokerage PR"
branch="feature-update-ibautomater-$version"
title="Update QuantConnect.IBAutomater to $version"
referenced=$(git -C "$BROKERAGE" grep -h "$REFERENCE" -- '*.csproj' | sed -n 's/.*Version="\([^"]*\)".*/\1/p' | sort -uV | tail -1)

if [[ "$referenced" == "$version" ]]; then
    pr="already on $version"
else
    pr=$(gh pr list --repo "$BROKERAGE_REPO" --head "$branch" --state open --json url --jq '.[0].url // empty')
fi

if [[ -z "$pr" ]]; then
    body="$(mktemp)"
    pr_body "$referenced" > "$body"
    git -C "$BROKERAGE" checkout -q -b "$branch"
    git -C "$BROKERAGE" grep -l "$REFERENCE" -- '*.csproj' |
        (cd "$BROKERAGE" && xargs sed -i "/$REFERENCE/ s/Version=\"[^\"]*\"/Version=\"$version\"/")
    git -C "$BROKERAGE" commit -q -a -m "$title"
    # A branch left by a run that died before opening the PR is replaced
    git -C "$BROKERAGE" push --force origin "$branch"

    if [[ -n "${DRY_RUN:-}" ]]; then
        git -C "$BROKERAGE" show --stat --format='%n%s'
        cat "$body"
        pr="dry run"
    else
        pr=$(gh pr create --repo "$BROKERAGE_REPO" --base master --head "$branch" --title "$title" --body-file "$body")
    fi
fi

echo "$BROKERAGE_REPO: $pr" | tee -a "${GITHUB_STEP_SUMMARY:-/dev/null}"
