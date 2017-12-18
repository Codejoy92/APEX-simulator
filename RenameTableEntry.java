
public class RenameTableEntry {

	int regAddress;
	byte archOrPhyRegIndicator; // 0 means arch reg 
	
	public RenameTableEntry(int regAddress) {
		this.regAddress = regAddress;
		this.archOrPhyRegIndicator = 0;
	}

	public RenameTableEntry(int regAddress, byte archOrPhyRegIndicator) {
		this.regAddress = regAddress;
		this.archOrPhyRegIndicator = archOrPhyRegIndicator;
	}

	public int getRegAddress() {
		return regAddress;
	}

	public void setRegAddress(int regAddress) {
		this.regAddress = regAddress;
	}

	public byte getArchOrPhyRegIndicator() {
		return archOrPhyRegIndicator;
	}

	public void setArchOrPhyRegIndicator(byte archOrPhyRegIndicator) {
		this.archOrPhyRegIndicator = archOrPhyRegIndicator;
	}
}
