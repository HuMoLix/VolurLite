import os
import re

TARGET_DIR = r"H:\HuMoLix\github\Volur-Lite\src\volur-lite-platform"

SWAGGER_IMPORTS = [
    r"import\s+io\.swagger\.v3\.oas\.annotations\.media\.Schema\s*;\r?\n?",
    r"import\s+io\.swagger\.v3\.oas\.annotations\.Operation\s*;\r?\n?",
    r"import\s+io\.swagger\.v3\.oas\.annotations\.Parameter\s*;\r?\n?",
    r"import\s+io\.swagger\.v3\.oas\.annotations\.tags\.Tag\s*;\r?\n?",
    r"import\s+io\.swagger\.v3\.oas\.annotations\.[^;]+;\r?\n?",
    r"import\s+org\.springdoc\.[^;]+;\r?\n?",
]

ANNOTATION_NAMES = ["Tag", "Operation", "Parameter", "Schema"]

def remove_annotations(content):
    pos = 0
    while pos < len(content):
        match = re.search(r"@(" + "|".join(ANNOTATION_NAMES) + r")\b", content[pos:])
        if not match:
            break
        ann_start = pos + match.start()
        ann_name = match.group(1)
        cursor = ann_start + len(ann_name) + 1
        
        while cursor < len(content) and content[cursor].isspace():
            cursor += 1
        
        if cursor < len(content) and content[cursor] == "(":
            depth = 1
            cursor += 1
            in_str = False
            in_char = False
            escape = False
            while cursor < len(content) and depth > 0:
                c = content[cursor]
                if escape:
                    escape = False
                elif c == "\\" and (in_str or in_char):
                    escape = True
                elif c == '"' and not in_char:
                    in_str = not in_str
                elif c == "'" and not in_str:
                    in_char = not in_char
                elif not in_str and not in_char:
                    if c == "(":
                        depth += 1
                    elif c == ")":
                        depth -= 1
                cursor += 1
            ann_end = cursor
        else:
            ann_end = ann_start + len(ann_name) + 1

        line_start = content.rfind("\n", 0, ann_start)
        if line_start == -1:
            line_start = 0
        else:
            line_start += 1
        prefix = content[line_start:ann_start]

        if prefix.strip() == "":
            trailing = cursor
            while trailing < len(content) and content[trailing] in " \t":
                trailing += 1
            if trailing < len(content) and content[trailing:trailing+2] == "\r\n":
                content = content[:line_start] + content[trailing+2:]
                pos = line_start
                continue
            elif trailing < len(content) and content[trailing] == "\n":
                content = content[:line_start] + content[trailing+1:]
                pos = line_start
                continue

        trailing = cursor
        while trailing < len(content) and content[trailing] in " \t":
            trailing += 1
        content = content[:ann_start] + content[trailing:]
        pos = ann_start

    for imp_pat in SWAGGER_IMPORTS:
        content = re.sub(imp_pat, "", content)
    
    content = re.sub(r"(\r?\n){3,}", r"\1\1", content)
    return content

def process():
    changed = []
    for root, _, files in os.walk(TARGET_DIR):
        if "target" in root.split(os.sep):
            continue
        for file in files:
            if not file.endswith(".java"):
                continue
            p = os.path.join(root, file)
            with open(p, "r", encoding="utf-8") as f:
                orig = f.read()
            if "io.swagger" in orig or "@Schema" in orig or "@Tag" in orig or "@Operation" in orig:
                res = remove_annotations(orig)
                if res != orig:
                    with open(p, "w", encoding="utf-8") as f:
                        f.write(res)
                    changed.append(p)
    print(f"Updated {len(changed)} files.")

if __name__ == "__main__":
    process()
