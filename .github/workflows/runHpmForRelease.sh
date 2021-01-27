#!/bin/bash

usage="Usage: $(basename "$0") [-g | --githubRef]<github referance> [-r | --releaseTag]<GitHub Release Tag in simver format>"

while [[ "$#" -gt 0 ]]; do
    case $1 in
        -r|--releaseTag) releaseTag="$2"; shift ;;
        -g|--githubRef) githubRef="$2"; shift ;;
        *) echo "Unknown parameter passed: $1"; echo $usage; exit 1 ;;
    esac
    shift
done

if [[ -z "${githubRef}" ]]; then
  echo "Not all requied parmiters passed!"; echo $usage; exit 1 ;
fi

echo "Param githubRef: $githubRef"
echo "Param releaseTag: $releaseTag"


if [[ -z "${releaseTag}" ]]; then
  version=$(echo $githubRef | grep -Eo [0-9].[0-9].[0-9]);
  baseCodePath="https://raw.githubusercontent.com/thecloudtaylor/hubitat-augustlock/$githubRef";
else
  version=$(echo $releaseTag | grep -Eo [0-9].[0-9].[0-9]);
  baseCodePath="https://raw.githubusercontent.com/thecloudtaylor/hubitat-augustlock/$releaseTag";
fi

echo "SimVersion: $version"

if [[ -z "${version}" ]]; then
  echo "Tag string did not contain a proper version (x.y.z)"; exit 1 ;
fi

echo $baseCodePath

releaseJson=$(curl -H "Accept: application/vnd.github.v3+json" https://api.github.com/repos/thecloudtaylor/hubitat-augustlock/releases/tags/$releaseTag)
echo $releaseJson

releaseNote=$(jq .body <<<$releaseJson)
echo $releaseNote

if [[ -z "${releaseNote}" ]]; then
  echo "Release Notes Could Not Be Found - did the release have body text?"; exit 1 ;
fi

appPath="$baseCodePath/augusthomeapp.groovy"
lockDriverPath="$baseCodePath/drivers/august_lock.groovy"
lockDoorSenseDriverPath="$baseCodePath/drivers/august_lock_doorsense.groovy"
keyPadDriverPath="$baseCodePath/drivers/august_keypad.groovy"

echo "Version: $version"
echo "AppPath: $appPath"
echo "lockDriverPath: $lockDriverPath"
echo "lockDoorSenseDriverPath: $lockDoorSenseDriverPath"
echo "keyPadDriverPath: $keyPadDriverPath"
echo "ReleaseNotes: $releaseNote"

hpmCmd="./.github/workflows/tools/hpm manifest-modify-driver --id e2490bbd-ed44-4141-a270-f3c6e6b9771c --version=$version --location $lockDoorSenseDriverPath ../hubitat-packages/packages/augustManifest.json"
echo "Running: $hpmCmd"
eval $hpmCmd
[ $? -eq 0 ]  || exit 1

hpmCmd="./.github/workflows/tools/hpm manifest-modify-driver --id 86e5b69e-8809-47a9-b106-0ef0116bf3c7 --version=$version --location $lockDriverPath ../hubitat-packages/packages/augustManifest.json"
echo "Running: $hpmCmd"
eval $hpmCmd
[ $? -eq 0 ]  || exit 1

hpmCmd="./.github/workflows/tools/hpm manifest-modify-driver --id 030c8220-ec97-475b-9fc8-db157b6bccd0 --version=$version --location $keyPadDriverPath ../hubitat-packages/packages/augustManifest.json"
echo "Running: $hpmCmd"
eval $hpmCmd
[ $? -eq 0 ]  || exit 1

hpmCmd="./.github/workflows/tools/hpm manifest-modify-app --id 3b8f568d-b664-4d6f-94cd-ec319319ed1b --version=$version --location $appPath ../hubitat-packages/packages/augustManifest.json"
echo "Running: $hpmCmd"
eval $hpmCmd
[ $? -eq 0 ]  || exit 1

hpmCmd="./.github/workflows/tools/hpm manifest-modify ../hubitat-packages/packages/augustManifest.json --version=$version --releasenotes=$releaseNote"
echo "Running: $hpmCmd"
eval $hpmCmd
[ $? -eq 0 ]  || exit 1

echo "Output of Manifest:"
cat ../hubitat-packages/packages/augustManifest.json

cd ../hubitat-packages

gitCmd="git config user.name 'AugustHome GitHub Pipeline'"
echo "Running: $gitCmd"
eval $gitCmd
[ $? -eq 0 ]  || exit 1

gitCmd="git config --global user.email 'augutstpipeline@thecloudtaylor.com'"
echo "Running: $gitCmd"
eval $gitCmd
[ $? -eq 0 ]  || exit 1

gitCmd="git add ./packages/augustManifest.json"
echo "Running: $gitCmd"
eval $gitCmd
[ $? -eq 0 ]  || exit 1

gitCmd="git commit -m \"GitHubPipeline Updating August Home Version to $version\""
echo "Running: $gitCmd"
eval $gitCmd
[ $? -eq 0 ]  || exit 1

gitCmd="git push"
echo "Running: $gitCmd"
eval $gitCmd
[ $? -eq 0 ]  || exit 1