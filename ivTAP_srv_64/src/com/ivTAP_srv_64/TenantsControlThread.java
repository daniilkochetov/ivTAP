/**
	This file is a part of intellectual virtual TAP device (ivTAP) server software. 
	It receives network packets captured by ivTAP clients and forwards them to the selected NIC. 
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
package com.ivTAP_srv_64;
/**
 * @author Daniil Kochetov unixguide@narod.ru 
 * ivTAP_srv v2.5
 * ivTAP_srv v2.6
 * - Added description of the tenants
 */

import java.util.logging.Level;

public class TenantsControlThread extends Thread{
	
	@Override
	public void run()
	{
		long tsTimeLong;
		String logStr = " ";
		
		while(true)
		{
			logStr = "Current tenants: ";
			if (!IVTAP_SRV.tenants.isEmpty()) {
				for (Tenant t : IVTAP_SRV.tenants) {
													
					logStr = logStr + t.getClientAddress() + ";"; 
										
					tsTimeLong = System.currentTimeMillis();
					if (tsTimeLong - (t.getLastTimestamp() + t.getCurrentLatency()) >= SrvProperties.controlMsgIdleTimeout * 1000) {
						//Tenant stopped sending control packets
						IVTAP_SRV.removeTenant(t.getClientAddress(),-3);
					}
					if (!t.isIdle() && (tsTimeLong - (t.getLastTimestampWithData() + t.getCurrentLatency()) >= SrvProperties.clientBytesIdleTimeout * 1000)) {
						IVTAP_SRV.log.log(Level.WARNING, "The client with IP " + t.getClientAddress() + " (" +t.getTenantDescr() + ")" + 
								" stopped sending packets with data, but still sends control packets");
						t.setIdle(true);
					}
					IVTAP_SRV.log.log(Level.INFO,
							String.format("Client %s (%s); Bytes counter: %d; Control messages counter %d; Time stamp: %d ms; Latency %d ms; Error %d ms; Max Error %d ms ",
									t.getClientAddress(),t.getTenantDescr(), t.getBytesReceived(), t.getControlMessagesReceived(), 
									t.getLastTimestamp(), t.getCurrentLatency(), t.getCurrentError(), t.getMaxError()));
				}
			}
			IVTAP_SRV.log.info(logStr);
			try{
				sleep(SrvProperties.tenantsCheckInterval * 1000);
			}catch(InterruptedException e){
				IVTAP_SRV.log.log(Level.SEVERE, "Exception: ", e);
			}
		}
	}
}
