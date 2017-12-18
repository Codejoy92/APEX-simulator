
class Register {
	private int value;
	private boolean status;	// Valid = true, Invalid = false
	private int dataForwarderPC;
	private boolean zeroFlag;
	
	public Register() {
		value = 0;
		status = true;
		dataForwarderPC = -1;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public boolean getStatus() {
		return status;
	}

	public void setStatus(boolean status) {
		this.status = status;
	}
	
	public boolean isValid() {
		return status;
	}
	
	public int getDataForwarderPC() {
		return dataForwarderPC;
	}

	public void setDataForwarderPC(int dataForwarderPC) {
		this.dataForwarderPC = dataForwarderPC;
	}

	public boolean isZeroFlag() {
		return zeroFlag;
	}

	public void setZeroFlag(boolean zeroFlag) {
		this.zeroFlag = zeroFlag;
	}

}