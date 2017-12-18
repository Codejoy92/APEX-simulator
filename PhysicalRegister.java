
class PhysicalRegister {

	private int value;
	private boolean status;		// Used for status bit vector (Valid = true)
	private boolean renamed; 	// Used for maintaining renamed bit vector (Renamed = true)
	private boolean allocated; 	// Used to keep track of allocated physical registers (Allocated = true)
	private boolean zeroFlag;
	private int dataForwarderPC;
	
	public PhysicalRegister() {
		value = 0;
		status = true;
		renamed = false;
		allocated = false;
		zeroFlag = false;
		dataForwarderPC = -1;
	}

	public PhysicalRegister(PhysicalRegister copy) {
		this.value = copy.getValue();
		this.status = copy.getStatus();
		this.renamed = copy.isRenamed();
		this.allocated = copy.isAllocated();
		this.zeroFlag = copy.isZeroFlag();
		this.dataForwarderPC = copy.getDataForwarderPC();
	}

	public void reset() {
		value = 0;
		status = true;
		renamed = false;
		allocated = false;
		zeroFlag = false;
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

	public boolean isRenamed() {
		return renamed;
	}

	public void setRenamed(boolean renamed) {
		this.renamed = renamed;
	}

	public boolean isAllocated() {
		return allocated;
	}

	public void setAllocated(boolean allocated) {
		this.allocated = allocated;
	}

	public boolean isZeroFlag() {
		return zeroFlag;
	}

	public void setZeroFlag(boolean zeroFlag) {
		this.zeroFlag = zeroFlag;
	}
}