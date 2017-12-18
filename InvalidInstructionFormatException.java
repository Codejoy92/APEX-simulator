
public class InvalidInstructionFormatException extends Exception {

	private static final long serialVersionUID = 1L;
	private String msg;
	
	public InvalidInstructionFormatException(String message) {
		this.msg = message;
	}

	@Override
	public String getMessage() {
		return this.msg;
	}

	@Override
	public String toString() {
		return "InvalidInstructionFormatException: " + msg + "]";
	}		
}