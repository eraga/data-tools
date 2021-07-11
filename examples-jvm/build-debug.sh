#!/usr/bin/env bash
#
./gradlew :examples-jvm:clean build  --stacktrace  -Dorg.gradle.debug=true\
    -Dkotlin.compiler.execution.strategy="in-process" \
    -Dkotlin.daemon.jvm.options="-Xdebug,-Xrunjdwp:transport=dt_socket\,address=5005\,server=y\,suspend=n"
