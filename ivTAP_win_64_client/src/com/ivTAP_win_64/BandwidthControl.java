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
 *  ivTAP v2.1
 *  added time consistency control
 *  changed the the point of sendControlMessage invocation
 *  ivTAP 2.9
 *  Check if pcap loop is already closed with PcapClosedException before breaking loop
 *  ivTAP 3.0
 *  low significance changes in comments and pcap loop restart procedure
 */

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jnetpcap.Pcap;
import org.jnetpcap.PcapClosedException;


public class BandwidthControl extends Thread{
	private static long bytesTransferredPrev = 0;
	private static long speed = 0;
	private static int bandwidthBreachedIntervals = 0;
	private static Logger log = Logger.getLogger(BandwidthControl.class.getName());
	private static byte[] controlMsgPreamble = { 0x00, 0x00, 0x00, 0x00, 0x00 };
	private ByteBuffer controlMsg;
	
	
	public void sendControlMessage(byte errorCode) {
		long tsTimeLong;
		/*
		 * Control message format (22 bytes)
		 * - controlMsgPreamble - first 5 bytes of 0x00, this helps IVTAP_SRV to separate control message from captured packets
		 * - errorCode - one byte of error code:
		 *   - 0 everything is all right
		 *   - 1 single breach of bandwidth threshold
		 *   - 2 client restarts due to idle timeout breached
		 *   - -1 client stops due to many simultaneous bandwidth threshold breaches
		 *   - -2 client stops due to maximum number of restarts in a row breached
		 * - current timestamp at the client (8 bytes)
		 * - bytes transferred since the client started (8 bytes)  
		 */
		
		controlMsg.put(controlMsgPreamble, 0, 5); //control message marker
		controlMsg.put(5,errorCode);
		tsTimeLong = System.currentTimeMillis();
		controlMsg.putLong(6, tsTimeLong); //timestamp at the client side
		controlMsg.putLong(14, IVTAP.getBytesTransferred()); // add bytes sent
		controlMsg.rewind();
		//System.out.println(javax.xml.bind.DatatypeConverter.printHexBinary(controlMsg.array()));
		controlMsg.rewind();
		//sending a control message to the server
		try {
			IVTAP.udpChannel.send(controlMsg, IVTAP.dstaddr);
			controlMsg.rewind();
		} catch (IOException e) {
			log.log(Level.SEVERE, "Exception: ", e);
		}
	}
	
	@Override
	public void run()
	{
		long idleIntervals = 0;
		int restartsCount = 0;
		byte controlMessageCode;
		Calendar calendar=Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE");
		String dayOfWeek;
		
		controlMsg = ByteBuffer.allocate(6+Long.BYTES*2);
		
		while(true)
		{
			controlMessageCode = (byte) 0;
			//introducing pause between bandwidth verifications 
			try{
				sleep(IVTAP.bandwidthCheckInterval * 1000);
			}catch(InterruptedException e){
				log.log(Level.SEVERE, "Exception: ", e);
			}

			
			//Check idle time on business days only
			calendar.setTimeInMillis(System.currentTimeMillis());
			dayOfWeek = dateFormat.format(calendar.getTime());
			if (bytesTransferredPrev == IVTAP.getBytesTransferred() && 
					IVTAP.businessDays.toLowerCase().contains(dayOfWeek.toLowerCase())) { 
				//no bytes captured for the last interval and this is a business day
				idleIntervals++;
				if (idleIntervals > IVTAP.maxIdleIntervals) {
					restartsCount++;
					if (restartsCount > IVTAP.maxRestarts) {
						log.severe("Idle timeout breached. No more restarts allowed.");
						controlMessageCode = (byte) -2;						
					} else {
						log.warning("Idle timeout breached " + String.valueOf(idleIntervals) + " time(s) in a row. More than allowed by the threshold");
						controlMessageCode = (byte) 2;
						idleIntervals = 0;
					}
				}
			} else {
				idleIntervals = 0;
				restartsCount = 0;
			}
			//Calculating speed and writing a log string about the current status of capturing
			if (bytesTransferredPrev <= IVTAP.getBytesTransferred()) {
				speed = (IVTAP.getBytesTransferred() - bytesTransferredPrev) * 8L / IVTAP.bandwidthCheckInterval; 
				if (idleIntervals == 0) {
					log.info("Bytes transferred: " + String.valueOf(IVTAP.getBytesTransferred()) +
							" at the speed of: " + String.valueOf(speed) + "bps");	
				} else {
					log.info("Bytes transferred: " + String.valueOf(IVTAP.getBytesTransferred()) +
							" at the speed of: " + String.valueOf(speed) + "bps. Idle time detected " + 
								String.valueOf(idleIntervals) + " time(s) in a row during the business day " + dayOfWeek + " [" +
								String.valueOf(calendar.get(Calendar.DAY_OF_WEEK)) + "]");
				}
				
			} else {
				log.info("bytesTransferred: " + String.valueOf(IVTAP.getBytesTransferred()) + " bytesTransferredPrev: " + bytesTransferredPrev);
			}
			bytesTransferredPrev = IVTAP.getBytesTransferred();
			
			//Check if we breach allowed bandwidth 
			if (speed >= IVTAP.speedLimit) {
				bandwidthBreachedIntervals++;
				if (bandwidthBreachedIntervals >= IVTAP.bandwidthBreachIntervals) {
					log.severe("Stop here, bandwidth limit " + IVTAP.speedLimit + 
							"bps breached " + bandwidthBreachedIntervals +
							" times in a row");
					controlMessageCode = (byte) -1;
				} else {
					log.warning("Bandwidth limit " + IVTAP.speedLimit + 
							"bps breached " + bandwidthBreachedIntervals +
							" times in a row. " + IVTAP.bandwidthBreachIntervals +
							" breaches in a row allowed");
					controlMessageCode = (byte) 1;
				}
			} else {
				bandwidthBreachedIntervals = 0;
			}
			sendControlMessage(controlMessageCode);
			
			switch (controlMessageCode) {
				case (byte) -1: case (byte) -2 : //We either breached bandwidth or idle timeouts maximum times in a row
					log.severe("The program terminated");  
					IVTAP.pcapRestartNeeded = false;
					try {
						log.info("Breaking loop");
	            		System.out.println("Breaking loop");
						IVTAP.pcapIn.breakloop();
					}catch(PcapClosedException e){
						log.log(Level.SEVERE, "Exception: ", e);
					}
					return;
				case (byte) 2 : //Restarting pcap loop due to suspiciously log period of silence 
					IVTAP.pcapRestartNeeded = true;
					log.warning("The pcap loop to be restarted " + String.valueOf(restartsCount) + " time(s) in a row due to idle timeout breached");
					try {
						log.info("Breaking loop");
	            		System.out.println("Breaking loop");
						IVTAP.pcapIn.breakloop();
					}catch(PcapClosedException e){
						log.log(Level.SEVERE, "Exception: ", e);
					}
					Pcap.freecode(IVTAP.bpf); 
					try {
						log.info("Closing source device");
		            	System.out.println("Closing source device");
						IVTAP.pcapIn.close();
					}catch(PcapClosedException e){
						log.log(Level.SEVERE, "Exception: ", e);
					}
					break;
			}
		}
	}
}
