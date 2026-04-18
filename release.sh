#!/bin/bash
set -e

if [ -z "$1" ]
  then
    echo "No version provided. Usage: ./release.sh <version>"
    exit 1
fi

VERSION=$1
echo "Releasing version $VERSION"

mvn clean install

mvn versions:set -DgenerateBackupPoms=false -DnewVersion=$VERSION

BRANCH=$(git rev-parse --abbrev-ref HEAD)
git diff --quiet && git diff --cached --quiet || git commit -a -m "Release $VERSION"
git push origin "$BRANCH"

# Idempotent tag: delete existing tag locally and remotely before re-creating
if git rev-parse "$VERSION" >/dev/null 2>&1; then
  echo "Tag $VERSION already exists locally, deleting and re-creating"
  git tag -d "$VERSION"
fi
if git ls-remote --tags origin "$VERSION" | grep -q "$VERSION"; then
  echo "Tag $VERSION already exists on remote, deleting"
  git push origin --delete "refs/tags/$VERSION" || true
fi
git tag -a "$VERSION" -m "Release $VERSION"
git push origin "refs/tags/$VERSION"

# Deploy and release to Sonatype
mvn clean deploy -P release
mvn nexus-staging:release -P release -DserverId=sonatype -DnexusUrl=https://s01.oss.sonatype.org/

echo "Release $VERSION completed successfully"
