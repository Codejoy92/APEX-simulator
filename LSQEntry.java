
public class LSQEntry extends InstructionInfo {
	
	private int index;
	private boolean allocated;
	private boolean loadInstruction;
	private boolean targetMemoryAddressReady;
	private boolean targetMemoryValueReady;
	
	public LSQEntry(LSQEntry entry) {
		super(entry.getProgramCounter(), entry.getInstruction(), entry.getOpCode(), entry.getSourceReg1Addr(), entry.getSourceReg1Value(), entry.getSourceReg2Addr(), entry.getSourceReg2Value(),
				entry.getDestRegAddr(), entry.getDestRegValue(), entry.getTargetMemoryAddr(), entry.getTargetMemoryValue(),
				entry.getLsqIndex(), entry.getRobIndex(), entry.getTagCFID(), entry.getDispatchCycle());
		this.index = entry.getIndex();
		this.allocated = entry.isAllocated();
		this.loadInstruction = entry.isLoadInstruction();
		this.targetMemoryAddressReady = entry.isTargetMemoryAddressReady();
		this.targetMemoryValueReady = entry.isTargetMemoryValueReady();
	}

	public LSQEntry(int index) {
		super();
		this.index = index;
		this.allocated = false;
		this.loadInstruction = false;
		this.targetMemoryAddressReady = false;
		this.targetMemoryValueReady = false;
	}
	
	public void reset() {
		super.reset();
		this.index = -1;
		this.allocated = false;
		this.loadInstruction = false;
		this.targetMemoryAddressReady = false;
		this.targetMemoryValueReady = false;
	}
	
	public boolean isAllocated() {
		return allocated;
	}
	
	public void setAllocated(boolean allocated) {
		this.allocated = allocated;
	}
	
	public boolean isLoadInstruction() {
		return loadInstruction;
	}
	
	public void setLoadInstruction(boolean loadInstruction) {
		this.loadInstruction = loadInstruction;
	}
	
	public boolean isTargetMemoryAddressReady() {
		return targetMemoryAddressReady;
	}
	
	public void setTargetMemoryAddressReady(boolean targetMemoryAddressReady) {
		this.targetMemoryAddressReady = targetMemoryAddressReady;
	}
	
	public boolean isTargetMemoryValueReady() {
		return targetMemoryValueReady;
	}
	
	public void setTargetMemoryValueReady(boolean targetMemoryValueReady) {
		this.targetMemoryValueReady = targetMemoryValueReady;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}
}
