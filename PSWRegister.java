
class PSWRegister {
	private boolean carry;
	private boolean zero;
	private boolean negative;
	private boolean status;	// Valid = true, Invalid = false
	private int dataForwarderPC;
	
	public PSWRegister() {
		carry = false;
		zero = false;
		negative = false;
		status = true;
		dataForwarderPC = -1;
	}

	public boolean isCarry() {
		return carry;
	}

	public void setCarry(boolean carry) {
		this.carry = carry;
	}

	public boolean isZero() {
		return zero;
	}

	public void setZero(boolean zero) {
		this.zero = zero;
	}

	public boolean isNegative() {
		return negative;
	}

	public void setNegative(boolean negative) {
		this.negative = negative;
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
}