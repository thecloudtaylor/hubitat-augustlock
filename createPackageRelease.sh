#!/bin/bash

usage="Usage: $(basename "$0") [-v | --version]<version in simver formate> [-c|--codePath]<root path to code> [-r|--releasenotes]<releasenotes>)"

while [[ "$#" -gt 0 ]]; do
    case $1 in
        -v|--version) version="$2"; shift ;;
        -c|--codePath) codePath="$2"; shift;;
        -r|--releasenotes) relNotes="$2"; shift ;;
        *) echo "Unknown parameter passed: $1"; echo $usage; exit 1 ;;
    esac
    shift
done

if [[ -z "${version}" ]] || [[ -z "${codePath}" ]] || [[ -z "${relNotes}" ]]; then
  echo "Not all requied parmiters passed!"; echo $usage; exit 1 ;
fi

appPath="$codePath/augusthomeapp.groovy.groovy"
lockDriverPath="$codePath/drivers/august_lock.groovy"
lockDoorSenseDriverPath="$codePath/drivers/august_lock_doorsense.groovy"
keyPadDriverPath="$codePath/drivers/august_keypad.groovy"

echo "Version: $version"
echo "CodePath: $codePath"
echo "AppPath: $appPath"
echo "lockDriverPath: $lockDriverPath"
echo "lockDoorSenseDriverPath: $lockDoorSenseDriverPath"
echo "keyPadDriverPath: $keyPadDriverPath"
echo "ReleaseNotes: $relNotes"

hpm manifest-modify-driver --id e2490bbd-ed44-4141-a270-f3c6e6b9771c --version=$version --location $lockDoorSenseDriverPath packageManifest.json
hpm manifest-modify-driver --id 86e5b69e-8809-47a9-b106-0ef0116bf3c7 --version=$version --location $lockDriverPath packageManifest.json
hpm manifest-modify-driver --id 030c8220-ec97-475b-9fc8-db157b6bccd0 --version=$version --location $keyPadDriverPath packageManifest.json
hpm manifest-modify-app --id 3b8f568d-b664-4d6f-94cd-ec319319ed1b --version=$version --location $appPath packageManifest.json
hpm manifest-modify --releasenotes="$relNotes" --version=$version packageManifest.json