#!/usr/bin/env bash
#
# Installs everything the suite needs on a Debian/Ubuntu machine: JDK 1.8, Maven, Google
# Chrome and the chromedriver that matches the installed Chrome build.
#
# The script is idempotent - re-running it only fills in what is missing.

set -euo pipefail

CHROME_FOR_TESTING_ENDPOINT="https://googlechromelabs.github.io/chrome-for-testing/known-good-versions-with-downloads.json"
CHROMEDRIVER_TARGET="/usr/local/bin/chromedriver"

log() {
    printf '==> %s\n' "$1"
}

require_apt() {
    if ! command -v apt-get >/dev/null 2>&1; then
        echo "This script supports Debian/Ubuntu only. Install JDK 1.8, Maven and Chrome manually." >&2
        exit 1
    fi
}

install_packages() {
    log "Installing JDK 1.8 and Maven"
    sudo apt-get update -qq
    sudo apt-get install -y -qq openjdk-8-jdk-headless maven curl unzip
}

install_chrome() {
    if command -v google-chrome >/dev/null 2>&1; then
        log "Google Chrome already installed: $(google-chrome --version)"
        return
    fi
    log "Installing Google Chrome"
    local deb
    deb="$(mktemp --suffix=.deb)"
    curl -fsSL -o "$deb" "https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb"
    sudo apt-get install -y -qq "$deb"
    rm -f "$deb"
}

install_chromedriver() {
    local chrome_version
    chrome_version="$(google-chrome --version | grep -oE '[0-9]+(\.[0-9]+){3}')"

    if command -v chromedriver >/dev/null 2>&1 \
        && chromedriver --version | grep -q "${chrome_version%%.*}\."; then
        log "chromedriver already matches Chrome ${chrome_version}"
        return
    fi

    log "Installing chromedriver for Chrome ${chrome_version}"
    local download_url
    download_url="$(curl -fsSL "$CHROME_FOR_TESTING_ENDPOINT" | python3 -c '
import json, sys

chrome_version = sys.argv[1]
milestone = chrome_version.split(".")[0]
versions = json.load(sys.stdin)["versions"]

candidates = [v for v in versions if v["version"] == chrome_version] \
    or [v for v in versions if v["version"].startswith(milestone + ".")]
if not candidates:
    sys.exit("No chromedriver published for Chrome " + chrome_version)

for download in candidates[-1]["downloads"].get("chromedriver", []):
    if download["platform"] == "linux64":
        print(download["url"])
        break
' "$chrome_version")"

    local archive_directory
    archive_directory="$(mktemp -d)"
    curl -fsSL -o "$archive_directory/chromedriver.zip" "$download_url"
    unzip -oq "$archive_directory/chromedriver.zip" -d "$archive_directory"
    sudo install -m 0755 "$(find "$archive_directory" -name chromedriver -type f | head -1)" "$CHROMEDRIVER_TARGET"
    rm -rf "$archive_directory"
    log "Installed $("$CHROMEDRIVER_TARGET" --version)"
}

require_apt
install_packages
install_chrome
install_chromedriver

log "Done. Run the suite with: JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 mvn clean test"
