import os
import re
import json

root = r"H:\HuMoLix\github\Volur-Lite\src\volur-lite-view-NEXT\src"
locales_dir = os.path.join(root, "locales")

def get_nested_keys(obj, prefix=""):
    keys = set()
    for k, v in obj.items():
        full = f"{prefix}.{k}" if prefix else k
        if isinstance(v, dict):
            keys.update(get_nested_keys(v, full))
        else:
            keys.add(full)
    return keys

for loc in ["zh-CN", "en-US", "zh-TW"]:
    loc_path = os.path.join(locales_dir, loc)
    with open(os.path.join(loc_path, "common.json"), "r", encoding="utf-8") as fp:
        common = json.load(fp)
    with open(os.path.join(loc_path, "auth", "auth.json"), "r", encoding="utf-8") as fp:
        auth_common = json.load(fp)
    with open(os.path.join(loc_path, "auth", "signin.json"), "r", encoding="utf-8") as fp:
        auth_signin = json.load(fp)
    with open(os.path.join(loc_path, "auth", "signup.json"), "r", encoding="utf-8") as fp:
        auth_signup = json.load(fp)
    with open(os.path.join(loc_path, "auth", "recovery.json"), "r", encoding="utf-8") as fp:
        auth_recovery = json.load(fp)
    with open(os.path.join(loc_path, "captcha", "recaptcha.json"), "r", encoding="utf-8") as fp:
        captcha_recaptcha = json.load(fp)

    auth_full = {
        **auth_common,
        "signin": auth_signin,
        "signup": auth_signup,
        "recovery": auth_recovery
    }
    all_dict = {
        "common": common,
        "auth": auth_full,
        "captcha": {
            "recaptcha": captcha_recaptcha
        }
    }
    tree_keys = get_nested_keys(all_dict)
    print(f"[{loc}] Total available keys in message tree: {len(tree_keys)}")

# Extract all t('...') or key references in code
key_pattern = re.compile(r"['\"]((?:auth|captcha|common)\.[a-zA-Z0-9_\.]+)['\"]")
code_keys = set()
for dirpath, _, filenames in os.walk(root):
    for f in filenames:
        if (f.endswith(".vue") or f.endswith(".ts")) and not f.endswith(".d.ts"):
            full_path = os.path.join(dirpath, f)
            with open(full_path, "r", encoding="utf-8") as fp:
                content = fp.read()
            for m in key_pattern.finditer(content):
                code_keys.add(m.group(1))

print(f"Total i18n keys referenced in code: {len(code_keys)}")

missing = code_keys - tree_keys
if missing:
    print(f"WARNING: Missing keys in zh-CN: {sorted(list(missing))}")
else:
    print("SUCCESS: 100% of keys referenced in code are present in the locale trees!")
