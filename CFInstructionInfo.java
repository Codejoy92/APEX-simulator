
public class CFInstructionInfo {

	// We will use dispatched cycle number as the instruction label for dispatched control flow instruction
	// This label is needed to search the entry of control flow instruction in CFInstructionOrder
	// if the branch is taken. Using the CFID we will squash the instructions from pipeline, lsq, iq.
	// Using the ROB index present in the IQ entry, we will flush all the ROB entries after the control flow
	// instruction's ROB entry.
	private int dispatchCycleLabel;
	private int CFID;
	private int robIndex;
	private String instr;
	
	public CFInstructionInfo(int dispatchCycleLabel, int cfID, int robIndex, String instr) {
		super();
		this.dispatchCycleLabel = dispatchCycleLabel;
		this.CFID = cfID;
		this.robIndex = robIndex;
		this.instr = instr;
	}

	public int getDispatchCycleLabel() {
		return dispatchCycleLabel;
	}
	
	public void setDispatchCycleLabel(int dispatchCycleLabel) {
		this.dispatchCycleLabel = dispatchCycleLabel;
	}
	
	public int getCFID() {
		return CFID;
	}
	
	public void setCFID(int cfID) {
		CFID = cfID;
	}

	public int getRobIndex() {
		return robIndex;
	}

	public void setRobIndex(int robIndex) {
		this.robIndex = robIndex;
	}

	public String getInstr() {
		return instr;
	}

	public void setInstr(String instr) {
		this.instr = instr;
	}
}
