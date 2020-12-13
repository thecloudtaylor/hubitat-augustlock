/*
Hubitat Driver For August Door Lock
Copyright 2020 - Taylor Brown

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

12-8-2020 :  Initial 
*/

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

metadata {
    definition (name: "August Keypad", namespace: "thecloudtaylor", author: "Taylor Brown") {
        capability "Refresh"
        capability "LockCodes"
        attribute "Battery Level", "string"
    }

    preferences{
        input ("debugLogs", "bool", 
			   title: "Enable debug logging", 
			   defaultValue: false)
		input ("descriptionText", "bool", 
			   title: "Enable description text logging", 
			   defaultValue: true)
    }
}

void LogDebug(logMessage)
{
    if(debugLogs)
    {
        log.debug "${device.displayName} ${logMessage}";
    }
}

void LogInfo(logMessage)
{
    log.info "${device.displayName} ${logMessage}";
}

void LogWarn(logMessage)
{
    log.warn "${device.displayName} ${logMessage}";
}

void disableDebugLog() 
{
    LogWarn("Disabling Debug Logging.");
    device.updateSetting("debugLogs",[value:"false",type:"bool"]);
}

void installed()
{
    LogInfo("Installing.");
    refresh()
}

void uninstalled()
{
    LogInfo("Uninstalling.");
}

void updated() 
{
    LogInfo("Updating.")
}

void parse(String message) 
{
    LogDebug("ParseCalled: ${message}");
}

void refresh() 
{
    LogDebug("RefreshCalled");

    parent.refresh()
}

void deleteCode(codeposition) 
{
    LogDebug("deleteCode(): ${codeposition}");
}

void getCodes() 
{
    LogDebug("getCodes()");
    def augustJson = parent.getCodes(device);
}

void setCode(codeposition, pincode, name) 
{
    LogDebug("setCode(): codeposition:${codeposition}, pincode:${pincode}, name:${name}");
}

void setCodeLength(pincodelength) 
{
    LogDebug("setCodeLength(): ${pincodelength}");
}

void updateKeypad(keypadMap)
{
    LogDebug("updateKeypad() keypadMap: ${keypadMap}");

    def keyPadBatteryLevel = keypadMap.batteryLevel
    LogDebug("updateLockDeviceStatus-KeyPadBatt: ${keyPadBatteryLevel}")
    sendEvent(name:"Battery Level", value:keyPadBatteryLevel)
}