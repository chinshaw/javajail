package org.braindroppings.forkingjava;

public class ForkException extends Exception {


	/**
     * Serialization ID.
     */
	private static final long serialVersionUID = 5498538842730004890L;
	
	public ForkException() {
		super();
	}

	public ForkException(String message, Throwable throwable) {
		super(message, throwable);
	}

	public ForkException(String message) {
		super(message);
	}

	public ForkException(Throwable throwable) {
		super(throwable);
	}
}
