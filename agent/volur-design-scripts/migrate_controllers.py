import os
import re

ROOT = os.path.abspath("src/volur-lite-platform")

for root, _, files in os.walk(ROOT):
    for f in files:
        if not f.endswith("Controller.java"):
            continue
        path = os.path.join(root, f)
        with open(path, "r", encoding="utf-8") as fp:
            content = fp.read()

        if "public ApiResponse<" not in content and "ApiResponse<" not in content:
            continue

        new_content = re.sub(r"public\s+ApiResponse<", "public ApiResponseEntity<", content)
        new_content = re.sub(r"private\s+ApiResponse<", "private ApiResponseEntity<", new_content)
        new_content = re.sub(r"protected\s+ApiResponse<", "protected ApiResponseEntity<", new_content)

        if "import com.humolix.volur.framework.common.api.ApiResponseEntity;" not in new_content:
            if "import com.humolix.volur.framework.common.api.ApiResponse;" in new_content:
                new_content = new_content.replace(
                    "import com.humolix.volur.framework.common.api.ApiResponse;",
                    "import com.humolix.volur.framework.common.api.ApiResponseEntity;"
                )
            else:
                pkg_match = re.search(r"package\s+[^;]+;\n", new_content)
                if pkg_match:
                    pos = pkg_match.end()
                    new_content = new_content[:pos] + "\nimport com.humolix.volur.framework.common.api.ApiResponseEntity;\n" + new_content[pos:]

        if "ApiResponse<" in new_content and "import com.humolix.volur.framework.common.api.ApiResponse;" not in new_content:
            new_content = new_content.replace(
                "import com.humolix.volur.framework.common.api.ApiResponseEntity;",
                "import com.humolix.volur.framework.common.api.ApiResponse;\nimport com.humolix.volur.framework.common.api.ApiResponseEntity;"
            )

        if new_content != content:
            with open(path, "w", encoding="utf-8") as fp:
                fp.write(new_content)
            print(f"Updated {f}")
