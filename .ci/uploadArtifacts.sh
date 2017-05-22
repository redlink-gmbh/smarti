#!/bin/bash -e

#SFTP_TARGET="smarti@cerbot.redlink.io"
TIME_ZONE="Europe/Vienna"
TITLE=smarti

##### NO MORE CONFIG BEYOND THIS POINT #####
export TZ=${TIME_ZONE:-UTC}
pwd="$(pwd)"
base="$(dirname "$(readlink -f "$0")")"

SSH_ID="ssh/id_rsa"
SSH_KNOWN_HOSTS="${base}/ssh/known_hosts"

SSH_OPTS="-o CheckHostIP=no -o HashKnownHosts=no"
if [ -r "${SSH_KNOWN_HOSTS}" ]; then
    SSH_OPTS="$SSH_OPTS -o UserKnownHostsFile=${SSH_KNOWN_HOSTS}"
fi

_RELEASE=false
_HASHES=false
while [ $# -gt 0 ]; do
    case "$1" in
    --release|-r)
        _RELEASE=true ;;
    --target|-t)
        SFTP_TARGET="$2"
        shift 1 ;;
    --checksums|-c)
        _HASHES=true ;;
    --)
        shift 1 #break skips the global shift
        break ;;
    *)
        echo "Unknown parameter '$1', exiting"
        exit 1 ;;
    esac
    shift 1
done

TMP=$(mktemp -d)
trap "rm -rf ${TMP}" EXIT

_commit="$(git log -1 --date="format-local:%F_%H-%M-%S" --format=format:"%cd_%h")"
_branch="$(git rev-parse --abbrev-ref HEAD)"
_tag="$(git tag --points-at HEAD | head -n 1)"
_describe="$(git describe --always)"
_date="$(git log -1 --date="format-local:%d.%m.%Y %H:%M:%S" --format=format:"%cd")"

DIR="${BITBUCKET_REPO_SLUG:+${BITBUCKET_REPO_SLUG}/}"
INFO="Branch: ${_branch}"
if [ "${_RELEASE}" == "true" ] && [ "x$_tag" <> "x" ]; then
    DIR="${DIR}releases/${_tag}"
    INFO="Tag:    ${_tag}"
else
    DIR="${DIR}branches/${_branch}/${_commit}"
fi

mkdir -p "${TMP}/upload/${DIR}"
_DL="${TMP}/info.md"
cat >${_DL} <<EOF
### Version Information

\`\`\`
$(git log -1 --format="Commit: %H%n${INFO}%nDate:   %cD%nAuthor: %cN <%cE>")
\`\`\`

## Download

EOF
find "${pwd}" -regex ".*/target/.*\(-exec\.jar\|\.rpm\|\.deb\)" | while read a; do
    f="$(basename "$a")"
    cp "${a}" "${TMP}/upload/${DIR}"
    echo -n "* **[${f}](./${f})**" >>${_DL}
    if [ "$_HASHES" == "true" ]; then
        for hash in md5 sha1 sha256; do
            (
                cd "${TMP}/upload/${DIR}"
                ${hash}sum "${f}" > "${f}.${hash}"
                touch -cr  "${f}"   "${f}.${hash}"
            )
            echo -n " *[${hash}](./${f}.${hash})*" >>${_DL}
        done
    fi
    echo >>${_DL}
done
echo >>${_DL}

if which pandoc &>/dev/null; then
    PAN_OPTS="-s -f markdown -t html5"
    if [ -f ${base}/header.html ]; then PAN_OPTS="${PAN_OPTS} -H ${base}/header.html"; fi
    if [ -f ${base}/pre-content.html ]; then PAN_OPTS="${PAN_OPTS} -B ${base}/pre-content.html"; fi
    if [ -f ${base}/post-content.html ]; then PAN_OPTS="${PAN_OPTS} -A ${base}/post-content.html"; fi

    _xxx=
    [ -d "${pwd}/docs" ] && find "${pwd}/docs" -maxdepth 1 -type f -name '*.md' | while read d; do
        _outFile="$(basename "${d}" .md).html"
        pandoc ${PAN_OPTS} -o "${TMP}/upload/${DIR}/${_outFile}" "$d"

        if [ -z "${_xxx}" ]; then
            echo "### More Documentation" >>${_DL}
            echo >>${_DL}
        fi
        echo "[$(basename "${d}" .md)](${_outFile})" >>${_DL}
        _xxx=true
    done
    echo >>${_DL}

    sed "/<!-- DOWNLOAD -->/r ${_DL}" ${pwd}/README.md \
        | pandoc ${PAN_OPTS} -o "${TMP}/upload/${DIR}/index.html"
    sed -i "s|<title>.*</title>|<title>${TITLE} - ${_date} - ${_describe}</title>|" \
        "${TMP}/upload/${DIR}/index.html"
else
    cat > "${TMP}/upload/${DIR}/header.html" <<EOF
<h1>${BITBUCKET_REPO_SLUG:-${TITLE}}</h1>
<h2>${INFO}</h2>
<pre>$(git log -1 | sed 's/&/\&amp;/g; s/</\&lt;/g; s/>/\&gt;/g; s/"/\&quot;/g; s/'"'"'/\&#39;/g')</pre>
EOF
fi

cat > ${TMP}/batch <<EOF
put -rp "${TMP}/upload/${DIR%%/*}"
bye
EOF

if [ -n "${SSH_KEY_PASS}" ]; then
    mkdir -p "${TMP}/ssh"
    cp "${base}/ssh/"* "${TMP}/ssh/"
    chmod 0600 "${TMP}/${SSH_ID}"
    ssh-keygen -p -P "${SSH_KEY_PASS}" -N "" -f "${TMP}/${SSH_ID}"
    SSH_OPTS="-i ${TMP}/${SSH_ID} ${SSH_OPTS}"
fi

if [ -n "${SFTP_TARGET}" ]; then
    sftp -q ${SSH_OPTS} -b "${TMP}/batch" "${SFTP_TARGET}"
else
    echo "Nowhere to upload :-("
fi
