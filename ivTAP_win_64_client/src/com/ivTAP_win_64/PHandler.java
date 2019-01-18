/**
	This file is a part of intellectual virtual TAP device (ivTAP) client software. 
	It captures desired network traffic from the selected NIC and forwards it to the ivTAP server over UDP channel. 
    Copyright (C) 2017-2018  Daniil Kochetov

    ivTAP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
    
    The software uses the following open source libraries
    - jNetPcap Package: Copyright (C) 2007-2010 Sly Technologies, Inc. Distributed under GNU LESSER GENERAL PUBLIC LICENSE
    - args4j: Copyright (c) 2013 Kohsuke Kawaguchi Distributed under MIT License
 */

package com.ivTAP_win_64;

/**
 * @author Daniil Kochetov unixguide@narod.ru
 * ivTAP 2.9
 *  Check if pcap loop is already closed with PcapClosedException before breaking loop
 */

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;
import org.jnetpcap.packet.format.FormatUtils;
import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.tcpip.Tcp;

public class PHandler<T> implements PcapPacketHandler<String> {
	//Avoid excessive instantiations within endless loop 
			private Tcp tcp = new Tcp(); // Preallocate a Tcp header 
			private Ip4 ip = new Ip4(); // Preallocate a IP header
			
			private int size;
			@SuppressWarnings("unused")
			private T user;
			private static Logger log = Logger.getLogger(PHandler.class.getName());

			public PHandler(T user) {
				this.setUser(user);
			}
			public void setUser(T user) {
				this.user = user;
			}

			@Override
			public void nextPacket(PcapPacket packet, String user) {
				if (packet.hasHeader(ip) && packet.hasHeader(tcp)) {
					if (log.isLoggable(Level.FINE)) {
						log.fine("Received packet len=" + String.valueOf(packet.getCaptureHeader().wirelen()) +
								" source_IP=" + FormatUtils.ip(ip.source()) +
								" source_port=" + String.valueOf(tcp.source()) +
								" destination_IP=" + FormatUtils.ip(ip.destination()) + 
								" destination_port=" + String.valueOf(tcp.destination()));  
					}
	        		//preparing to send
	        		size = packet.size();
	        		ByteBuffer byteBuffer = ByteBuffer.allocate(size);
	        		packet.transferTo(byteBuffer); 
	        		IVTAP.setBytesTransferred(IVTAP.getBytesTransferred() + size);
	        		if (IVTAP.getBytesTransferred() >= 8000000000000000000L) {
	        			IVTAP.setBytesTransferred(0L);
	        		}
	            	//sending UDP	
	        		byteBuffer.flip();
	        		try {
						IVTAP.udpChannel.send(byteBuffer, IVTAP.dstaddr);
					} catch (IOException e) {
						log.log(Level.SEVERE, "Exception: ", e);
					}
				}
			}
}
