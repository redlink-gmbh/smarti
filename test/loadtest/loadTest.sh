#!/bin/bash

SMARTI_URL=${SMARTI_URL:-http://localhost:8080/}

CLIENT=${CLIENT:-loadtest}
CHANNELS=10
MESSAGES=20
CALLBACK=${CALLBACK}
TOKEN=

while [ $# -gt 0 ]; do
    _arg="$1"
    shift
    case "$_arg" in
	-c|--channels)
	    CHANNELS="$1"; shift ;;
	-m|--messages)
	    MESSAGES="$1"; shift ;;
	-C|--client)
	    CLIENT="$1"; shift ;;
	-s|--smarti)
	    SMARTI_URL="$1"; shift ;;
	-T|--token)
	    TOKEN="-H X-Auth-Token:$1"; shift ;;
	--callback)
	    CALLBACK="$1"; shift ;;
        --)
            exec "$@"
            exit $? ;;
	*)
	    echo "Unknow param $_arg" >&2
	    exit 1 ;;
    esac
done

function rndStr() {
    cat /dev/urandom \
        | tr -dc 'a-zA-Z0-9_' \
        | tr '[:upper:]' '[:lower:]' \
        | fold -w ${1:-16} \
        | head -n 1
}

function pushMessage() {
    local _m="${2:-$(rndStr 16)}"
    echo '[]' \
        | jq --arg c "${1}" \
             --arg m "${_m}" \
             --arg t "${3}" \
             --arg u "${4:-$(rndStr 8)}" \
             --arg h "${CALLBACK:-${SMARTI_URL%/}/debug/${CLIENT}_${_m}}" \
             --argjson d "${5:-null}" \
             '{message_id: $m, channel_id: $c, text: $t, token: null, user_id: $u, timestamp: $d, webhook_url: $h}' \
        | curl $TOKEN -sX POST \
               "${SMARTI_URL%/}/rocket/${CLIENT}" \
               -H 'Content-Type: application/json' \
               --data-binary @-
}

function closeChannel() {
    echo -n "Closing channel $1"
    _cID=$(curl $TOKEN -s "${SMARTI_URL%/}/rocket/${CLIENT}/${1}/conversationid")
    echo " = conversation $_cID"
    curl $TOKEN -sX POST "${SMARTI_URL%/}/conversation/${_cID}/publish" \
         | jq '{id:.id,status:.meta.status}'
}

_c=0
while [ ${_c} -lt ${CHANNELS} ]; do
    _channel_id=$(rndStr)
    echo "Starting conversation $((_c+1))/${CHANNELS}: ${_channel_id}"
    _m=0
    while [ ${_m} -lt ${MESSAGES} ]; do
        _date=$(date "+%s" -d "-1 day -$_c day +$_m min")
        echo -n "Sending message $((_m+1))/${MESSAGES} ($(date '+%F_%T' -d@${_date}))"
        pushMessage ${_channel_id} ${_channel_id}_$((_m+1)) "$(${FORTUNE:-fortune} -l de)" '' "${_date}000"
        echo " done"
        ((_m++))
    done
    closeChannel ${_channel_id}
    echo "Completed conversation $((_c+1))/${CHANNELS}: ${_channel_id}"
    echo
    ((_c++))
done
