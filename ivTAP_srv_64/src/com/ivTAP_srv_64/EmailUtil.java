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

import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailUtil {
	private static Properties props;
	private static Session session;
	
	public EmailUtil(String smtpHostServer) throws IOException {
		props = System.getProperties();
		props.put("mail.smtp.host", smtpHostServer);
		session = Session.getInstance(props, null);
	}
	
	public void sendEmail(String toEmail, String subject, String body){
		try
	    {
	      MimeMessage msg = new MimeMessage(session);
	      //set message headers
	      msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
	      msg.addHeader("format", "flowed");
	      msg.addHeader("Content-Transfer-Encoding", "8bit");

	      msg.setFrom(new InternetAddress("no_reply@ivTAP.ivtap", "NoReply-ivTAP"));

	      msg.setReplyTo(InternetAddress.parse("no_reply@ivTAP.ivtap", false));

	      msg.setSubject(subject, "UTF-8");

	      msg.setText(body, "UTF-8");

	      msg.setSentDate(new Date());

	      msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
    	  Transport.send(msg);
    	  IVTAP_SRV.log.log(Level.INFO, "SMTP message sent succesfully to: "+toEmail+" with subject: "+subject);
	    }
	    catch (Exception e) {
	      IVTAP_SRV.log.log(Level.WARNING, "Exception when sending SMTP message to: "+toEmail+" with subject: "+subject, e);
	      e.printStackTrace();
	    }
	}
}
