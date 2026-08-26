import os

# root build.gradle
with open('build.gradle', 'r') as f:
    content = f.read()
if 'kotlin-gradle-plugin' not in content:
    content = content.replace("classpath 'com.android.tools.build:gradle:8.13.0'", "classpath 'com.android.tools.build:gradle:8.13.0'\n        classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0'")
with open('build.gradle', 'w') as f:
    f.write(content)

# app build.gradle
with open('app/build.gradle', 'r') as f:
    content = f.read()
if 'kotlin-android' not in content:
    content = content.replace("apply plugin: 'com.android.application'", "apply plugin: 'com.android.application'\napply plugin: 'kotlin-android'")
    content = content.replace("dependencies {", "dependencies {\n    implementation \"org.jetbrains.kotlin:kotlin-stdlib:1.9.0\"\n    testImplementation \"org.jetbrains.kotlin:kotlin-test-junit:1.9.0\"")
with open('app/build.gradle', 'w') as f:
    f.write(content)
