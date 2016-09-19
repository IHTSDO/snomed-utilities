package org.ihtsdo.snomed.util;

public class SnomedUtilException extends Exception {

	private static final long serialVersionUID = 1L;

	public SnomedUtilException(String msg, Throwable t) {
		super(msg, t);
	}

	public SnomedUtilException(String msg) {
		super(msg);
	}
}
