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

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * @author Daniil Kochetov unixguide@narod.ru 
 * ivTAP_srv v2.6
 * - Added description of the tenants
 */

public class TenantsDescription {

	
	public static Map<String,String> loadProperties(String filename) throws Exception{
        Properties prop = new Properties();
        Map<String,String>map = new HashMap<String,String>();
        
        FileInputStream inputStream = new FileInputStream(filename);
        prop.load(inputStream);
        
        for (final Entry<Object, Object> entry : prop.entrySet()) {
            map.put((String) entry.getKey(), (String) entry.getValue());
        }
        inputStream.close();
        return map;
    }
}
