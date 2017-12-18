
public class ROBEntry extends InstructionInfo {
	
	private boolean allocated;
	private int archRegAddr;
	private boolean resultReady;
	
	// To store the precise state in ROB entry
	private RenameTableEntry[] renameTableBackup;
	private PhysicalRegisterFile phyRegFileBackup;
	
	public ROBEntry(ROBEntry entry) {
		super(entry.getProgramCounter(), entry.getInstruction(), entry.getOpCode(), entry.getSourceReg1Addr(), entry.getSourceReg1Value(), entry.getSourceReg2Addr(), entry.getSourceReg2Value(),
				entry.getDestRegAddr(), entry.getDestRegValue(), entry.getTargetMemoryAddr(), entry.getTargetMemoryValue(),
				entry.getLsqIndex(), entry.getRobIndex(), entry.getTagCFID(), entry.getDispatchCycle());
		this.allocated = entry.isAllocated();
		this.archRegAddr = entry.getArchRegAddr();
		this.resultReady = entry.isResultReady();
		this.renameTableBackup = null;
		this.phyRegFileBackup = null;
	}

	public ROBEntry() {
		super();
		this.allocated = false;
		this.archRegAddr = -1;
		this.resultReady = false;
	}
	
	public void reset() {
		super.reset();
		this.allocated = false;
		this.archRegAddr = -1;
		this.resultReady = false;
	}
	
	public boolean isAllocated() {
		return allocated;
	}
	
	public void setAllocated(boolean allocated) {
		this.allocated = allocated;
	}

	public boolean isResultReady() {
		return resultReady;
	}

	public void setResultReady(boolean resultReady) {
		this.resultReady = resultReady;
	}

	public int getArchRegAddr() {
		return archRegAddr;
	}

	public void setArchRegAddr(int archRegAddr) {
		this.archRegAddr = archRegAddr;
	}
	
	public void backupRenameTable(RenameTableEntry[] renameTable) {
		// Perform cloning or deep copy while taking backup
		int size = renameTable.length;
		
		renameTableBackup = new RenameTableEntry[size];
		for (int cnt = 0; cnt < size; cnt++) {
			renameTableBackup[cnt] = new RenameTableEntry(renameTable[cnt].getRegAddress(),
					renameTable[cnt].getArchOrPhyRegIndicator());
		}
	}
	
	public void backupPhysicalRegisterFile(PhysicalRegisterFile phyRegFile) {
		// Perform cloning or deep copy while taking backup
		phyRegFileBackup = new PhysicalRegisterFile(phyRegFile);
	}

	public RenameTableEntry[] getRenameTableBackup() {
		return renameTableBackup;
	}

	public PhysicalRegisterFile getPhyRegFileBackup() {
		return phyRegFileBackup;
	}
	
}
