
public class DivideByZeroException extends Exception {
	
	private static final long serialVersionUID = 1L;
	private String msg;
	
	public DivideByZeroException(String message) {
		this.msg = message;
	}

	@Override
	public String getMessage() {
		return this.msg;
	}

	@Override
	public String toString() {
		return "DivideByZeroException: " + msg + "]";
	}		
}
