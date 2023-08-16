package org.ihtsdo.snomed.util;

import java.io.Serial;

public class SnomedUtilException extends Exception {

	@Serial
	private static final long serialVersionUID = 1L;

	public SnomedUtilException(String msg, Throwable t) {
		super(msg, t);
	}

	public SnomedUtilException(String msg) {
		super(msg);
	}
}
