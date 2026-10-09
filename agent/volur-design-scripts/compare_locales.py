import os
import json

base_locales = r"H:\HuMoLix\github\Volur-Lite\src\volur-lite-view-NEXT\src\locales"
locales = ["zh-CN", "en-US", "zh-TW"]

data = {}
for loc in locales:
    p = os.path.join(base_locales, loc, "auth.json")
    with open(p, "r", encoding="utf-8") as fp:
        data[loc] = json.load(fp)

all_keys = set()
for loc in locales:
    all_keys |= set(data[loc].keys())

print(f"Total keys across all 3 locales: {len(all_keys)}")
for loc in locales:
    missing = all_keys - set(data[loc].keys())
    print(f"Missing in {loc}: {len(missing)}")
    if missing:
        print(sorted(list(missing)))
