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
 * ivTAP v2.0
 */

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

public class ClientProperties {
	
	protected void loadProperties(String filename) throws FileNotFoundException, IOException, NumberFormatException {
		Properties prop = new Properties();
		InputStream input = null;
		int dayNumber;
		Calendar calendar=Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE");
		List<String> businessDaysList = null;
		
		input = new FileInputStream("ivTAPclient.properties");
		prop.load(input);
		IVTAP.sIntName = prop.getProperty("sIntName");
		IVTAP.filterString = prop.getProperty("filterString");
		IVTAP.srvAddr = prop.getProperty("srvAddr");
		IVTAP.srvPort = Integer.parseInt(prop.getProperty("srvPort"));
		IVTAP.speedLimit = Integer.parseInt(prop.getProperty("speedLimit"));
		IVTAP.bandwidthCheckInterval = Integer.parseInt(prop.getProperty("bandwidthCheckInterval"));
		IVTAP.bandwidthBreachIntervals = Integer.parseInt(prop.getProperty("bandwidthBreachIntervals"));
		IVTAP.maxIdleIntervals = Integer.parseInt(prop.getProperty("maxIdleIntervals"));
		IVTAP.maxRestarts = Integer.parseInt(prop.getProperty("maxRestarts"));
		if(prop.getProperty("businessDaysNumbers") != null) {
			businessDaysList = new ArrayList<String>(Arrays.asList(prop.getProperty("businessDaysNumbers").split(",")));
		} else {
			businessDaysList = new ArrayList<String>(Arrays.asList("2", "3", "4", "5", "6"));
		}
		IVTAP.businessDays = "";
		for(int i=0;i<businessDaysList.size();i++)
		{
			dayNumber = 10;
			try {
				dayNumber = Integer.valueOf(businessDaysList.get(i));
			} catch(NumberFormatException e){
				IVTAP.log.log(Level.SEVERE, "Exception: ", e);
			}
			if (dayNumber >= 1 && dayNumber <= 7) {
				calendar.setWeekDate(2018, 8, dayNumber);
				IVTAP.businessDays = IVTAP.businessDays + dateFormat.format(calendar.getTime())+",";
			}
		}
		if (IVTAP.businessDays.length() > 0 && IVTAP.businessDays.charAt(IVTAP.businessDays.length() - 1) == ',') {
			IVTAP.businessDays = IVTAP.businessDays.substring(0, IVTAP.businessDays.length() - 1);
	    }
		
		input.close();
	}
}
