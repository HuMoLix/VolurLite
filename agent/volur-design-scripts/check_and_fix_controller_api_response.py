import sys
import re
from pathlib import Path

SRC_DIR = Path(r"H:\HuMoLix\github\Volur-Lite\src")
DEP_PAT = re.compile(r"ApiResponseEntity\.(OK|SUCCESS|CREATED|BAD_REQUEST|UNAUTHORIZED|FORBIDDEN|NOT_FOUND|ERROR|INTERNAL_SERVER_ERROR)\b")
OK_NULL_PAT = re.compile(r"ApiResponseEntity\.ok\(\s*(\"[^\"]*\")\s*,\s*null\s*\)")

def main():
    fix = "--fix" in sys.argv or "-f" in sys.argv
    found = 0
    for fpath in SRC_DIR.rglob("*Controller.java"):
        txt = fpath.read_text(encoding="utf-8")
        issues = []
        for i, line in enumerate(txt.splitlines(), 1):
            if DEP_PAT.search(line):
                issues.append((i, "DEPRECATED_CALL", line.strip()))
            if OK_NULL_PAT.search(line):
                issues.append((i, "OK_NULL_ANTIPATTERN", line.strip()))
        if issues:
            found += len(issues)
            print(f"[{fpath.relative_to(SRC_DIR)}]")
            for line_no, itype, raw in issues:
                print(f"  L{line_no}: {itype} -> {raw}")
            if fix:
                new_txt = OK_NULL_PAT.sub(r"ApiResponseEntity.success(\1)", txt)
                new_txt = re.sub(r"ApiResponseEntity\.OK\(\s*(\"[^\"]*\")\s*,\s*null\s*\)", r"ApiResponseEntity.success(\1)", new_txt)
                new_txt = re.sub(r"ApiResponseEntity\.OK\(", "ApiResponseEntity.ok(", new_txt)
                new_txt = re.sub(r"ApiResponseEntity\.SUCCESS\(", "ApiResponseEntity.success(", new_txt)
                new_txt = re.sub(r"ApiResponseEntity\.CREATED\(", "ApiResponseEntity.created(", new_txt)
                new_txt = re.sub(r"ApiResponseEntity\.BAD_REQUEST\(", "ApiResponseEntity.badRequest(", new_txt)
                new_txt = re.sub(r"ApiResponseEntity\.UNAUTHORIZED\(", "ApiResponseEntity.unauthorized(", new_txt)
                new_txt = re.sub(r"ApiResponseEntity\.FORBIDDEN\(", "ApiResponseEntity.forbidden(", new_txt)
                new_txt = re.sub(r"ApiResponseEntity\.NOT_FOUND\(", "ApiResponseEntity.notFound(", new_txt)
                new_txt = re.sub(r"ApiResponseEntity\.ERROR\(", "ApiResponseEntity.error(", new_txt)
                new_txt = re.sub(r"ApiResponseEntity\.INTERNAL_SERVER_ERROR\(", "ApiResponseEntity.internalServerError(", new_txt)
                fpath.write_text(new_txt, encoding="utf-8")
                print("  [FIXED]")
    if found == 0:
        print("ALL_CONTROLLERS_COMPLIANT")
    else:
        sys.exit(1)

if __name__ == "__main__":
    main()
