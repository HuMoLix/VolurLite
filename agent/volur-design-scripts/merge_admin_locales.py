import json
import os
import sys

def merge_for_locale(source_dir, target_file, extra_files):
    if not os.path.exists(target_file):
        base_data = {}
    else:
        with open(target_file, 'r', encoding='utf-8') as f:
            base_data = json.load(f)

    for fname in extra_files:
        path = os.path.join(source_dir, fname + '.json')
        if os.path.exists(path):
            with open(path, 'r', encoding='utf-8') as f:
                mod_data = json.load(f)
            base_data[fname] = mod_data

    with open(target_file, 'w', encoding='utf-8') as f:
        json.dump(base_data, f, ensure_ascii=False, indent=2)

def main():
    root = os.path.abspath(os.path.join(os.path.dirname(__file__), '../..'))
    extra_files = [
        'challenges',
        'competitions',
        'broadcasts',
        'clusters',
        'containers',
        'images',
        'logs',
        'tasks',
        'emails',
        'risk'
    ]

    zh_src = os.path.join(root, 'src', 'volur-lite-view', 'src', 'locales', 'zh_CN', 'admin')
    en_src = os.path.join(root, 'src', 'volur-lite-view', 'src', 'locales', 'en_US', 'admin')

    zh_tgt = os.path.join(root, 'src', 'volur-lite-view-NEXT', 'src', 'locales', 'zh-CN', 'admin.json')
    en_tgt = os.path.join(root, 'src', 'volur-lite-view-NEXT', 'src', 'locales', 'en-US', 'admin.json')
    tw_tgt = os.path.join(root, 'src', 'volur-lite-view-NEXT', 'src', 'locales', 'zh-TW', 'admin.json')

    merge_for_locale(zh_src, zh_tgt, extra_files)
    merge_for_locale(en_src, en_tgt, extra_files)
    merge_for_locale(zh_src, tw_tgt, extra_files)
    print("OK")

if __name__ == '__main__':
    main()
