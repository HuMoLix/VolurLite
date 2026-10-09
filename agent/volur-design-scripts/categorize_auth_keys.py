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
                rel_path = os.path.relpath(full_path, root).replace("\\", "/")
                key_files[key].add(rel_path)

locales_dir = os.path.join(root, "locales", "zh-CN")
with open(os.path.join(locales_dir, "auth.json"), "r", encoding="utf-8") as fp:
    auth_json = json.load(fp)

all_keys = set(auth_json.keys()) | set(key_files.keys())

mapping = {}
for k in all_keys:
    files = key_files.get(k, set())
    is_recaptcha = k.startswith("recaptcha_") or any("recaptcha" in f for f in files)
    is_recovery = k.startswith("recovery_") or any("recovery" in f for f in files)
    is_signup = k.startswith("signup_") or any("signup" in f for f in files)
    is_signin = (k.startswith("signin_") or "signin" in k or any("signin" in f for f in files)) and not is_signup and not is_recovery
    
    if is_recaptcha:
        cat = "captcha/recaptcha"
    elif is_recovery:
        cat = "auth/recovery"
    elif is_signup:
        cat = "auth/signup"
    elif is_signin:
        cat = "auth/signin"
    else:
        cat = "auth/auth"
    mapping[k] = (cat, sorted(list(files)))

for cat in ["captcha/recaptcha", "auth/recovery", "auth/signup", "auth/signin", "auth/auth"]:
    keys_in_cat = [k for k, v in mapping.items() if v[0] == cat]
    print(f"=== {cat} ({len(keys_in_cat)}) ===")
    print(sorted(keys_in_cat))
