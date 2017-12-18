
class Instruction {
	private String opCode;
	private String operand1;
	private String operand2;
	private String operand3;
	
	public Instruction(String opCode, String arg1, String arg2, String arg3) {
		super();
		this.opCode = opCode;
		this.operand1 = arg1;
		this.operand2 = arg2;
		this.operand3 = arg3;
	}

	public String getOpCode() {
		return opCode;
	}

	public void setOpCode(String opCode) {
		this.opCode = opCode;
	}

	public String getArg1() {
		return operand1;
	}

	public void setArg1(String arg1) {
		this.operand1 = arg1;
	}

	public String getArg2() {
		return operand2;
	}

	public void setArg2(String arg2) {
		this.operand2 = arg2;
	}

	public String getArg3() {
		return operand3;
	}

	public void setArg3(String arg3) {
		this.operand3 = arg3;
	}
}