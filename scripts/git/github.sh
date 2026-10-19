#!/usr/bin/env bash
set -e
REMOTE="github"
BRANCH="main"
TMP_BRANCH="github-export"

git update-index -q --refresh
if ! git diff-index --quiet HEAD --; then
  echo "[-] You have uncommitted changes. Please commit or stash them before running this script."
  exit 1
fi

LOCAL_AUTHOR=$(git log -1 --pretty='%an <%ae>')
LOCAL_DATE=$(git log -1 --pretty=%ad)
LOCAL_MSG=$(git log -1 --pretty=%B)
COMMIT_MSG="${1:-$LOCAL_MSG}"

CURT_BRANCH=$(git symbolic-ref --short HEAD)

cleanup() {
  echo "[+] Cleaning up..."
  git checkout -f "$CURT_BRANCH" 2>/dev/null || true
  git branch -D "$TMP_BRANCH" 2>/dev/null || true
  git checkout -- . 2>/dev/null || true
}
trap cleanup EXIT

git branch -D "$TMP_BRANCH" 2>/dev/null || true
git checkout --orphan "$TMP_BRANCH"

echo "[+] Removing files from the temporary branch..."
git rm -rf --cached --ignore-unmatch \
    .env \
    GEMINI.md \
    package.sh \
    docker-compose.override.yml \
    src/volur-lite-view-NEXT/nginx/conf/nginx.conf \
    src/volur-lite-view-NEXT/nginx/ssl \
    scripts \
    agent \
    .agents

cat << 'EOF' >> .gitignore

# ================================
# GitHub Public Release Ignores
# ================================
.env
GEMINI.md
package.sh
docker-compose.override.yml
src/volur-lite-view-NEXT/nginx/conf/nginx.conf
src/volur-lite-view-NEXT/nginx/ssl/
scripts/
agent/
.agents/
EOF

git add .
git commit \
    --author="$LOCAL_AUTHOR" \
    --date="$LOCAL_DATE" \
    -m "$COMMIT_MSG"

echo "[+] Pushing to GitHub ($REMOTE $BRANCH)..."
if nc -z 127.0.0.1 7890 2>/dev/null; then
    git -c http.proxy=socks5h://127.0.0.1:7890 -c http.postBuffer=524288000 push -f "$REMOTE" "$TMP_BRANCH:$BRANCH"
else
    git -c http.version=HTTP/1.1 -c http.postBuffer=524288000 push -f "$REMOTE" "$TMP_BRANCH:$BRANCH"
fi

echo "[+] Done! The latest commit has been pushed to GitHub on the '$BRANCH' branch."
