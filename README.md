# ivTAP
Intellectual virtual network tap
ivTAP - Copyright (C) 2017-2018  Daniil Kochetov.

This is a client/server suite to sniff network traffic at the remote hosts and deliver desired packets to a central monitoring probe device. 

## Project manifest
I'm not a professional programmer, at least I didn't own a cent working as a programmer for any project. I am an application performance analyst. One of the software I use to monitor performance of the applications takes source data from SPAN (or mirroring) port of the network switch where desired traffic is going. This approach works well if I have physical access to the network equipment and can connect switches to the probe device with patchcord, but it is not suitable if I need to monitor cloud-hosted applications. I created this software to provide cloud hosted applications with the same monitoring capabilities, which I have in the local data center.
- Client part of the software supposed to be launched at the operating system where we want to inspect network traffic. Using "winpcap" it captures network traffic filtering only required packets. Then it sends them to the server part over UDP channel. Doing this it constantly controls data flow, watching the UDP bandwidth consumption doesn't breach pre-defined threshold. This way I protect WAN link from being overloaded with reflected traffic.
- Server part of the software supposed to be launched at the probe device operating system. It extracts packets captured from single or multiple ivTAP clients and injects them to physical SPAN port like they would be captured naturally form physical SPAN port.
This way my application performance monitoring solution receives data from many distant hosts without placement of physical or virtual probe devise at every cloud or data center.
See details: https://apa-in-it.blogspot.com/2019/01/centralized-network-traffic-monitoring.html

## Usage

### Requirements and important considerations
- You must have administrative access rights to probe device operating system and to the hosts where you place the client part of ivTAP. Winpcap or libpcap must be installed at target hosts and at the probe device.
- You must disable the network offload feature at the machine where you deploy the client part of ivTAP. 
- If you are going to send mirrored UDP flow over WAN link, you should consider available bandwidth and define safe thresholds in the client configuration.
- High network jitter between client and server parts of the software might introduce a noticeable error.
- When you sniff traffic directly from SPAN port, it goes to receive (Rx) queue of the probe device network adapter. The server part of ivTAP can only put it into transmit (Tx) queue. Normally, probe device monitoring software doesn't care of it and takes all the traffic (at least it works well in my case). This might be an issue in the only case when your probe uses non-standard network driver. In this case, a simple workaround might help: internal tunnel from one logical GRE interface to another one should solve the issue.

### ivTAP Client settings
All the settings are stored in ivTAPclient.properties file.  
*sIntName=\\Device\\NPF_{XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX}* : This is a source device name. Full list of the devices can be displayed by ivTAP client software if you launch its main class with "-l" option. Or if you put wrong name of the device in the configuration file.  
*filterString=tcp port 3389* : This is a BPF filter sting. When you launch the program, winpcap/libpcap will be capturing only those packets, which matches this filter.  
*srvAddr=10.0.0.1* : IP address of the host where you run the server part of ivTAP software.  
*srvPort=20004* : UDP port of the ivTAP server, where you send captured packets.  
*speedLimit=10000000* : Maximal bandwidth which ivTAP client allowed to use when sending captured packets to ivTAP server over UDP channel. Must be specified in bits per second.  
*bandwidthCheckInterval=10* : Interval of bandwidth checks, specified in seconds.  
*bandwidthBreachIntervals=6* : Maximum number of sequential bandwidth check intervals with breached speedLimit. If bandwidth is always higher than allowed during *bandwidthBreachIntervals* in a row, ivTAP client will stop. This way we avoid unnecessary program terminations on rare bandwidth consumption spikes.  
*maxIdleIntervals=40* : network capturing process will restart itself if it won't capture any packet during *maxIdleIntervals*. Practical usage in third party cloud environments shown that winpcap/libpcap might silently become unable to capture network traffic after some kind of manipulations at operating system level or at virtual machine environment. This happens without any exception so I find no better workarround.  
*maxRestarts=3* : the program will terminate after *maxRestarts* number of restarts when no traffic is detected.  
*businessDaysNumbers=2,3,4,5,6* : there is no use to restart ivTAP client at the weekend even if it doesn't see any packet. Names of these days of the week can be verified in the ivTAP client log when it initiates (this is helpful if you are not sure what is the first day of the week number at the particular host).  

### ivTAP Server settings
All the settings are stored in ivTAP.properties file.  
*dIntName=eth0* : destination interface where ivTAP injects traffic.  
*bindAddress=127.0.0.1* : internal IP address where ivTAP binds its UDP channel.  
*bindPort=20004* : port number where ivTAP binds its UDP channel.  
*tenantsCheckInterval=10* : interval between ivTAP clients checks, specified in seconds.  
*smptRelayServer=10.0.0.2*: SMTP server for sending alert messages.  
*alertEmailRecipient=unixguide@narod.ru* : email addressee for alert messages.  
*controlMsgIdleTimeout=60* : if any existing client would stop sending control messages for *controlMsgIdleTimeout* seconds, ivTAP server will send an alert.  
*clientBytesIdleTimeout=300* : if any existing client would stop sending captured packets, keep sending control messages, ivTAP server will create warning record in log, but won't send alert message.



