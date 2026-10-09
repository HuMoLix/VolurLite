import os
import json

base_path = r"H:\HuMoLix\github\Volur-Lite\src\volur-lite-view-NEXT\src\locales"

translations = {
    "zh-CN": "安全核验",
    "en-US": "Security verification",
    "zh-TW": "安全核驗"
}

for loc, text in translations.items():
    p = os.path.join(base_path, loc, "auth", "auth.json")
    with open(p, "r", encoding="utf-8") as fp:
        data = json.load(fp)
    data["security_verification"] = text
    with open(p, "w", encoding="utf-8") as fp:
        json.dump(data, fp, ensure_ascii=False, indent=2)
    print(f"Added security_verification to {loc}/auth/auth.json")
