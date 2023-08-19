import os
import re
import subprocess

KOTLIN_OUT_DIR = '../dexkit/src/main/java/org/luckypray'
KOTLIN_ALIAS_OUT_DIR = '../dexkit/src/main/java/org/luckypray/dexkit'
CPP_OUT_DIR = '../Core/dexkit/include/schema'


files = []
for dir, _, _files in os.walk('./fbs'):
    for file in _files:
        if file.endswith('.fbs'):
            path = os.path.join(dir, file)
            files.append(path)

# kotlin
subprocess.call(['./flatc', '--kotlin', '--gen-mutable', '-o', KOTLIN_OUT_DIR] + files)
for dir, _, _files in os.walk(f"{KOTLIN_OUT_DIR}/dexkit/schema"):
    class_list = []
    for file in _files:
        class_list.append(file.replace('.kt', ''))
    class_list = sorted(class_list)
    for file in _files:
        file_path = os.path.join(dir, file)
        with open(file_path, 'rb') as f:
            content = f.read()
        content = content.replace(b'package dexkit.schema', b'package org.luckypray.dexkit.schema')
        content = content.replace(b'dexkit.schema.', b'')
        content = content.replace(b'\nclass ', b'\ninternal class ')
        for v in class_list:
            lines = []
            for line in content.splitlines():
                if b'const val' not in line:
                    line = re.sub(f'(?<!\w){v}(?!\w)'.encode(), f'`-{v}`'.encode(), line)
                lines.append(line)
            content = b'\n'.join(lines)
        with open(file_path, 'wb') as f:
            f.write(content)
    with open(f'{KOTLIN_ALIAS_OUT_DIR}/Alias.kt', 'wb') as f:
        f.write(b'package org.luckypray.dexkit\n\n')
        for v in class_list:
            f.write(f'internal typealias Inner{v} = org.luckypray.dexkit.schema.`-{v}`\n'.encode())
        f.write(b'\n')

# c++
subprocess.call(['./flatc', '--cpp', '--cpp-std', 'c++17', '--scoped-enums', '--no-emit-min-max-enum-values', '-o', CPP_OUT_DIR] + files)