#!/bin/sh
#
# Installs Claude Faces Expert into the current project.
#
# Usage:
#   curl -sL https://raw.githubusercontent.com/omnifaces/claude-faces-expert/main/install.sh | sh
#

set -e

REPO="https://github.com/omnifaces/claude-faces-expert.git"
REFERENCE="Jakarta Faces rules: @.claude/faces/rules.md"
TMP_DIR=$(mktemp -d)

git clone --depth 1 --quiet "$REPO" "$TMP_DIR"

mkdir -p .claude/faces .claude/skills
cp -r "$TMP_DIR/.claude/faces/"* .claude/faces/
cp -r "$TMP_DIR/.claude/skills/"* .claude/skills/

rm -rf "$TMP_DIR"

if [ ! -f CLAUDE.md ]; then
    printf "# Project Rules\n\n%s\n" "$REFERENCE" > CLAUDE.md
    echo "Created CLAUDE.md with Faces rules reference."
elif ! grep -qF "@.claude/faces/rules.md" CLAUDE.md; then
    printf "\n## Jakarta Faces\n\n%s\n" "$REFERENCE" >> CLAUDE.md
    echo "Appended Faces rules reference to CLAUDE.md."
else
    echo "CLAUDE.md already references Faces rules."
fi

echo "Claude Faces Expert installed into .claude/"
