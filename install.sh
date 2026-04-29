#!/bin/sh
#
# Installs Claude Faces Expert.
#
# Usage:
#   # Project install (into ./.claude/, default):
#   curl -sL https://raw.githubusercontent.com/omnifaces/claude-faces-expert/main/install.sh | sh
#
#   # User install (into ~/.claude/, applies to all projects):
#   curl -sL https://raw.githubusercontent.com/omnifaces/claude-faces-expert/main/install.sh | sh -s -- --user
#

set -e

REPO="https://github.com/omnifaces/claude-faces-expert.git"
TMP_DIR=$(mktemp -d)

if [ "$1" = "--user" ]; then
    TARGET="$HOME/.claude"
    CLAUDE_MD="$HOME/.claude/CLAUDE.md"
    REFERENCE="Jakarta Faces rules: @~/.claude/faces/rules.md"
    SCOPE="user"
else
    TARGET=".claude"
    CLAUDE_MD="CLAUDE.md"
    REFERENCE="Jakarta Faces rules: @.claude/faces/rules.md"
    SCOPE="project"
fi

git clone --depth 1 --quiet "$REPO" "$TMP_DIR"

mkdir -p "$TARGET/faces" "$TARGET/skills"
cp -r "$TMP_DIR/.claude/faces/"* "$TARGET/faces/"
cp -r "$TMP_DIR/.claude/skills/"* "$TARGET/skills/"

rm -rf "$TMP_DIR"

if [ ! -f "$CLAUDE_MD" ]; then
    mkdir -p "$(dirname "$CLAUDE_MD")"
    printf "# Project Rules\n\n%s\n" "$REFERENCE" > "$CLAUDE_MD"
    echo "Created $CLAUDE_MD with Faces rules reference."
elif ! grep -qF "@~/.claude/faces/rules.md" "$CLAUDE_MD" \
   && ! grep -qF "@.claude/faces/rules.md" "$CLAUDE_MD"; then
    printf "\n## Jakarta Faces\n\n%s\n" "$REFERENCE" >> "$CLAUDE_MD"
    echo "Appended Faces rules reference to $CLAUDE_MD."
else
    echo "$CLAUDE_MD already references Faces rules."
fi

echo "Claude Faces Expert installed ($SCOPE scope) into $TARGET/"
