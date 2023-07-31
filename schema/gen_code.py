import os
import subprocess

KOTLIN_OUT_DIR = '../dexkit/src/main/java/org/luckypray/dexkit'
CPP_OUT_DIR = '../Core/dexkit/schema'


files = []
for dir, _, _files in os.walk('./fbs'):
    for file in _files:
        if file.endswith('.fbs'):
            path = os.path.join(dir, file)
            files.append(path)

# kotlin
subprocess.call(['./flatc', '--kotlin', '-o', KOTLIN_OUT_DIR] + files)
for dir, _, _files in os.walk(KOTLIN_OUT_DIR):
    for file in _files:
        file_path = os.path.join(dir, file)
        with open(file_path, 'r') as f:
            content = f.read()
        content = content.replace('package schema', 'package org.luckypray.dexkit.schema')
        content = content.replace('schema.', '')
        with open(file_path, 'w') as f:
            f.write(content)

# c++
subprocess.call(['./flatc', '--cpp', '--cpp-std', 'c++17', '-o', CPP_OUT_DIR] + files)