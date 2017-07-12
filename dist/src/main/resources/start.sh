#!/usr/bin/env bash

${JVM:-java} ${JVM_ARGS} -jar /usr/share/${packageName}/${binaryName}.jar ${ARGS}
