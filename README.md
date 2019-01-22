# ivTAP
Intellectual virtual network tap
ivTAP - Copyright (C) 2017-2018  Daniil Kochetov.

This is a client/server suite to sniff network traffic at the remote hosts and deliver desired packets to a central monitring probe device. 

## Project manifest
I'm not a professional programmer, at least I didn't own a cent working as a programmer for any project. I am an applicaiton performance analyst. One of the software I use to monitor performance of the applications takes source data from SPAN (or mirroring) port of the network switch where desired traffic is going. This approach works good if I have phisical access to the network equipment and can connect switches to the probe device with patchcord, but it is not suitable if I need to monitor cloud hosted applicaiotns. I created this software to provide cloud hosted applciaionts with the same monitring capabilities which I have in the local data center.
- Client part of the software supposed to be launced at the operating system where we want to inspect network traffic. Using "winpcap" it captures network traffic filtering only required packets. Then it sends them to the server part over UDP channel. Doing this it constantly controls data flow, watching the UDP bandwidth consuption doesn't breach pre-defined threshold. This way I protect WAN link from being overloaded with reflected traffic.
- Server part of the software extracts packets captured from single or multiple ivTAP clients and injects them to phisical SPAN port like they would be captured naturally form phisical SPAN port.
This way my applciaiotn performance monitirng solution receives data from many disnant hosts without placement of phisical or virtual probe devise at every cloud or data center.

