/*
Hubitat App For August Home

Copyright 2020 - Taylor Brown

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.


11-25-2020 :  Initial 

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

        //connectToAugust()
        getDiscoverButton()
        
        section {
            input name: "debugOutput", type: "bool", title: "Enable Debug Logging?", defaultValue: false, submitOnChange: true
        }
        
        listDiscoveredDevices()
        
        getDebugLink()
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
            input 'refreshDevices', 'button', title: 'Refresh all devices', submitOnChange: true
        }
        section {
            input 'initialize', 'button', title: 'initialize', submitOnChange: true
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
    createAccessToken();
    
}

def initialize() 
{
    LogInfo("Initializing August Home.");
    unschedule()
    //sessionRequest()
    //sendVerificationCodeRequest()
    sendVerificationCodeResponse()
    //getLocks();
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

def sessionRequest(loginMethod="email", def userName="***REMOVED***", def password='***REMOVED***') 
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
    LogDebug("connectToAugust-params ${params}")

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

def sendVerificationCodeRequest(loginMethod="email", def userName="***REMOVED***") 
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
            //[installId:66b5b48f-ec95-46f1-ac65-04e5a491cce8, applicationId:, userId:f9a97f3d-1516-4092-99dc-bd2956a8a34f, vInstallId:false, vPassword:true, vEmail:false, vPhone:false, hasInstallId:true, hasPassword:true, hasEmail:true, hasPhone:true, isLockedOut:false, captcha:, email:[], phone:[], expiresAt:2021-03-29T01:24:49.273Z, temporaryAccountCreationPasswordLink:, iat:1606613089, exp:1616981089, LastName:Brown, FirstName:Taylor]
        
        }
    }
    catch (groovyx.net.http.HttpResponseException e) 
    {
        LogError("connectToAugust failed -- ${e.getLocalizedMessage()}: ${e.response.data}")
        return false;
    }

}

def sendVerificationCodeResponse(loginMethod="email", def userName="***REMOVED***", def verifcationCode="526818") 
{
    LogDebug("sendVerificationCodeRequest()");



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

def getLocks() 
{
    LogDebug("getLocks()");

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
        }
    }
    catch (groovyx.net.http.HttpResponseException e) 
    {
        LogError("connectToAugust failed -- ${e.getLocalizedMessage()}: ${e.response.data}")
        return false;
    }

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
}



def appButtonHandler(btn) {
    switch (btn) {
    case 'discoverDevices':
        discoverDevices()
        break
    case 'refreshToken':
        refreshToken()
        break
    case 'deleteDevices':
        deleteDevices()
        break
    case 'refreshDevices':
        refreshAllThermostats()
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

    
}



def handleAuthRedirect() 
{
    LogDebug("handleAuthRedirect()");

    def authCode = params.code

    LogDebug("AuthCode: ${authCode}")
    def authorization = ("${global_conusmerKey}:${global_consumerSecret}").bytes.encodeBase64().toString()

    def headers = [
                    Authorization: authorization,
                    Accept: "application/json"
                ]
    def body = [
                    grant_type:"authorization_code",
                    code:authCode,
                    redirect_uri:global_redirectURL
    ]
    def params = [uri: global_apiURL, path: "/oauth2/token", headers: headers, body: body]
    
    try 
    {
        httpPost(params) { response -> loginResponse(response) }
    } 
    catch (groovyx.net.http.HttpResponseException e) 
    {
        LogError("Login failed -- ${e.getLocalizedMessage()}: ${e.response.data}")
    }

    def stringBuilder = new StringBuilder()
    stringBuilder << "<!DOCTYPE html><html><head><title>Honeywell Connected to Hubitat</title></head>"
    stringBuilder << "<body><p>Hubitate and Honeywell are now connected.</p>"
    stringBuilder << "<p><a href=http://${location.hub.localIP}/installedapp/configure/${app.id}/mainPage>Click here</a> to return to the App main page.</p></body></html>"
    
    def html = stringBuilder.toString()

    render contentType: "text/html", data: html, status: 200
}

def refreshToken()
{
    LogDebug("refreshToken()");

    if (state.refresh_token != null)
    {
        def authorization = ("${global_conusmerKey}:${global_consumerSecret}").bytes.encodeBase64().toString()

        def headers = [
                        Authorization: authorization,
                        Accept: "application/json"
                    ]
        def body = [
                        grant_type:"refresh_token",
                        refresh_token:state.refresh_token

        ]
        def params = [uri: global_apiURL, path: "/oauth2/token", headers: headers, body: body]
        
        try 
        {
            httpPost(params) { response -> loginResponse(response) }
        } 
        catch (groovyx.net.http.HttpResponseException e) 
        {
            LogError("Login failed -- ${e.getLocalizedMessage()}: ${e.response.data}")  
        }
    }
    else
    {
        LogError("Failed to refresh token, refresh token null.")
    }
}

def loginResponse(response) 
{
    LogDebug("loginResponse()");

    def reCode = response.getStatus();
    def reJson = response.getData();
    LogDebug("reCode: {$reCode}")
    LogDebug("reJson: {$reJson}")

    if (reCode == 200)
    {
        state.access_token = reJson.access_token;
        state.refresh_token = reJson.refresh_token;
        
        def runTime = new Date()
        def expireTime = (Integer.parseInt(reJson.expires_in) - 100)
        runTime.setSeconds(expireTime)
        LogDebug("TokenRefresh Scheduled at: ${runTime}")
        schedule(runTime, refreshToken)
    }
    else
    {
        LogError("LoginResponse Failed HTTP Request Status: ${reCode}");
    }
}


