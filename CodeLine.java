
class CodeLine {
	private int fileLineNumber;
	private int instAddress;
	private String instruction;
	
	public int getFileLineNumber() {
		return fileLineNumber;
	}
	
	public void setFileLineNumber(int fileLineNumber) {
		this.fileLineNumber = fileLineNumber;
	}
	
	public int getInstAddress() {
		return instAddress;
	}
	
	public void setInstAddress(int instAddress) {
		this.instAddress = instAddress;
	}
	
	public String getInstruction() {
		return instruction;
	}
	
	public void setInstruction(String instruction) {
		this.instruction = instruction;
	}
	
}