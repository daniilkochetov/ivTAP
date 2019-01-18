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

import org.kohsuke.args4j.Option;

public class Settings {
	@Option(name = "-l", usage="List available interfaces and exit")
	public boolean listInterfaces;
}
