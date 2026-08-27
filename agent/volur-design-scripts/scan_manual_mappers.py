import re
from pathlib import Path

SRC_DIR = Path(r"H:\HuMoLix\github\Volur-Lite\src")

def scan_methods():
    # Find any private method returning VO/DTO/Entity or named toX/buildX/mapX
    method_pat = re.compile(r"private\s+([\w<>]+)\s+(\w+)\s*\([^)]*\)\s*\{")
    for f in SRC_DIR.rglob("*ServiceImpl.java"):
        txt = f.read_text(encoding="utf-8")
        for match in method_pat.finditer(txt):
            ret_type, method_name = match.groups()
            if "VO" in ret_type or "DTO" in ret_type or "Entity" in ret_type or method_name.startswith(("to", "build", "convert", "map")):
                line_no = txt[:match.start()].count("\n") + 1
                print(f"{f.relative_to(SRC_DIR)}:{line_no}: private {ret_type} {method_name}(...)")

if __name__ == "__main__":
    scan_methods()
