package org.braindroppings.forkingjava;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ForkTimeoutException extends Exception {

	/**
	 * Serialization ID
	 */
	private static final long serialVersionUID = -5271706827946632901L;

	private int timtout;

	private TimeUnit unit;

	public ForkTimeoutException(String message) {
		super(message);
	}

	public ForkTimeoutException(String message, TimeoutException e) {
		super(message, e);
	}

	public ForkTimeoutException(String message, int timeout, TimeUnit unit) {
		super(message);
	}

	public int getTimtout() {
		return timtout;
	}

	public TimeUnit getUnit() {
		return unit;
	}
}