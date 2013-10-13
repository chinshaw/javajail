package org.braindroppings.forkingjava;

import java.io.IOException;

public class BootstrapException extends Exception {

	/**
	 * Serialization ID
	 */
	private static final long serialVersionUID = 7874356791438786516L;

	public BootstrapException(String message) {
		super(message);
	}

	public BootstrapException(String message, IOException e) {
		super(message, e);
	}
}
