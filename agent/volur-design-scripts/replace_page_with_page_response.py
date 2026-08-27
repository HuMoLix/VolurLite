import sys
import re
from pathlib import Path

SRC_DIR = Path(r"H:\HuMoLix\github\Volur-Lite\src")

def scan():
    pat = re.compile(r"ApiResponse<\s*Page<")
    results = []
    for f in SRC_DIR.rglob("*Controller.java"):
        txt = f.read_text(encoding="utf-8")
        for i, line in enumerate(txt.splitlines(), 1):
            if pat.search(line):
                results.append((f, i, line.strip()))
    return results

def main():
    res = scan()
    for f, line_no, line in res:
        print(f"{f.relative_to(SRC_DIR)}:{line_no}: {line}")
    if res:
        print(f"FOUND_{len(res)}_VIOLATIONS")
        sys.exit(1)
    else:
        print("ALL_CONTROLLERS_COMPLIANT")

if __name__ == "__main__":
    main()
