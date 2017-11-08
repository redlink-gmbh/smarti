#!/usr/bin/env bash

_INSTALL=/usr/share/${packageName}

${JVM:-java} ${JVM_ARGS} -jar ${_INSTALL}/db-migrator.jar ${ARGS} \
    || { echo "Error running database-migration scripts, refusing to start!"; exit 35; }
${JVM:-java} ${JVM_ARGS} -jar ${_INSTALL}/${binaryName}.jar ${ARGS}
