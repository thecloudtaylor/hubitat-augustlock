/*
Hubitat Driver For August Door Lock
Copyright 2020 - Taylor Brown

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

11-25-2020 :  Initial 
11-28-2020 :  0.0.1 Alpha
12-4-2020:    Refactor drivers
*/

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

metadata {
    definition (name: "August Lock", namespace: "thecloudtaylor", author: "Taylor Brown") {
        capability "Lock"
        capability "Refresh"
        capability "Battery"
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

    parent.updateLockDeviceStatus(device)

}

void lock()
{
    LogInfo("Locking Door");
    parent.lockDoor(device)

}

void unlock()
{
    LogInfo("Unlocking Door");
    parent.unlockDoor(device)

}

void createChildKeypad(id, lockId)
{
    LogInfo("createChildKeypad(id:${id}; lockId:${lockId})")
    
}