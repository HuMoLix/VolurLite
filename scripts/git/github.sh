#!/usr/bin/env bash
set -e
REMOTE="github"
BRANCH="main"
TMP_BRANCH="github-export"

if ! git diff-index --quiet HEAD --; then
  echo "[-] You have uncommitted changes. Please commit or stash them before running this script."
  exit 1
fi

LOCAL_AUTHOR=$(git log -1 --pretty='%an <%ae>')
LOCAL_DATE=$(git log -1 --pretty=%ad)
LOCAL_MSG=$(git log -1 --pretty=%B)

CURT_BRANCH=$(git symbolic-ref --short HEAD)
git branch -D "$TMP_BRANCH" 2>/dev/null || true
git checkout --orphan "$TMP_BRANCH"

echo "[+] Removing files from the temporary branch..."
git rm -rf --cached --ignore-unmatch \
    .env \
    GEMINI.md \
    package-amd64.sh \
    scripts \
    agent \
    .agents

cat << 'EOF' >> .gitignore
# Ignore all files
.env
GEMINI.md
package-amd64.sh
scripts/
agent/
.agents
EOF

git add .
git commit \
    --author="$LOCAL_AUTHOR" \
    --date="$LOCAL_DATE" \
    -m "$LOCAL_MSG"

echo "[+] Pushing to GitHub..."
git push -f "$REMOTE" "$TMP_BRANCH:$BRANCH"

echo "[+] Cleaning up..."
git checkout "$CURT_BRANCH"
git branch -D "$TMP_BRANCH"

git checkout -- .

echo "[+] Done! The latest commit has been pushed to GitHub on the '$BRANCH' branch."
