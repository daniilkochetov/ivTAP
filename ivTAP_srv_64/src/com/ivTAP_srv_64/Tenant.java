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

public class Tenant {
	String clientAddress = "0.0.0.0";
	protected long bytesReceived = 0;
	protected long controlMessagesReceived = 0;
	protected long lastTimestamp = 0;
	protected long lastTimestampWithData = 0;
	protected long currentError = 0;
	protected long currentLatency = 0;
	protected long maxError = 0;
	boolean isIdle = false;
	String tenantDescr = "";
	
	public String getTenantDescr() {
		return tenantDescr;
	}
	public void setTenantDescr(String tenantDescr) {
		if (tenantDescr != null) {
			this.tenantDescr = tenantDescr;
		} else {
			this.tenantDescr = "unknown tenant";
		}
	}
	public boolean isIdle() {
		return isIdle;
	}
	public void setIdle(boolean isIdle) {
		this.isIdle = isIdle;
	}
	public long getMaxError() {
		return maxError;
	}
	public void setMaxError(long maxError) {
		this.maxError = maxError;
	}
	public long getCurrentLatency() {
		return currentLatency;
	}
	public void setCurrentLatency(long currentLatency) {
		this.currentLatency = currentLatency;
	}
	public long getControlMessagesReceived() {
		return controlMessagesReceived;
	}
	public void setControlMessagesReceived(long controlMessagesReceived) {
		this.controlMessagesReceived = controlMessagesReceived;
	}
	protected String getClientAddress() {
		return clientAddress;
	}
	protected void setClientAddress(String clientAddress) {
		this.clientAddress = clientAddress;
	}
	protected long getBytesReceived() {
		return bytesReceived;
	}
	protected void setBytesReceived(long bytesReceived) {
		this.bytesReceived = bytesReceived;
	}
	protected long getLastTimestamp() {
		return lastTimestamp;
	}
	protected void setLastTimestamp(long lastTimestamp) {
		this.lastTimestamp = lastTimestamp;
	}
	protected long getLastTimestampWithData() {
		return lastTimestampWithData;
	}
	protected void setLastTimestampWithData(long lastTimestampWithData) {
		this.lastTimestampWithData = lastTimestampWithData;
	}
	protected long getCurrentError() {
		return currentError;
	}
	protected void setCurrentError(long l) {
		this.currentError = l;
	}

}
