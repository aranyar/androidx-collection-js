#!/bin/bash

set -e

if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <path-to-androidx> <new-revision>"
    exit 1
fi

ANDROIDX_PATH="$1"
NEW_REVISION="$2"

if [ ! -d "$ANDROIDX_PATH" ]; then
    echo "Error: Directory $ANDROIDX_PATH does not exist"
    exit 1
fi

if [ ! -d "$ANDROIDX_PATH/.git" ]; then
    echo "Error: $ANDROIDX_PATH is not a git repository"
    exit 1
fi

CURRENT_REVISION=$(cat androidx_revision.txt)

PATCH_FILE="${HOME}/collection.patch"

git -C "$ANDROIDX_PATH" diff "$CURRENT_REVISION" "$NEW_REVISION" -- collection/collection > "$PATCH_FILE"

echo "updating from:"
git -C "$ANDROIDX_PATH" show "$CURRENT_REVISION":libraryversions.toml 2>/dev/null | grep -E '^COLLECTION = "[^"]*"'
echo "to:"
git -C "$ANDROIDX_PATH" show "$NEW_REVISION":libraryversions.toml 2>/dev/null | grep -E '^COLLECTION = "[^"]*"'

echo "$NEW_REVISION" > androidx_revision.txt

echo "To update run:"
echo "git apply --3way --exclude=build.gradle -p 3 \$HOME/collection.patch"
