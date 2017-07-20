#!/bin/bash

### BEGIN INIT INFO
# Provides:          @packageName@
# Required-Start:    $local_fs $remote_fs $syslog $network
# Required-Stop:     $local_fs $remote_fs $syslog $network
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: @project.parent.name@
# Description:       @project.parent.name@
### END INIT INFO

# Authors: Jakob Frank <jakob.frank@redlink.co>

NAME="@packageName@"
DESC="@project.parent.name@"

# Setup variables
EXEC=/usr/share/${NAME}/@binaryName@
HOME="/var/lib/${NAME}"
USER=@daemonUser@
GROUP=@daemonUser@

JVMS=(/usr/lib/jvm/jre-1.8.0/bin/java /usr/bin/java8 java8 /usr/bin/java java)
JVM=
JVM_ARGS="-Xmx4g"

. /etc/init.d/functions

# Read configuration variable file if it is present
[ -r /etc/default/${NAME} ] && . /etc/default/${NAME}

PID_FILE="${HOME}/smarti.pid"
ARGS="${ARGS}"

if [ -z "$EXEC" ] || [ -z "$HOME" ]; then
    echo "Incomplete Configuration, set EXEC and HOME!" "$NAME"
    exit 9
fi

if [ -z "$JVM" ]; then
    for j in "${JVMS[@]}"; do
        JVM=$(which $j 2>/dev/null || true)
        [ -n "$JVM" -a -x "$JVM" ] && break
    done
fi

do_start() {
    if [ -f ${PID_FILE} ]; then
        echo "Already running (PID file found)"
    else
        echo "Starting ${EXEC}..."
        cd $HOME
        export HOME JVM JVM_ARGS ARGS
        daemon --user=${USER} \
          ${EXEC} >/var/log/${NAME}/main.out 2>&1 &
    fi
}

do_stop() {
    if [ ! -f ${PID_FILE} ]; then
        echo "${NAME} not running (PID not found)"
    else
        PID=$(cat ${PID_FILE})
        COUNT=0
        LIMIT=3
        while $(kill -0 ${PID} > /dev/null 2>&1); do
            if [ ${COUNT} -lt ${LIMIT} ]; then
                echo "Stopping ${NAME}... (pid=${PID}, try=${COUNT})"
                if ! kill ${PID}; then
                    echo "Could not send SIGTERM to process $PID"
                fi
            else
                echo "Forcing stop of ${NAME} (pid=${PID}) after ${COUNT} tries..."
                if ! kill -9 ${PID}; then
                    echo "Could not send SIGTERM to process $PID"
                fi
            fi
            COUNT=$((COUNT+1))
            sleep $((COUNT*5))
        done
        rm -f ${PID_FILE}
    fi
}

check_status() {
    if [ ! -f ${PID_FILE} ]; then
        echo "${NAME} not running (PID not found)"
    else
        PID=$(cat ${PID_FILE})
        if $(kill -0 ${PID} > /dev/null 2>&1); then
            echo "${NAME} running (PID ${PID})"
        else
            echo "${NAME} not running (warning: but PID found at ${PID_FILE}!)"
        fi
    fi
}

case "$1" in
    start)
        echo "Starting $DESC" "$NAME"
        do_start
        ;;
    stop)
        echo "Stopping $DESC" "$NAME"
        do_stop
        ;;
    restart)
        echo "Restarting $DESC" "$NAME"
        do_stop
        do_start
        ;;
    status)
        check_status
        ;;
    *)
        echo "Usage: daemon {start|stop|restart|status}" >&2
        exit 3
        ;;
esac