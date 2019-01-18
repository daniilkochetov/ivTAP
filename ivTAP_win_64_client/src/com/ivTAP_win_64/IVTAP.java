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
 *  ivTAP 2.9
 *  Check if pcap loop is already closed with PcapClosedException before breaking loop
 *  ivTAP 3.0
 *  low significance changes in comments and pcap loop restart procedure
 */


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jnetpcap.Pcap;
import org.jnetpcap.PcapAddr;
import org.jnetpcap.PcapBpfProgram;
import org.jnetpcap.PcapIf;
import org.jnetpcap.packet.PcapPacketHandler;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;



public class IVTAP {
	
	static String sIntName = "";
	static String filterString = "";
	static String srvAddr = "";
	static int srvPort = 2424;
	static int speedLimit = 1000000;
	static long bandwidthCheckInterval = 10;
	static int bandwidthBreachIntervals = 1;
	static boolean pcapRestartNeeded = false;
	static int maxIdleIntervals = 30;
	static int maxRestarts = 5;
	static String businessDays = "";
	
	static DatagramChannel udpChannel = null;
	static InetSocketAddress dstaddr = null;
	static Pcap pcapIn = null;
	static PcapBpfProgram bpf = null;
	static PcapPacketHandler<String> jpacketHandler = null;
	static BandwidthControl bControl = null;
	
	public static long bytesTransferred = 0L;

	static Logger log = Logger.getLogger(IVTAP.class.getName());
	
	public static synchronized long getBytesTransferred() {
		return bytesTransferred;
	}
	public static synchronized void setBytesTransferred(long bytesTransferred) {
		IVTAP.bytesTransferred = bytesTransferred;
	}
	
	private static void listDevices() {
		
		List<PcapIf> alldevs = new ArrayList<PcapIf>(); // Will be filled with NICs  
        StringBuilder errbuf = new StringBuilder(); // For any error msgs 
        int r = Pcap.findAllDevs(alldevs, errbuf);
		
        System.out.println("ivTAP configuration helper");
		System.out.println("Available interfaces:");
        
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
        System.out.println("Days of the week in current system language:");
        //list days of the week
        Calendar c = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE");
        String allWeekDays = "";
        
        c.setTimeInMillis(System.currentTimeMillis());
        for (int d = 0; d < 7; d++) {
        	allWeekDays = allWeekDays + dateFormat.format(c.getTime()) + ",";
        	c.add(Calendar.DAY_OF_MONTH, 1);
        }
        allWeekDays = allWeekDays.substring(0, allWeekDays.length()-1);
        System.out.printf("%s\n", allWeekDays);
    	return;
	}
	
	private static Pcap openPcapInstance() {
		/*************************************************************************** 
         * Open up the selected source device
         **************************************************************************/  
		Pcap pcap = null;
		StringBuilder errbuf = new StringBuilder(); // For any error msgs of libpcap
		int snaplen = 64 * 1024;           // Capture all packets, no trucation  
        int flags = Pcap.MODE_NON_PROMISCUOUS;  
        int timeout = 10;           // 10 ms is my educated guess
        
        pcap = Pcap.openLive(sIntName, snaplen, flags, timeout, errbuf);  
        if (pcap == null) {
        	log.severe("Error while opening source device: " + sIntName + errbuf.toString());
        	listDevices();
            return null;  
        }
        //Preparing and applying bpf filter at source interface
        if (filterString == "") {
        	log.severe("Filter is emply, it's too risky to run ivTAP without filter.");
            pcap.close();
           	return null;
        }
        bpf = new PcapBpfProgram();
        if (pcap.compile(bpf, filterString, 0, 0xFFFFFF00) == -1) {
        	log.severe(pcapIn.getErr());
            pcap.close();
            return null; 
        }
        if (pcap.setFilter(bpf) != Pcap.OK) {
        	log.severe(pcap.getErr());
        	Pcap.freecode(bpf);
            pcap.close();
        	return null;
        }
        return pcap;
	}
	public static void main(String[] args) {
		
		
		// Parse command line
		Settings settings = new Settings();
        CmdLineParser parser = new CmdLineParser(settings);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            log.log(Level.SEVERE, "Exception: ", e);
            parser.printUsage(System.out);
            return;
        }
        
        // Print a list of devices on this system and exit
        if (settings.listInterfaces) {
        	listDevices();
        	return;
        }

        // We are still here? - This is a production run, we need properties to continue
        ClientProperties clientProperties = new ClientProperties(); 
        try {
			clientProperties.loadProperties("ivTAP.properties");
		} catch (NumberFormatException | IOException e) {
			log.log(Level.SEVERE, "Exception: ", e);
		}
        
        log.info("Start capturing packets");
        log.info("Source interface: " + sIntName);
        log.info("Filter: " + filterString);
        log.info("Destination Server: " + srvAddr + ":" + srvPort);
        log.info("Bandwidth threshold: " + speedLimit + " bps");
        log.info("Idle timeout to restart capturing: " + maxIdleIntervals*bandwidthCheckInterval + " sec");
        log.info("Max restarts: " + maxRestarts);
        log.info("Business days set to: " + businessDays);
		
        /*************************************************************************** 
         * Create a packet handler which will receive packets from the libpcap loop. 
         **************************************************************************/
        jpacketHandler = new PHandler<String>("ivTAP");
        
        //Opening UDP channel to send captured packets    
        try {
        	udpChannel = DatagramChannel.open();
		} catch (IOException e) {
			log.log(Level.SEVERE, "Exception: ", e);
		}
        dstaddr = new InetSocketAddress(srvAddr, srvPort);
        
        /*************************************************************************** 
         * The pcap loop supposed to be endless.
         * We have to describe reaction on Ctrl+C or kill -1 
         **************************************************************************/
        
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run()
            {
            	pcapRestartNeeded = false;
            	log.info("Breaking loop");
            	System.out.println("Breaking loop");
            	pcapIn.breakloop();
            	log.info("Closing source device");
            	System.out.println("Closing source device");
                pcapIn.close();
            	log.info("Removing BPF filter");
            	System.out.println("Removing BPF filter");
            	Pcap.freecode(bpf);
                log.info("Stopping bandwidth control");
                System.out.println("Stopping bandwidth control");
                bControl.interrupt();
                log.info("Closing UDP channel");
                System.out.println("Closing UDP channel");
                try {
					udpChannel.close();
				} catch (IOException e) {
					log.log(Level.SEVERE, "Exception: ", e);
				}
            }
        });
        
        //Initializing bandwidth control thread
        bControl = new BandwidthControl();
        bControl.start();
        //starting endless capture loop
        pcapRestartNeeded = true;
        while (pcapRestartNeeded) {
        	log.info("################# Staring pcap loop! #################");
        	pcapRestartNeeded = false;
        	pcapIn = openPcapInstance();
        	if (pcapIn == null) {
        		log.severe("Couldn't create pcap instance");
            	return;
            }
        	pcapIn.loop(-1, jpacketHandler, "ivTAP");
        	log.info("################# pcap loop ended! #################");
        }
        // Rarely, infinite pcap loop can be ended with pcapRestartNeeded = false on NIC reset 
    	log.info("Closing source device");
        pcapIn.close();
    	log.info("Removing BPF filter");
    	Pcap.freecode(bpf);
        log.info("Stopping bandwidth control");
        bControl.interrupt();
        log.info("Closing UDP channel");
        try {
			udpChannel.close();
		} catch (IOException e) {
			log.log(Level.SEVERE, "Exception: ", e);
		}
	}

}
