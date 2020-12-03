#!/bin/bash

usage="Usage: $(basename "$0") [-v | --version]<version in simver formate> [-a|--appPath]<path to appgroovy> [-d|--driverPath]<path to drivergroovy> [-r|--releasenotes]<releasenotes>)"

while [[ "$#" -gt 0 ]]; do
    case $1 in
        -v|--version) version="$2"; shift ;;
        -a|--appPath) appPath="$2"; shift;;
        -d|--driverPath) driverPath="$2"; shift ;;
        -r|--releasenotes) relNotes="$2"; shift ;;
        *) echo "Unknown parameter passed: $1"; echo $usage; exit 1 ;;
    esac
    shift
done

if [[ -z "${version}" ]] || [[ -z "${appPath}" ]] || [[ -z "${driverPath}" ]] || [[ -z "${relNotes}" ]]; then
  echo "Not all requied parmiters passed!"; echo $usage; exit 1 ;
fi

echo "Version: $version"
echo "AppPath: $appPath"
echo "DriverPath: $driverPath"
echo "ReleaseNotes: $relNotes"


hpm manifest-modify-driver --id f3be8fbc-03aa-40b7-b02e-317817fbbfcb --version=$version --location $driverPath packageManifest.json
hpm manifest-modify-app --id 3b8f568d-b664-4d6f-94cd-ec319319ed1b --version=$version --location $appPath packageManifest.json
hpm manifest-modify --releasenotes="$relNotes" --version=$version packageManifest.json