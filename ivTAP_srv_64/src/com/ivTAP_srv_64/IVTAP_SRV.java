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
 * - Reads "tenant restarts due to idle timeout breached" and "tenant stops due to maximum number of restarts in a row breached" control messages and performs respective actions
 * ivTAP_srv v2.6
 * - Added description of the tenants 
 * - better logic for tenants update
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jnetpcap.Pcap;
import org.jnetpcap.PcapAddr;
import org.jnetpcap.PcapIf;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import com.ivTAP_srv_64.ByteUtils;
import com.ivTAP_srv_64.Settings;
import com.ivTAP_srv_64.SrvProperties;
import com.ivTAP_srv_64.Tenant;;

public class IVTAP_SRV {
	
	static final int CONTROLMESSAGELENGHT = 22;

	static Logger log = Logger.getLogger(IVTAP_SRV.class.getName());
	
	static CopyOnWriteArrayList<Tenant> tenants = new CopyOnWriteArrayList<Tenant>();
	static Map<String, String> tenantDescrMap = new HashMap<String, String>();
	static TenantsControlThread tControl = null;
	static EmailUtil mailUtil = null;
	
	private static byte[] controlMsgMarker = new byte[5];
	private static byte[] controlMsgPreamble = { 0x00, 0x00, 0x00, 0x00, 0x00 };
	private static long tsTimeLong, bytesReceived;
	
	private static void listDevices() {
		
		List<PcapIf> alldevs = new ArrayList<PcapIf>(); // Will be filled with NICs  
        StringBuilder errbuf = new StringBuilder(); // For any error msgs 
        int r = Pcap.findAllDevs(alldevs, errbuf); 
		System.out.println("List available interfaces and exit");
        
        if (r == -1 || alldevs.isEmpty()) {  
            System.err.printf("Can't read list of devices, error is %s", errbuf  
                .toString());  
            return;  
        }
        
        Iterator<PcapIf> itrdev = alldevs.iterator();
        while(itrdev.hasNext()) {
        	PcapIf device = (PcapIf)itrdev.next();
        	StringBuilder sb = new StringBuilder();
        	
        	sb.append(device.getName());
        	sb.append(";");
        	sb.append((device.getDescription() != null) ? device.getDescription() : "No description available");
        	sb.append("; MAC address:");
            try {
				if (device.getHardwareAddress() != null) {
					byte[] mac = device.getHardwareAddress();
					for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
					}
				} else {
					sb.append("No MAC address");
				}
			} catch (IOException e) {
				System.err.printf("Can't read MAC address");
			}
            sb.append("; IP address(es):");
            List<PcapAddr>  addrs = device.getAddresses();
            Iterator<PcapAddr> itraddr = addrs.iterator();
            while(itraddr.hasNext()) {
            	PcapAddr pcapAddr = (PcapAddr)itraddr.next();
            	sb.append(pcapAddr.getAddr().toString());
            }
            System.out.printf("%s\n", sb.toString());
        }      	
    	return;
	}
	
	private static int createNewTenantRecord(DatagramPacket packet) { //returns -1 if error, 0 if successful
		
		Tenant nt = new Tenant();
		nt.setClientAddress(packet.getAddress().getHostAddress());
		try {
			bytesReceived = ByteUtils.bytesToLong(packet.getData(),14);
		} catch (IOException e) {
			log.log(Level.WARNING, "Exception when reading bytes counter from control message from tenant IP" + packet.getAddress().getHostAddress());
			log.log(Level.WARNING, "Exception: ", e);
			return -1;
		}
		nt.setTenantDescr(tenantDescrMap.get(nt.getClientAddress()));
		nt.setBytesReceived(bytesReceived);
		nt.setControlMessagesReceived(1);
		try {
			tsTimeLong = ByteUtils.bytesToLong(packet.getData(),6);
		} catch (IOException e) {
			log.log(Level.WARNING, "Exception when reading last timestamp from control message from tenant IP" + packet.getAddress().getHostAddress());
			log.log(Level.WARNING, "Exception: ", e);
			return -1;
		}
		nt.setLastTimestamp(tsTimeLong);
		if (bytesReceived > 0) {
			nt.setLastTimestampWithData(nt.getLastTimestamp());
		}
		tsTimeLong = System.currentTimeMillis();
		nt.setCurrentLatency(Math.abs(tsTimeLong - nt.getLastTimestamp()));
		nt.setCurrentError(0);
		nt.setMaxError(0);
		nt.setIdle(false);
		
		tenants.add(nt);
		log.log(Level.INFO,String.format("Control message received from new client %s (%s); Bytes counter: %d; Time stamp: %d ms; Latency %d ms; Error %d ms ",
				nt.getClientAddress(),nt.getTenantDescr(),nt.getBytesReceived(),nt.getLastTimestamp(), nt.getCurrentLatency(), nt.getCurrentError()));
		mailUtil.sendEmail(SrvProperties.alertEmailRecipient, "ivTAP: new tenant detected " + nt.getClientAddress() +  " (" +nt.getTenantDescr() + ")", 
				"New tenant detected at IP: " + nt.getClientAddress() +  " (" +nt.getTenantDescr() + ")");
 
		return 0;
	}
	
	private static int updateExistingTenantRecord(DatagramPacket packet, Tenant t) { //returns -1 if error, 0 if successful
		
		try {
			tsTimeLong = ByteUtils.bytesToLong(packet.getData(),6);
		} catch (IOException e) {
			log.log(Level.WARNING, "Exception when reading last timestamp from control message");
			log.log(Level.WARNING, "Exception: ", e);
			return -1;
		}
		t.setLastTimestamp(tsTimeLong);
		tsTimeLong = System.currentTimeMillis();
		t.setCurrentError(Math.abs(t.getCurrentLatency() - Math.abs(tsTimeLong - t.getLastTimestamp())));
		t.setCurrentLatency(Math.abs(tsTimeLong - t.getLastTimestamp()));
		if (t.getMaxError() < t.currentError) {
			t.setMaxError(t.currentError);
		}
		
		try {
			bytesReceived = ByteUtils.bytesToLong(packet.getData(),14);
		} catch (IOException e) {
			log.log(Level.WARNING, "Exception when reading bytes counter from control message");
			log.log(Level.WARNING, "Exception: ", e);
			return -1;
		}
		if (bytesReceived > t.getBytesReceived()) {
			t.setLastTimestampWithData(t.getLastTimestamp());
			t.setIdle(false);
		}
		t.setControlMessagesReceived(t.getControlMessagesReceived()+1);
		t.setBytesReceived(bytesReceived);
		return 0;
	}

	static void removeTenant(String tAddress, int reason) {
		for (Tenant t : tenants) {
			if(t.getClientAddress().equals(tAddress)) {
				switch (reason) {
				case -1:
					log.log(Level.WARNING,"Tenant at IP: " + t.getClientAddress() + " (" +t.getTenantDescr() + ")" + " stopped due to many bandwidth threshold breaches in a row");
					mailUtil.sendEmail(SrvProperties.alertEmailRecipient, "ivTAP: tenant stopped "+ t.getClientAddress() + " (" +t.getTenantDescr() + ")", "Tenant at IP:" + t.getClientAddress() + " (" +t.getTenantDescr() + ")" 
							+ " stopped due to many bandwidth threshold breachesin a row");
					break;
				case -2: //the tenant stopped due to maximum number of restarts in a row breached
					log.log(Level.WARNING,"Tenant at IP: " + t.getClientAddress() + " (" +t.getTenantDescr() + ")" + " stopped due to maximum number of restarts in a row breached");
					mailUtil.sendEmail(SrvProperties.alertEmailRecipient, "ivTAP: tenant stopped " + t.getClientAddress() + " (" +t.getTenantDescr() + ")", "Tenant at IP:" + t.getClientAddress() + " (" +t.getTenantDescr() + ")" 
							+ " stopped due to maximum number of restarts in a row breached");
					break;
				case -3:
					log.log(Level.WARNING, "The client with IP " + t.getClientAddress() + " stopped sending control packets");
					mailUtil.sendEmail(SrvProperties.alertEmailRecipient, "ivTAP: tenant stopped "+ t.getClientAddress() + " (" +t.getTenantDescr() + ")", "Tenant at IP:" + t.getClientAddress() + " (" +t.getTenantDescr() + ")"
							+ " stopped sending control packets");
					break;
				default: break;
				}
					
				tenants.remove(t);
				return;
			}
		}
		return;
	}
	
private static int updateTenants(DatagramPacket packet) { // returns -1 if control packet is malformed or 0 if successful
	
		System.arraycopy(packet.getData(),0,controlMsgMarker,0,5);
		
		if (Arrays.equals(controlMsgMarker, controlMsgPreamble)) {
			
			switch(packet.getData()[5]) {
			case 1: //bandwidth threshold breached
				log.log(Level.WARNING,"Tenant at IP:" + packet.getAddress().getHostAddress() + " breached bandwidth threshold");
				break;
			case 2: //tenant restarts due to idle timeout breached
				log.log(Level.WARNING,"Tenant at IP:" + packet.getAddress().getHostAddress() + " restarts due to idle timeout breached");
				break;
			case -1: //the tenant stopped due to bandwidth threshold breaches
				removeTenant(packet.getAddress().getHostAddress(),-1);
				break;
			case -2: //the tenant stopped due to maximum number of restarts in a row breached
				removeTenant(packet.getAddress().getHostAddress(),-2);
				break;
			default: break;
			}
			
			for (Tenant t : tenants) {
				if(t.getClientAddress().equals(packet.getAddress().getHostAddress())) {
					// control packet came from known tenant
					return updateExistingTenantRecord(packet, t);
				}
			}
			// new tenant
			return createNewTenantRecord(packet);
		}
		return 0;
	}
	
	
	
	public static void main(String[] args) {
   		
		StringBuilder errbuf = new StringBuilder(); // For any error messages
		
		tenants.clear();
		
		// Parse command line
		Settings settings = new Settings();
		CmdLineParser parser = new CmdLineParser(settings);
				
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			log.log(Level.SEVERE,"Cannot parse comand line arguments: ", e);
		    System.err.println("e = " + e.toString());
		    parser.printUsage(System.out);
		    return;
		}
		        
		// Print a list of devices on this system and exit
		if (settings.listInterfaces) {
			log.log(Level.INFO,"List device and exit");
		   	listDevices();
		   	return;
		}
		
		// We are still here? - This is a production run, we need properties to continue
		System.out.println("Initializing...");
		log.log(Level.INFO,"Initializing...");
		SrvProperties srvProperties = new SrvProperties(); 
		try {
			srvProperties.loadProperties("ivTAP.properties");
		} catch (NumberFormatException | IOException e) {
			log.log(Level.SEVERE,"Malformed configuration file: ",e);
			e.printStackTrace();
			return;
		}
		//Loading tenants descriptions
		@SuppressWarnings("unused")
		TenantsDescription tenantsDescription = new TenantsDescription();
		try
        {
			tenantDescrMap = TenantsDescription.loadProperties("tenantsDescription.properties");
        }
        catch (Exception e) {
        	log.log(Level.WARNING,"Cannot load tenants description properties file: ",e);
            e.printStackTrace();
            System.out.println("Cannot load tenants description properties file: " + e.getMessage());
        }
		//tenantsCheckInterval = SrvProperties.tenantsCheckInterval;
		log.log(Level.INFO,"Configuration loaded from ivTAP.properties file");
		log.log(Level.INFO,"Tenants check interval is " + String.valueOf(SrvProperties.tenantsCheckInterval));
		log.log(Level.INFO,"Idle timeout for control message is " + String.valueOf(SrvProperties.controlMsgIdleTimeout));
		log.log(Level.INFO,"Idle timeout for clinet traffic is " + String.valueOf(SrvProperties.clientBytesIdleTimeout));

		//Initialize SMTP features
		try {
			mailUtil = new EmailUtil(SrvProperties.smptRelayServer); 
		} catch (Exception e) {
			log.log(Level.SEVERE, "Exception when initializing EmailUtil:", e);
			e.printStackTrace();
			return;
		}
		log.log(Level.INFO,"Mail features initialized with SMTP server " + SrvProperties.smptRelayServer);
		log.log(Level.INFO,"Alert addressee is: " + SrvProperties.alertEmailRecipient);
		
		InetSocketAddress bindsocketaddr = null;
		try {
			bindsocketaddr = new InetSocketAddress(SrvProperties.bindAddr, SrvProperties.bindPort);
		} catch (IllegalArgumentException | SecurityException e) {
			log.log(Level.SEVERE,"Cannot bind UDP socket address: ",e);
			e.printStackTrace();
		}
		
    	int snaplen = 64 * 1024;           // Capture all packets, no truncation  
        int flags = Pcap.MODE_PROMISCUOUS; // capture all packets  
        int timeout = 10;           // 10 ms  - just an educated guess
        Pcap pcapOut = Pcap.openLive(SrvProperties.dIntName, snaplen, flags, timeout, errbuf);  
        if (pcapOut == null) {  
            System.err.printf("Error while opening destination device: " + SrvProperties.dIntName + errbuf.toString());  
            log.log(Level.SEVERE,"Error while opening destination device: " + SrvProperties.dIntName + errbuf.toString());
            return;  
        }
        log.log(Level.INFO,"Destination interface " + SrvProperties.dIntName + " opened");
        
		try {
			DatagramSocket listener = new DatagramSocket(bindsocketaddr);
			log.log(Level.INFO,"UDP channel opened at: " + SrvProperties.bindAddr + ":" + String.valueOf(SrvProperties.bindPort));
			try {
				DatagramPacket udppacket = null;
		        tControl = new TenantsControlThread();
		        tControl.start();
		        log.log(Level.INFO,"Tenant control thread started");
				log.log(Level.INFO,"Initialization complete");
				System.out.println("waitnig for packets...");
				
				/*************************************************************************** 
		         * Main loop of the process is endless.
		         * We have to describe reaction on Ctrl+C or kill -1 
		         **************************************************************************/
				Runtime.getRuntime().addShutdownHook(new Thread(){
		            @Override
		            public void run()
		            {
		            	log.info("Closing destination device and UDP socket");
		                pcapOut.close();
	                	listener.close();
		                tControl.interrupt();
		                System.out.println("ivTAP server processes stopped, resourses deallocated");
		                log.log(Level.INFO,"ivTAP server processes stopped, resourses deallocated");
		            }
		        });
				byte[] message = new byte[65536];
				udppacket = new DatagramPacket(message, message.length);
				while (true) {
					try {
						listener.receive(udppacket);
					} catch (IOException e) {
						log.log(Level.WARNING,"Malformed UDP packet received",e);
					}
					//Check if it is tenant control packet
					if (udppacket.getLength() == CONTROLMESSAGELENGHT) {
						if (updateTenants(udppacket) == -1) {
							log.log(Level.WARNING,"Malformed control packet detected from " + udppacket.getAddress().getHostAddress());
						}
					} else {
						if (pcapOut.sendPacket(udppacket.getData(),0,udppacket.getLength()) == -1) {
							log.log(Level.WARNING,"Can't forward a packet from " + udppacket.getAddress().getHostAddress() + " incapsulating it in UPD packet of " 
									+ String.valueOf(udppacket.getLength()) + " bytes. The reason is: " + pcapOut.getErr());
						}
					}
				}
			} finally {
				listener.close();
			}
		} catch (SocketException e) {
			log.log(Level.SEVERE,"Cannot open UDP channel for listening: ",e);
			e.printStackTrace();
		}
	}

}
