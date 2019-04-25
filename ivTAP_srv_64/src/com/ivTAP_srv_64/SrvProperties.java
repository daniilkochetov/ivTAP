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
 * ivTAP_srv v2.0
 */

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class SrvProperties {
	static String dIntName = "";
	static String bindAddr = "0.0.0.0";
	static int bindPort = 2424;
	static int tenantsCheckInterval = 60;
	static String smptRelayServer = "";
	static String alertEmailRecipient = "";
	static int controlMsgIdleTimeout = 60;
	static int clientBytesIdleTimeout = 300;
	
	
	protected void loadProperties(String filename) throws FileNotFoundException, IOException, NumberFormatException {
		Properties prop = new Properties();
		InputStream input = null;
		
		//input = new FileInputStream("ivTAP.properties");
		input = new FileInputStream(filename);
		prop.load(input);
		dIntName = prop.getProperty("dIntName");
		bindAddr = prop.getProperty("bindAddress");
		bindPort = Integer.parseInt(prop.getProperty("bindPort"));
		tenantsCheckInterval = Integer.parseInt(prop.getProperty("tenantsCheckInterval"));
		smptRelayServer = prop.getProperty("smptRelayServer");
		alertEmailRecipient = prop.getProperty("alertEmailRecipient");
		controlMsgIdleTimeout = Integer.parseInt(prop.getProperty("controlMsgIdleTimeout"));
		clientBytesIdleTimeout = Integer.parseInt(prop.getProperty("clientBytesIdleTimeout"));
		
		input.close();
	}
}
