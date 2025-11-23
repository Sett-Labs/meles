# Meles Datalogging

## Sources and targets

These are commonly found in datalogging software and thus also in Meles.

- **Data Sources:** TCP(as client&server), UDP(as server), MQTT, serial
- **Data Targets:** SQL servers (MySQL, MariaDB, Postgres, MSSQL), SQLite, txt files, UDP(as client)

In addition, Meles adds the following.
- **Data Sources:** I²C, Email, Matrix, (limited) Modbus(RTU/ascii), txt files
- **Data Targets:** Email, Matrix, UDP(sending)

## Features

But it's not because those are listed as commonly found, the way they are implemented is.

**Infrastructure & Deployment**
- Traditional dataloggers: Use purpose-built GUIs or hardware that often require custom network setups for safety, forcing reliance on the vendor's proprietary security model.
- Meles: Meles operates as a daemon hosting a standard Telnet server on a configurable port, running equally well on embedded hardware, Linux, 
and Windows. 
  -> This allows leveraging IT's normal workflow,letting them pick the existing infrastructure solution that best fits their security and management needs.

- Traditional dataloggers: Work primarily as standalone, isolated instances without built-in interaction capabilities with other instances of the same software.
- Meles: Because it can host a tcp server and create custom datastreams any Meles can act as a data source for another instance. Given that the command interface
isn't tied to the user interface, one Meles instance can directly control another.
  -> Allows making small 'sensor hubs' that feed into a central logger without needing other servers.
  
**Data sources** 
 
 - Traditional dataloggers: Communication protocol selection is tied to the next steps in the process, to changing this requires going through those again.
 - Meles: Acquiring data is a stand alone step, nothing is tied into it. Next step link to it by id, so changing it is a matter of changing the id.

The user defines how data is acquired but this doesn't link it to a processing pipeline. 
Below is how this looks in the settings file. (see docs for further explanation)  
```xml
<streams>
    <!-- TCP client -->
    <stream id="uniquesensorid" type="tcp">
        <address>localhost:4004</address>
        <!-- Optional, these defaults are taken if not specified -->
		<eol>crlf</eol> <!-- The eol of a message being carriage return and line feed, determines buffering -->
        <ttl>-1</ttl> <!-- No ttl specified, normal format for example 5m (for 5 minutes) or 10s etc -->        
		<echo>no</echo> <!-- Don't echo the data back to the source -->
        <log>true</log> <!-- Log all the raw data -->
        <prefixorigin>no</prefixorigin> <!-- Prepend the id of the stream in front of raw data -->        
    </stream>
    <!-- Serial Port -->
    <stream id="differentsensorid" type="serial">
        <port>ttymxc0</port>
        <!-- all Optional ones mentioned earlier -->
        <serialsettings>19200,8,1,none</serialsettings> <!-- Baudrate,databits(5-8),stopbits(1-2),parity(even,odd,stick) -->
    </stream>
	<!-- And so on for the other sources -->
</streams>
```
  -> Saves implementation time and simplifies implementing redundancy.
 
 - Traditional dataloggers: Logging the raw data is a separate process that needs to be added or selected.
 - Meles: Logging raw data is the first step of the data acquisition process and it's opt out not opt in. Data format tab delimited 
with timestamp, source id and message as content. This uses a logging framework (tinylog), limits files to 100MB with daily rollover and zipping as default.
  -> A bug or a typo in a script isn't unrecoverable or require reversing processing by default, so you can't forget to enable it.

 - Traditional dataloggers: Monitoring sources is often not straightforward, based on physical port or just plain missing.
 - Meles: A telnet CLI session allows requesting the data of multiple sources concurrently (and multiple concurrent sessions are also possible).
 The command to request the data is the same as the text used to set it as a source for processing. 
 In addition, the telnet interface allows prepending:
  - Timestamp (both full date time and custom)
  - Id (to distinguish if multiple concurrent ones or just as reminder)
  - Elapsed time since previous message
  -> Follow multiple sensors in realtime for debugging a script or just checking how it responds to external stimuli.
  
 - Traditional dataloggers: Sending to sources is often not straightforward, based on physical port or just plain missing. 
 - Meles: Sending data to a device is done in telnet through 'sensorid:message',  the '!!' suffix can be used so the id doesn't have
to be typed every time. So 'sensorid:!!' followed by sending messages as is (just !! stops it).
  -> The ease of direct terminal access with the convenience of having backspace.
  
 - Traditional dataloggers: When a device is idle, this event is at best logged.
 - Meles: A connection created or lost or a device (no longer) idle is just another trigger source. 
  -> Allows triggering a user defined sequence that can be as simple as 'log an error' or as complex as 'switch to a redundant data source'.
  
**Data processing** 
 - Traditional dataloggers: Either dialog based or limited gui or proprietary language. Requiring proprietary tools. 
 - Meles: Full featured XML based scripting supports filtering, editing and advanced math operations. Works with any notepad.
   -> Removes the need to pay for custom sensor implementations and allows anyone to collaborate without needing a dedicated IDE.
   
 - Traditional dataloggers: No Dry run options for the data processing, requires the device attached.
 - Meles: Files are a valid data source and can thus replace a device, this includes playback at a custom rate and message size.
   -> Allows writing the processing script without the device, or reprocessing raw data if a calibration value had a typo.
   -> Allows bulk processing in two stages, first concurrently processing blocks of data but the output retains the same order.
This is followed by a sequential stage so storage and calcutaions match the order in which it's received.

 
 - Traditional dataloggers: Processing is the final step before storage or display.
 - Meles: Processing is an (optional) step in the total data pipeline thus just another source.
   -> Alter received data before sending it onwards, apply offsets or fix know bad values.
  
 
**Data output**
 - Traditional dataloggers: Once data is in, it doesn't go out again.
 - Meles: Allows protocol conversion between TCP, UDP, Serial, Matrix and to some extend email. The result of I²C communication can be forwarded as ascii.
   -> Act as a sniffing device between a sensor and the (proprietary) GUI. Or handle conversion so a legacy device can receive data from a network based one.
 
 - Traditional dataloggers: No custom outputs
 - Meles: Build a data stream based on realtime data combined with fixed constants and output this to any connected source that can accept ascii.
   -> Mimic a sensor or a custom data format to support picky devices.
 