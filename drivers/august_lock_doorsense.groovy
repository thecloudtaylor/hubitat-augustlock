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
    definition (name: "August Lock with DoorSense", namespace: "thecloudtaylor", author: "Taylor Brown") {
        capability "Lock"
        capability "Refresh"
        capability "ContactSensor"
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

void dooropened()
{
    LogDebug("DoorOpenedCalled");

    sendEvent(name:"contact", value: "open", isStateChange: true, descriptionText: "Door Opened");
}

void doorclosed()
{
    LogDebug("DoorClosedCalled");

    sendEvent(name:"contact", value: "closed", isStateChange: true, descriptionText: "Door Closed");
}

//common lock code

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
    
    def children = getChildDevices()
    LogInfo("Deleting all child devices: ${children}")
    children.each {
        if (it != null) {
            deleteChildDevice(it.getDeviceNetworkId())
        }
    } 
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
    try 
    {
        addChildDevice(
            'thecloudtaylor',
            'August Keypad',
            "${id}",
            [
                name: "August Keypad",
                label: "KeypadID: ${id} LockID: ${lockId}"
            ])
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

void updateKeypad(keypadMap)
{
    LogDebug("updateKeypad() keypadMap: ${keypadMap}");

    def keyDev = getChildDevice(keypadMap._id)
    if (keyDev == null)
    {
        LogDebug("Keypad was found but get Device was Null.")
        createChildKeypad(keypadMap._id, keypadMap.lockID)
        keyDev = getChildDevice(keypadMap._id)
    }

    keyDev.updateKeypad(keypadMap)
}

void getCodes(com.hubitat.app.DeviceWrapper keypadDevice)
{
    LogDebug("getCodes()");

    parent.updateLockCodes(device, keypadDevice)
}