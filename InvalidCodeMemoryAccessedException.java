
class InvalidCodeMemoryAccessedException extends Exception {
	
	private static final long serialVersionUID = 1L;
	private String msg;
	
	public InvalidCodeMemoryAccessedException(String message) {
		this.msg = message;
	}

	@Override
	public String getMessage() {
		return this.msg;
	}

	@Override
	public String toString() {
		return "InvalidCodeMemoryAccessedException: " + msg + "]";
	}		
}