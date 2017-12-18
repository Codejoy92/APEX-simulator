
public class IQEntry extends InstructionInfo {

	private boolean status;				// Allocated = true
	private boolean src1Status;			// Data Ready = true
	private boolean src2Status;			// Data Ready = true
	private boolean PSWZeroFlagStatus;	// Data Ready = true
	private int fuType;					// To check if required FU is free or not.
	//private int dispatchCycle;
	
	// Rest of the fields are present in the InstructionInfo object
	
	public IQEntry() {
		super();
		status = false;
		src1Status = false;
		src2Status = false;
		PSWZeroFlagStatus = false;
		//dispatchCycle = -1;
		fuType = IConstants.IFunctionalUnits.INTFU;
	}

	public void reset() {
		super.reset();
		status = false;
		src1Status = false;
		src2Status = false;
		PSWZeroFlagStatus = false;
		//dispatchCycle = -1;
		fuType = IConstants.IFunctionalUnits.INTFU;
	}
	
	public boolean getStatus() {
		return status;
	}

	public void setStatus(boolean status) {
		this.status = status;
	}

	public boolean getSrc1Status() {
		return src1Status;
	}

	public void setSrc1Status(boolean src1Status) {
		this.src1Status = src1Status;
	}

	public boolean getSrc2Status() {
		return src2Status;
	}

	public void setSrc2Status(boolean src2Status) {
		this.src2Status = src2Status;
	}

	public int getFuType() {
		return fuType;
	}

	public void setFuType(int fuType) {
		this.fuType = fuType;
	}

	public boolean getPSWZeroFlagStatus() {
		return PSWZeroFlagStatus;
	}

	public void setPSWZeroFlagStatus(boolean pSWZeroFlagStatus) {
		PSWZeroFlagStatus = pSWZeroFlagStatus;
	}

//	public int getDispatchCycle() {
//		return dispatchCycle;
//	}
//
//	public void setDispatchCycle(int dispatchCycle) {
//		this.dispatchCycle = dispatchCycle;
//	}
}


