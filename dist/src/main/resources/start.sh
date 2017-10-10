#!/usr/bin/env bash

_INSTALL=/usr/share/${packageName}
if which mongo &>/dev/null; then
    _MONGO_URI=
    # check application properties
    if [ -r application.properties ]; then
        _MONGO_URI=$(grep spring.data.mongodb.uri application.properties | cut -d= -f2 | tr -d '[:space:]')
    fi
    # check ENV
    if [ -n "${SPRING_DATA_MONGODB_URI}" ]; then
        _MONGO_URI="${SPRING_DATA_MONGODB_URI}"
    fi
    # check java system properties
    for D in ${JVM_ARGS} ${ARGS}; do
        case ${D} in
        -Dspring.data.mongodb.uri=*)
            _MONGO_URI=$(echo "$D" | cut -d= -f2 | tr -d '[:space:]')
            ;;
        *) ;;
        esac
    done
    # check direct start-parameters
    for P in ${ARGS}; do
        case ${P} in
        --spring.data.mongodb.uri=*)
            _MONGO_URI=$(echo "$P" | cut -d= -f2 | tr -d '[:space:]')
            _NEXT=false
            ;;
        --spring.data.mongodb.uri)
            _NEXT=true
            ;;
        *)
            if [ ${_NEXT:-false} == true ]; then
                _MONGO_URI="$P"
            fi
            _NEXT=false
            ;;
        esac
    done


    if [ -z "${_MONGO_URI}" ]; then
        echo "Could not determine mongo-db connection, can't run migration scripts"
        echo "If startup fails, please manually run the migration scripts in ${_INSTALL}/scripts"
    else
        echo "Running database migration scripts from {_INSTALL}/scripts against ${_MONGO_URI}"
        find "${_INSTALL}/scripts" -name 'migrate-*' | sort | while read s; do
            SC=$(basename "${s}")
            echo " -- START $SC"
            mongo ${_MONGO_URI} ${s}
            echo " -- END   $SC"
        done
    fi
else
    echo "mongo-executable not found, can't run migration scripts"
    echo "If startup fails, please manually run the migration scripts in ${_INSTALL}/scripts"
fi

${JVM:-java} ${JVM_ARGS} -jar ${_INSTALL}/${binaryName}.jar ${ARGS}
