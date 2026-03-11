#!/bin/sh
#
# Installs Claude Faces Expert into the current project.
#
# Usage:
#   curl -sL https://raw.githubusercontent.com/omnifaces/claude-faces-expert/main/install.sh | sh
#

set -e

REPO="https://github.com/omnifaces/claude-faces-expert.git"
TMP_DIR=$(mktemp -d)

git clone --depth 1 --quiet "$REPO" "$TMP_DIR"

mkdir -p .claude/faces .claude/skills
cp -r "$TMP_DIR/.claude/faces/"* .claude/faces/
cp -r "$TMP_DIR/.claude/skills/"* .claude/skills/

rm -rf "$TMP_DIR"

echo "Claude Faces Expert installed into .claude/"
echo ""
echo "Add this line to your CLAUDE.md:"
echo "  Jakarta Faces rules: @.claude/faces/rules.md"
