
class InstructionInfo {
	private int programCounter;
	private String instruction;
	private String opCode;
	private int sourceReg1Addr;
	private int SourceReg1Value;
	private int sourceReg2Addr;
	private int sourceReg2Value;
	private int sourceReg3Value;
	private boolean sourcePSWZeroFlagValue;
	private int sourcePSWZeroFlagReg;
	private int destRegAddr;
	private int destRegValue;
	private int targetMemoryAddr;
	private int targetMemoryValue;
	private boolean updatePSW;
	private boolean carryPSW;
	private boolean zeroPSW;
	private boolean negativePSW;
	private int lsqIndex;
	private int robIndex;
	private int tagCFID;
	private int dispatchCycle;


	public InstructionInfo(int programCounter, String instruction, String opCode, int addrSourceReg1, int valueSourceReg1,
			int addrSourceReg2, int valueSourceReg2, int addrDestReg, int valuedestReg, int addrTargetMemory,
			int valueTargetMemory, int lsqIndex, int robIndex, int tagCFID, int dispatchCycle) {
		super();
		this.programCounter = programCounter;
		this.instruction = instruction;
		this.opCode = opCode;
		this.sourceReg1Addr = addrSourceReg1;
		this.SourceReg1Value = valueSourceReg1;
		this.sourceReg2Addr = addrSourceReg2;
		this.sourceReg2Value = valueSourceReg2;
		this.sourceReg3Value = -1;
		this.sourcePSWZeroFlagReg = -1;
		this.sourcePSWZeroFlagValue = false;
		this.destRegAddr = addrDestReg;
		this.destRegValue = valuedestReg;
		this.targetMemoryAddr = addrTargetMemory;
		this.targetMemoryValue = valueTargetMemory;
		this.updatePSW = true;
		this.carryPSW = false;
		this.zeroPSW = false;
		this.negativePSW = false;
		this.lsqIndex = lsqIndex;
		this.robIndex = robIndex;
		this.tagCFID = tagCFID;
		this.dispatchCycle = dispatchCycle;
	}
	
	public InstructionInfo(int programCounter) {
		this.programCounter = programCounter;
		this.instruction = null;
		this.opCode = null;
		this.sourceReg1Addr = -1;
		this.SourceReg1Value = -1;
		this.sourceReg2Addr = -1;
		this.sourceReg2Value = -1;
		this.sourceReg3Value = -1;
		this.sourcePSWZeroFlagReg = -1;
		this.sourcePSWZeroFlagValue = false;
		this.destRegAddr = -1;
		this.destRegValue = -1;
		this.targetMemoryAddr = -1;
		this.targetMemoryValue = -1;
		this.updatePSW = true;
		this.carryPSW = false;
		this.zeroPSW = false;
		this.negativePSW = false;
		this.lsqIndex = -1;
		this.tagCFID = -1;
		this.dispatchCycle = -1;
	}

	public InstructionInfo() {
		this.programCounter = -1;
		this.instruction = null;
		this.opCode = null;
		this.sourceReg1Addr = -1;
		this.SourceReg1Value = -1;
		this.sourceReg2Addr = -1;
		this.sourceReg2Value = -1;
		this.sourceReg3Value = -1;
		this.sourcePSWZeroFlagReg = -1;
		this.sourcePSWZeroFlagValue = false;
		this.destRegAddr = -1;
		this.destRegValue = -1;
		this.targetMemoryAddr = -1;
		this.targetMemoryValue = -1;
		this.updatePSW = true;
		this.carryPSW = false;
		this.zeroPSW = false;
		this.negativePSW = false;
		this.lsqIndex = -1;
		this.tagCFID = -1;
		this.dispatchCycle = -1;
	}

	public void reset() {
		this.programCounter = -1;
		this.instruction = null;
		this.opCode = null;
		this.sourceReg1Addr = -1;
		this.SourceReg1Value = -1;
		this.sourceReg2Addr = -1;
		this.sourceReg2Value = -1;
		this.sourceReg3Value = -1;
		this.sourcePSWZeroFlagReg = -1;
		this.sourcePSWZeroFlagValue = false;
		this.destRegAddr = -1;
		this.destRegValue = -1;
		this.targetMemoryAddr = -1;
		this.targetMemoryValue = -1;
		this.updatePSW = true;
		this.carryPSW = false;
		this.zeroPSW = false;
		this.negativePSW = false;
		this.lsqIndex = -1;
		this.tagCFID = -1;
		this.dispatchCycle = -1;
	}
	
	public int getProgramCounter() {
		return programCounter;
	}

	public void setProgramCounter(int programCounter) {
		this.programCounter = programCounter;
	}

	public String getInstruction() {
		return instruction;
	}

	public void setInstruction(String instruction) {
		this.instruction = instruction;
	}

	public String getOpCode() {
		return opCode;
	}

	public void setOpCode(String opCode) {
		this.opCode = opCode;
	}

	public int getSourceReg1Addr() {
		return sourceReg1Addr;
	}

	public void setSourceReg1Addr(int sourceReg1Addr) {
		this.sourceReg1Addr = sourceReg1Addr;
	}

	public int getSourceReg1Value() {
		return SourceReg1Value;
	}

	public void setSourceReg1Value(int sourceReg1Value) {
		SourceReg1Value = sourceReg1Value;
	}

	public int getSourceReg2Addr() {
		return sourceReg2Addr;
	}

	public void setSourceReg2Addr(int sourceReg2Addr) {
		this.sourceReg2Addr = sourceReg2Addr;
	}

	public int getSourceReg2Value() {
		return sourceReg2Value;
	}

	public void setSourceReg2Value(int sourceReg2Value) {
		this.sourceReg2Value = sourceReg2Value;
	}

	public int getDestRegAddr() {
		return destRegAddr;
	}

	public void setDestRegAddr(int destRegAddr) {
		this.destRegAddr = destRegAddr;
	}

	public int getDestRegValue() {
		return destRegValue;
	}

	public void setDestRegValue(int destRegValue) {
		this.destRegValue = destRegValue;
	}

	public int getTargetMemoryAddr() {
		return targetMemoryAddr;
	}

	public void setTargetMemoryAddr(int targetMemoryAddr) {
		this.targetMemoryAddr = targetMemoryAddr;
	}

	public int getTargetMemoryValue() {
		return targetMemoryValue;
	}

	public void setTargetMemoryValue(int targetMemoryValue) {
		this.targetMemoryValue = targetMemoryValue;
	}

	public boolean canUpdatePSW() {
		return updatePSW;
	}

	public void setUpdatePSW(boolean updatePSW) {
		this.updatePSW = updatePSW;
	}

	public boolean isCarryPSW() {
		return carryPSW;
	}

	public void setCarryPSW(boolean carryPSW) {
		this.carryPSW = carryPSW;
	}

	public boolean isZeroPSW() {
		return zeroPSW;
	}

	public void setZeroPSW(boolean zeroPSW) {
		this.zeroPSW = zeroPSW;
	}

	public boolean isNegativePSW() {
		return negativePSW;
	}

	public void setNegativePSW(boolean negativePSW) {
		this.negativePSW = negativePSW;
	}

	public int getLsqIndex() {
		return lsqIndex;
	}

	public void setLsqIndex(int lsqIndex) {
		this.lsqIndex = lsqIndex;
	}

	public int getRobIndex() {
		return robIndex;
	}

	public void setRobIndex(int robIndex) {
		this.robIndex = robIndex;
	}

	@Override
	public String toString() {
		return "InstructionInfo [programCounter=" + programCounter + ", instruction=" + instruction + ", opCode="
				+ opCode + ", sourceReg1Addr=" + sourceReg1Addr + ", SourceReg1Value=" + SourceReg1Value
				+ ", sourceReg2Addr=" + sourceReg2Addr + ", sourceReg2Value=" + sourceReg2Value + ", destRegAddr="
				+ destRegAddr + ", destRegValue=" + destRegValue + ", targetMemoryAddr=" + targetMemoryAddr
				+ ", targetMemoryValue=" + targetMemoryValue + ", updatePSW=" + updatePSW + "]";
	}

	public int getSourceReg3Value() {
		return sourceReg3Value;
	}

	public void setSourceReg3Value(int sourceReg3Value) {
		this.sourceReg3Value = sourceReg3Value;
	}

	public int getTagCFID() {
		return tagCFID;
	}

	public void setTagCFID(int tagCFID) {
		this.tagCFID = tagCFID;
	}
	
	public int getDispatchCycle() {
		return dispatchCycle;
	}

	public void setDispatchCycle(int dispatchCycle) {
		this.dispatchCycle = dispatchCycle;
	}

	public int getSourcePSWZeroFlagReg() {
		return sourcePSWZeroFlagReg;
	}

	public void setSourcePSWZeroFlagReg(int sourcePSWZeroFlagReg) {
		this.sourcePSWZeroFlagReg = sourcePSWZeroFlagReg;
	}

	public boolean isSourcePSWZeroFlagValue() {
		return sourcePSWZeroFlagValue;
	}

	public void setSourcePSWZeroFlagValue(boolean sourcePSWZeroFlagValue) {
		this.sourcePSWZeroFlagValue = sourcePSWZeroFlagValue;
	}

}