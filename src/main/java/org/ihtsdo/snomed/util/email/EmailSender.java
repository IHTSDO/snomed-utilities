package org.ihtsdo.snomed.util.email;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.ImageHtmlEmail;
import org.apache.commons.mail.resolver.DataSourceUrlResolver;

public class EmailSender {

	private final String smtpHost;
	private final int smtpPort;
	private final String smtpUsername;
	private final String smtpPassword;
	private final boolean smtpSsl;

	EmailSender(final String smtpHost, final int smtpPort, final String smtpUsername, final String smtpPassword, final boolean smtpSsl) {
		this.smtpHost = smtpHost;
		this.smtpPort = smtpPort;
		this.smtpUsername = smtpUsername;
		this.smtpPassword = smtpPassword;
		this.smtpSsl = smtpSsl;
	}

	public void send(EmailRequest request) throws EmailException, MalformedURLException {
		ImageHtmlEmail email = new ImageHtmlEmail();
		URL url = new URL(request.getBaseUrl());
		email.setDataSourceResolver(new DataSourceUrlResolver(url));

		email.setFrom(request.getFromEmail(), request.getFromName());
		email.setSubject(request.getSubject());
		email.addTo(request.getToEmail());
		email.setHtmlMsg(request.getHtmlBody());
		email.setTextMsg(request.getTextBody());

		addStandardDetails(email);
		email.send();
	}

	private void addStandardDetails(Email email) throws EmailException {
		email.setHostName(smtpHost);
		email.setSmtpPort(smtpPort);
		email.setAuthenticator(new DefaultAuthenticator(smtpUsername, smtpPassword));
		email.setSSLOnConnect(smtpSsl);
	}

}
