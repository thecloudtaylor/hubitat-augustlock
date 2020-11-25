/*
Hubitat Driver For August Door Lock
(C) 2020 - Taylor Brown

11-25-2020 :  Initial 
*/
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

metadata {
    definition (name: "August WiFI Lock", namespace: "thecloudtaylor", author: "Taylor Brown") {
        capability "Lock"
        capability "Actuator"
        capability "Refresh"
        capability "ContactSensor"
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

}

void lock()
{
    LogInfo("Locking Door");
}

void unlock()
{
    LogInfo("Unlocking Door");
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