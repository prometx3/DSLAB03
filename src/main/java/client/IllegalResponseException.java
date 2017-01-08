package client;

public class IllegalResponseException extends Exception {

	private static final long serialVersionUID = 1L;

	public IllegalResponseException(String message, Throwable cause) {
		super(message, cause);
	}

	public IllegalResponseException(String message) {
		super(message);
	}

}
