#!/bin/bash

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

echo "$NEW_REVISION" > androidx_revision.txt

echo "To update run:"
echo "git apply -p 3 \$HOME/collection.patch"
