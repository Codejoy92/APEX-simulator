
public class PipeLineStageEx extends PipeLineStage {
	
	private boolean receivedForwardedSrc1;
	private boolean receivedForwardedSrc2;
	private boolean receivedForwardedPSW;
	private int archRegAddr;
	private boolean receivedArchRegAddr;
	private boolean receivedRenamedInstruction;
	
	public PipeLineStageEx() {
		super();
		receivedForwardedSrc1 = false;
		receivedForwardedSrc2 = false;
		receivedForwardedPSW = false;
		archRegAddr = -1;
		receivedArchRegAddr = false;
		receivedRenamedInstruction = false;
	}

	public boolean isReceivedForwardedSrc1() {
		return receivedForwardedSrc1;
	}
	
	public void setReceivedForwardedSrc1(boolean receivedForwardedSrc1) {
		this.receivedForwardedSrc1 = receivedForwardedSrc1;
	}
	
	public boolean isReceivedForwardedSrc2() {
		return receivedForwardedSrc2;
	}
	
	public void setReceivedForwardedSrc2(boolean receivedForwardedSrc2) {
		this.receivedForwardedSrc2 = receivedForwardedSrc2;
	}
	
	public boolean isReceivedForwardedPSW() {
		return receivedForwardedPSW;
	}
	
	public void setReceivedForwardedPSW(boolean receivedForwardedPSW) {
		this.receivedForwardedPSW = receivedForwardedPSW;
	}

	public boolean isReceivedRenamedInstruction() {
		return receivedRenamedInstruction;
	}

	public void setReceivedRenamedInstruction(boolean receivedRenamedInstruction) {
		this.receivedRenamedInstruction = receivedRenamedInstruction;
	}

	public int getArchRegAddr() {
		return archRegAddr;
	}

	public void setArchRegAddr(int archRegAddr) {
		this.archRegAddr = archRegAddr;
	}

	public boolean isReceivedArchRegAddr() {
		return receivedArchRegAddr;
	}

	public void setReceivedArchRegAddr(boolean receivedArchRegAddr) {
		this.receivedArchRegAddr = receivedArchRegAddr;
	}
}
