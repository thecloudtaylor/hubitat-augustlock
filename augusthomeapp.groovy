/*
Hubitat App For August Home

Copyright 2020 - Taylor Brown

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.


11-25-2020 :  Initial
11-28-2020 :  0.0.1 Alpha 

Considerable inspiration and examples thanks to: https://github.com/snjoetw/py-august
*/


import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.transform.Field


@Field static String global_apiURL = "https://api-production.august.com"
@Field static String global_HeaderApiKey = "79fd0eb6-381d-4adf-95a0-47721289d1d9"
@Field static String global_HeaderUserAgent = "August/2019.12.16.4708 CFNetwork/1121.2.2 Darwin/19.3.0"
@Field static String global_HeaderAcceptVersion = "0.0.1"



definition(
        name: "August Home",
        namespace: "thecloudtaylor",
        author: "Taylor Brown (@thecloudtaylor)",
        description: "August Home App and Driver",
        category: "DoorLock",
        iconUrl: "",
        iconX2Url: "")

preferences 
{
    page(name: "mainPage")
    page(name: "loginPage", title: "Login Options", install: true)
    page(name: "debugPage", title: "Debug Options", install: true)


}

mappings {
    path("/handleAuth") {
        action: [
            GET: "handleAuthRedirect"
        ]
    }
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "Setup connection to August Home and Discover devices", install: true, uninstall: true) {

        getLoginLink()
        
        getDiscoverButton()
        
        section {
            input name: "debugOutput", type: "bool", title: "Enable Debug Logging?", defaultValue: false, submitOnChange: true
        }
        
        listDiscoveredDevices()
        
        getDebugLink()
    }
}

def loginPage()
{
    dynamicPage(name:"loginPage", title: "Login Options", install: false, uninstall: false) {
    section {
        paragraph "August Login Options"
    }
    getLoginOptions()
    getTwoFA()
    }

}

def debugPage() {
    dynamicPage(name:"debugPage", title: "Debug", install: false, uninstall: false) {
        section {
            paragraph "Debug buttons"
        }
        section {
            input 'refreshToken', 'button', title: 'Force Token Refresh', submitOnChange: true
        }
        section {
            input 'deleteDevices', 'button', title: 'Delete all devices', submitOnChange: true
        }
        section {
            input 'refreshLocks', 'button', title: 'Refresh all locks', submitOnChange: true
        }
        section {
            input 'initialize', 'button', title: 'initialize', submitOnChange: true
        }
    }
}

def getLoginOptions() 
{
 section 
    {
        input name: "loginMethod", type: "enum", title: "Authentication Method", options: ["email", "phone"], required: true, defaultValue: "phone"
        input name: "username", type: "text", title: "Username(email or phone i.e +1(123)456-7890", description: "August Username", required: true
        input name: "password", type: "password", title: "Password", description: "August Password", required: true   
        input 'login', 'button', title: 'Login', submitOnChange: true
    }
}

def getTwoFA() 
{
 section 
    {
        if (state.twoFAneeded == false)
        {

        }
        else
        {
            input name: "twoFAcode", type: "text", title: "Two factor auth code", description: "AuthCode", required: true
            input 'verify', 'button', title: 'Verify', submitOnChange: true
        }
    }
}

def LogDebug(logMessage)
{
    if(settings?.debugOutput)
    {
        log.debug "${logMessage}";
    }
}

def LogInfo(logMessage)
{
    log.info "${logMessage}";
}

def LogWarn(logMessage)
{
    log.warn "${logMessage}";
}

def LogError(logMessage)
{
    log.error "${logMessage}";
}

def installed()
{
    LogInfo("Installing August Home.");
    state.twoFAneeded = false;
    
}

def initialize() 
{
    LogInfo("Initializing August Home.");
    unschedule()
    refreshToken()
    refreshLocks()
}

def updated() 
{
    LogDebug("Updated with config: ${settings}");
    initialize();
}

def uninstalled() 
{
    LogInfo("Uninstalling August Home.");
    unschedule()
    for (device in getChildDevices())
    {
        deleteChildDevice(device.deviceNetworkId)
    }
}

def sessionRequest(loginMethod, userName, password) 
{
    LogDebug("sessionRequest()");


    //state.install_id
    //state.access_token
    //state.access_token_expires 

    def installID = getHubUID()
    def identifier = "${loginMethod}:${userName}"

    def uri = global_apiURL + '/session'

    def headers = [
        "Accept-Version":global_HeaderAcceptVersion,
        "x-august-api-key":global_HeaderApiKey,
        "x-kease-api-key":global_HeaderApiKey,
        "Content-Type":"application/json",
        "User-Agent":global_HeaderUserAgent
    ]
    def body = [
        "installId": installID,
        "identifier": identifier,
        "password": password,
    ]

    def params = [ uri: uri, headers:headers, body: body]
    LogDebug("sessionRequest-params ${params}")

    //bugbug: should be validating resoponse
    //bugbug: should be setting expire
    try
    {
        httpPostJson(params) { response -> 
            
            def reCode = response.getStatus();
            def reJson = response.getData();
            def reHeaders = 
            LogDebug("reCode: ${reCode}")
            LogDebug("reJson: ${reJson}")
            LogDebug("reHeaders: ${reHeaders}")

            state.access_token = response.getHeaders("x-august-access-token").value[0].toString()
            LogDebug("AccessToken: ${state.access_token}")
            //[installId:66b5b48f-ec95-46f1-ac65-04e5a491cce8, applicationId:, userId:f9a97f3d-1516-4092-99dc-bd2956a8a34f, vInstallId:false, vPassword:true, vEmail:false, vPhone:false, hasInstallId:true, hasPassword:true, hasEmail:true, hasPhone:true, isLockedOut:false, captcha:, email:[], phone:[], expiresAt:2021-03-29T01:24:49.273Z, temporaryAccountCreationPasswordLink:, iat:1606613089, exp:1616981089, LastName:Brown, FirstName:Taylor]
        
        }
    }
    catch (groovyx.net.http.HttpResponseException e) 
    {
        LogError("connectToAugust failed -- ${e.getLocalizedMessage()}: ${e.response.data}")
        return false;
    }

}

def sendVerificationCodeRequest(loginMethod, userName) 
{
    LogDebug("sendVerificationCodeRequest()");

    def uri =""

    if (loginMethod == "email")
    {
        uri = global_apiURL + '/validation/email'
    }
    else if (loginMethod == "phone")
    {
        uri = global_apiURL + '/validation/phone'
    }
    else
    {
        LogError("Invalid verification type: ${loginMethod}")
        return false;
    }

    def headers = [
        "Accept-Version":global_HeaderAcceptVersion,
        "x-august-api-key":global_HeaderApiKey,
        "x-kease-api-key":global_HeaderApiKey,
        "Content-Type":"application/json",
        "User-Agent":global_HeaderUserAgent,
        "x-august-access-token":state.access_token
    ]
    def body = [
        "value":userName
    ]

    def params = [ uri: uri, headers:headers, body: body]
    LogDebug("sendVerificationCodeRequest-params ${params}")

    try
    {
        httpPostJson(params) { response -> 
    
            def reCode = response.getStatus();
            def reJson = response.getData();
            LogDebug("reCode: ${reCode}")
            LogDebug("reJson: ${reJson}")
            response.getHeaders().each {
                LogDebug("reHeader: ${it}")
            }
            state.twoFAneeded = true;
            //[installId:66b5b48f-ec95-46f1-ac65-04e5a491cce8, applicationId:, userId:f9a97f3d-1516-4092-99dc-bd2956a8a34f, vInstallId:false, vPassword:true, vEmail:false, vPhone:false, hasInstallId:true, hasPassword:true, hasEmail:true, hasPhone:true, isLockedOut:false, captcha:, email:[], phone:[], expiresAt:2021-03-29T01:24:49.273Z, temporaryAccountCreationPasswordLink:, iat:1606613089, exp:1616981089, LastName:Brown, FirstName:Taylor]
        
        }
    }
    catch (groovyx.net.http.HttpResponseException e) 
    {
        LogError("sendVerificationCodeRequest failed -- ${e.getLocalizedMessage()}: ${e.response.data}")
        return false;
    }

}

def sendVerificationCodeResponse(loginMethod, userName, verifcationCode) 
{
    LogDebug("sendVerificationCodeResponse()");

    def uri =""
    def body = []


    if (loginMethod == "email")
    {
        uri = global_apiURL + '/validate/email'
        body = [
            email:userName,
            "code":verifcationCode
        ]
    }
    else if (loginMethod == "phone")
    {
        uri = global_apiURL + '/validate/phone'
        body = [
            phone:userName,
            "code":verifcationCode
        ]
    }
    else
    {
        LogError("Invalid validate type: ${loginMethod}")
        return false;
    }

    def headers = [
        "Accept-Version":global_HeaderAcceptVersion,
        "x-august-api-key":global_HeaderApiKey,
        "x-kease-api-key":global_HeaderApiKey,
        "Content-Type":"application/json",
        "User-Agent":global_HeaderUserAgent,
        "x-august-access-token":state.access_token
    ]

    def params = [ uri: uri, headers:headers, body: body]
    LogDebug("sendVerificationCodeResponse-params ${params}")

    try
    {
        httpPostJson(params) { response -> 
    
            def reCode = response.getStatus();
            def reJson = response.getData();
            LogDebug("reCode: ${reCode}")
            LogDebug("reJson: ${reJson}")
            response.getHeaders().each {
                LogDebug("reHeader: ${it}")
            }
            state.access_token = response.getHeaders("x-august-access-token").value[0].toString()
            LogDebug("AccessToken: ${state.access_token}")
            //[installId:66b5b48f-ec95-46f1-ac65-04e5a491cce8, applicationId:, userId:f9a97f3d-1516-4092-99dc-bd2956a8a34f, vInstallId:false, vPassword:true, vEmail:false, vPhone:false, hasInstallId:true, hasPassword:true, hasEmail:true, hasPhone:true, isLockedOut:false, captcha:, email:[], phone:[], expiresAt:2021-03-29T01:24:49.273Z, temporaryAccountCreationPasswordLink:, iat:1606613089, exp:1616981089, LastName:Brown, FirstName:Taylor]
            state.twoFAneeded = false;
        }
    }
    catch (groovyx.net.http.HttpResponseException e) 
    {
        LogError("connectToAugust failed -- ${e.getLocalizedMessage()}: ${e.response.data}")
        return false;
    }

}


def refreshToken() 
{
    LogDebug("refreshToken()");

    def uri = global_apiURL + '/users/houses/mine'
    def body = []

    def headers = [
        "Accept-Version":global_HeaderAcceptVersion,
        "x-august-api-key":global_HeaderApiKey,
        "x-kease-api-key":global_HeaderApiKey,
        "Content-Type":"application/json",
        "User-Agent":global_HeaderUserAgent,
        "x-august-access-token":state.access_token
    ]

    def params = [ uri: uri, headers:headers, body: body]
    LogDebug("refreshToken-params ${params}")

    try
    {
        httpGet(params) { response -> 
    
            def reCode = response.getStatus();
            def reJson = response.getData();
            LogDebug("reCode: ${reCode}")
            LogDebug("reJson: ${reJson}")
            response.getHeaders().each {
                LogDebug("reHeader: ${it}")
            }
            state.access_token = response.getHeaders("x-august-access-token").value[0].toString()
            LogDebug("AccessToken: ${state.access_token}")
            //[installId:66b5b48f-ec95-46f1-ac65-04e5a491cce8, applicationId:, userId:f9a97f3d-1516-4092-99dc-bd2956a8a34f, vInstallId:false, vPassword:true, vEmail:false, vPhone:false, hasInstallId:true, hasPassword:true, hasEmail:true, hasPhone:true, isLockedOut:false, captcha:, email:[], phone:[], expiresAt:2021-03-29T01:24:49.273Z, temporaryAccountCreationPasswordLink:, iat:1606613089, exp:1616981089, LastName:Brown, FirstName:Taylor]
        
        }
    }
    catch (groovyx.net.http.HttpResponseException e) 
    {
        LogError("refreshToken failed -- ${e.getLocalizedMessage()}: ${e.response.data}")
        return false;
    }

    def runTime = new Date()
    runTime.setHours(24)
    runOnce(runTime, refreshToken)

}

def discoverLocks() 
{
    LogDebug("discoverLocks()");

    def uri = global_apiURL + '/users/locks/mine'

    def headers = [
        "Accept-Version":global_HeaderAcceptVersion,
        "x-august-api-key":global_HeaderApiKey,
        "x-kease-api-key":global_HeaderApiKey,
        "Content-Type":"application/json",
        "User-Agent":global_HeaderUserAgent,
        "x-august-access-token":state.access_token
    ]

    def params = [ uri: uri, headers:headers]
    LogDebug("getLocks-params ${params}")

    def reJson =''
    try
    {
        httpGet(params) { response -> 
    
            def reCode = response.getStatus();
            reJson = response.getData();
            LogDebug("reCode: ${reCode}")
            LogDebug("reJson: ${reJson}")
            response.getHeaders().each {
                LogDebug("reHeader: ${it}")
            }
        }
    }
    catch (groovyx.net.http.HttpResponseException e) 
    {
        LogError("connectToAugust failed -- ${e.getLocalizedMessage()}: ${e.response.data}")
        return false;
    }
    
    reJson.each 
    {
        LogDebug("LockID: ${it.key}")
        LogDebug("LockName: ${it.value['LockName']}")
        LogDebug("macAddress: ${it.value['macAddress']}")
        LogDebug("HouseID: ${it.value['HouseID']}")
        LogDebug("HouseName: ${it.value['HouseName']}")
        
        try 
        {
            if (${it.value['LockStatus'].doorState != "init")
            {
                addChildDevice(
                    'thecloudtaylor',
                    'August Lock with DoorSense',
                    "${it.key}",
                    [
                        name: "August WiFI Lock",
                        label: "${it.value['HouseName']} - ${it.value['LockName']}"
                    ])
            }
            else
            {
                addChildDevice(
                    'thecloudtaylor',
                    'August Lock',
                    "${it.key}",
                    [
                        name: "August WiFI Lock",
                        label: "${it.value['HouseName']} - ${it.value['LockName']}"
                    ])   
            }
        } 
        catch (com.hubitat.app.exception.UnknownDeviceTypeException e) 
        {
            "${e.message} - you need to install the appropriate driver: ${device.type}"
        } 
        catch (IllegalArgumentException ignored) 
        {
            //Intentionally ignored.  Expected if device id already exists in HE.
        }
    }

}

def refreshLocks()
{
    LogDebug("refreshLocks()");

    def children = getChildDevices()
    children.each 
    {
        if (it != null) 
        {
            getLockStatus(it);
        }
    }
    def refTimeInSec = refreshIntervals.toInteger()

    def runTime = new Date()
    runTime.setSeconds(refTimeInSec)
    LogDebug("LockRefresh Scheduled at: ${runTime}")
    runOnce(runTime, refreshLocks)

}

def getLockStatus(com.hubitat.app.DeviceWrapper device) 
{
    LogDebug("getLockStatus()");

    def deviceID = device.getDeviceNetworkId();

    def uri = global_apiURL + "/locks/${deviceID}/status"

    def headers = [
        "Accept-Version":global_HeaderAcceptVersion,
        "x-august-api-key":global_HeaderApiKey,
        "x-kease-api-key":global_HeaderApiKey,
        "Content-Type":"application/json",
        "User-Agent":global_HeaderUserAgent,
        "x-august-access-token":state.access_token
    ]

    def params = [ uri: uri, headers:headers]
    LogDebug("getLockStatus-params ${params}")

    def reJson =''
    try
    {
        httpGet(params) { response -> 
    
            def reCode = response.getStatus();
            reJson = response.getData();
            LogDebug("reCode: ${reCode}")
            LogDebug("reJson: ${reJson}")
            response.getHeaders().each {
                LogDebug("reHeader: ${it}")
            }
        }
    }
    catch (groovyx.net.http.HttpResponseException e) 
    {
        LogError("getLockStatus failed -- ${e.getLocalizedMessage()}: ${e.response.data}")
        return false;
    }


    def lockStatus = reJson.status
    LogDebug("getLockStatus-status: ${lockStatus}")
    sendEvent(device, [name: 'lock', value: lockStatus])

    def doorState = reJson.doorState
    LogDebug("getLockStatus-doorState: ${doorState}")
    sendEvent(device, [name: 'contact', value: doorState])
}

def lockDoor(com.hubitat.app.DeviceWrapper device) 
{
    LogDebug("lockDoor()");

    def deviceID = device.getDeviceNetworkId();

    def uri = global_apiURL + "/remoteoperate/${deviceID}/lock"

    def headers = [
        "Accept-Version":global_HeaderAcceptVersion,
        "x-august-api-key":global_HeaderApiKey,
        "x-kease-api-key":global_HeaderApiKey,
        "Content-Type":"application/json",
        "User-Agent":global_HeaderUserAgent,
        "x-august-access-token":state.access_token
    ]

    def params = [ uri: uri, headers:headers]
    LogDebug("getLockStatus-params ${params}")

    try
    {
        httpPut(params) { response -> 
    
            def reCode = response.getStatus();
            def reJson = response.getData();
            LogDebug("reCode: ${reCode}")
            LogDebug("reJson: ${reJson}")
            response.getHeaders().each {
                LogDebug("reHeader: ${it}")
            }
            //[installId:66b5b48f-ec95-46f1-ac65-04e5a491cce8, applicationId:, userId:f9a97f3d-1516-4092-99dc-bd2956a8a34f, vInstallId:false, vPassword:true, vEmail:false, vPhone:false, hasInstallId:true, hasPassword:true, hasEmail:true, hasPhone:true, isLockedOut:false, captcha:, email:[], phone:[], expiresAt:2021-03-29T01:24:49.273Z, temporaryAccountCreationPasswordLink:, iat:1606613089, exp:1616981089, LastName:Brown, FirstName:Taylor]
        
        }
    }
    catch (groovyx.net.http.HttpResponseException e) 
    {
        LogError("lockDoor failed -- ${e.getLocalizedMessage()}: ${e.response.data}")
        return false;
    }
    getLockStatus(device)
}

def unlockDoor(com.hubitat.app.DeviceWrapper device) 
{
    LogDebug("unlockDoor()");

    def deviceID = device.getDeviceNetworkId();

    def uri = global_apiURL + "/remoteoperate/${deviceID}/unlock"

    def headers = [
        "Accept-Version":global_HeaderAcceptVersion,
        "x-august-api-key":global_HeaderApiKey,
        "x-kease-api-key":global_HeaderApiKey,
        "Content-Type":"application/json",
        "User-Agent":global_HeaderUserAgent,
        "x-august-access-token":state.access_token
    ]

    def params = [ uri: uri, headers:headers]
    LogDebug("getLockStatus-params ${params}")

    try
    {
        httpPut(params) { response -> 
    
            def reCode = response.getStatus();
            def reJson = response.getData();
            LogDebug("reCode: ${reCode}")
            LogDebug("reJson: ${reJson}")
            response.getHeaders().each {
                LogDebug("reHeader: ${it}")
            }
            //[installId:66b5b48f-ec95-46f1-ac65-04e5a491cce8, applicationId:, userId:f9a97f3d-1516-4092-99dc-bd2956a8a34f, vInstallId:false, vPassword:true, vEmail:false, vPhone:false, hasInstallId:true, hasPassword:true, hasEmail:true, hasPhone:true, isLockedOut:false, captcha:, email:[], phone:[], expiresAt:2021-03-29T01:24:49.273Z, temporaryAccountCreationPasswordLink:, iat:1606613089, exp:1616981089, LastName:Brown, FirstName:Taylor]
        
        }
    }
    catch (groovyx.net.http.HttpResponseException e) 
    {
        LogError("unlockDoor failed -- ${e.getLocalizedMessage()}: ${e.response.data}")
        return false;
    }
    getLockStatus(device)
}


def getDiscoverButton() 
{
    if (state.access_token == null) 
    {
        section 
        {
            paragraph "Device discovery button is hidden until authorization is completed."            
        }
    } 
    else 
    {
        section 
        {
            input 'discoverDevices', 'button', title: 'Discover', submitOnChange: true
        }
    }
}

def getLoginLink() {
    section{
        href(
            name       : 'loginHref',
            title      : 'Login Options',
            page       : 'loginPage',
            description: 'Access Login Options'
        )
    }
}

def getDebugLink() {
    section{
        href(
            name       : 'debugHref',
            title      : 'Debug buttons',
            page       : 'debugPage',
            description: 'Access debug buttons (force Token refresh, delete child devices , refresh devices)'
        )
    }
}

def listDiscoveredDevices() {
    def children = getChildDevices()
    def builder = new StringBuilder()
    builder << "<ul>"
    children.each {
        if (it != null) {
            builder << "<li><a href='/device/edit/${it.getId()}'>${it.getLabel()}</a></li>"
        }
    }
    builder << "</ul>"
    def links = builder.toString()
    section {
        paragraph "Discovered devices are listed below:"
        paragraph links
    }
    section {
        paragraph "Refresh interval (how often devices are automaticaly refreshed/polled):"

        input name: "refreshIntervals", type: "enum", title: "Set the refresh interval.", options: [0:"off", 60:"1 minute", 120:"2 minutes", 300:"5 minutes",600:"10 minutes",900:"15 minutes",1800:"30 minutes",3600:"60 minutes"], required: true, defaultValue: "600"
    }
}

    //
    
    

def appButtonHandler(btn) {
    switch (btn) {
    case 'login':
        sessionRequest(settings.loginMethod, settings.username, settings.password)
        sendVerificationCodeRequest(settings.loginMethod, settings.username)
        break
    case 'verify':
        sendVerificationCodeResponse(settings.loginMethod, settings.username, settings.twoFAcode)
        break
    case 'discoverDevices':
        discoverDevices()
        break
    case 'refreshToken':
        refreshToken()
        break
    case 'deleteDevices':
        deleteDevices()
        break
    case 'refreshLocks':
        refreshLocks()
        break
    case 'initialize':
        initialize()
        break
    }
}

def deleteDevices()
{
    def children = getChildDevices()
    LogInfo("Deleting all child devices: ${children}")
    children.each {
        if (it != null) {
            deleteChildDevice it.getDeviceNetworkId()
        }
    } 
}

def discoverDevices()
{
    LogDebug("discoverDevices()");

    discoverLocks()
}