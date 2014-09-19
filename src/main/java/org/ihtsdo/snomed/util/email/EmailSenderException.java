package org.ihtsdo.snomed.util.email;

@SuppressWarnings("serial")
public class EmailSenderException extends Exception {

	public EmailSenderException(String message, Throwable cause) {
		super(message, cause);
	}

	public EmailSenderException(String message) {
		super(message);
	}

	public EmailSenderException(Throwable cause) {
		super(cause);
	}
	
}
