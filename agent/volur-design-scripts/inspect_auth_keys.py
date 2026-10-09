import os
import re
import json
from collections import defaultdict

root = r"H:\HuMoLix\github\Volur-Lite\src\volur-lite-view-NEXT\src"
pattern = re.compile(r"['\"]auth\.([a-zA-Z0-9_]+)['\"]")

key_files = defaultdict(set)

for dirpath, _, filenames in os.walk(root):
    for f in filenames:
        if f.endswith((".vue", ".ts")) and not f.endswith(".d.ts"):
            full_path = os.path.join(dirpath, f)
            with open(full_path, "r", encoding="utf-8") as fp:
                content = fp.read()
            for m in pattern.finditer(content):
                key = m.group(1)
                rel_path = os.path.relpath(full_path, root)
                key_files[key].add(rel_path)

locales_dir = os.path.join(root, "locales", "zh-CN")
with open(os.path.join(locales_dir, "auth.json"), "r", encoding="utf-8") as fp:
    auth_json = json.load(fp)

print(f"Total unique keys referenced in code: {len(key_files)}")
print(f"Total keys in auth.json: {len(auth_json)}")

unused = set(auth_json.keys()) - set(key_files.keys())
missing = set(key_files.keys()) - set(auth_json.keys())

print(f"Unused in code ({len(unused)}): {sorted(list(unused))}")
print(f"Missing in auth.json ({len(missing)}): {sorted(list(missing))}")
