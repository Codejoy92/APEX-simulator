
class DataMemory {
	private int baseAddress;
	private int dataArray[];
	
	public DataMemory(int baseAddress, int cwDataMemory) {
		this.baseAddress = baseAddress;
		dataArray = new int[cwDataMemory];
	}

	public int getBaseAddress() {
		return baseAddress;
	}

	public void setBaseAddress(int baseAddress) {
		this.baseAddress = baseAddress;
	}

	public int[] getDataArray() {
		return dataArray;
	}

	public void setDataArray(int[] dataArray) {
		this.dataArray = dataArray;
	}
	
	public int getData(int address) throws ArrayIndexOutOfBoundsException {
		if (address >= dataArray.length) {
			throw new ArrayIndexOutOfBoundsException(
					"Index out of bound. Index: " + address + " Size: " + dataArray.length);
		}
		
		return dataArray[address];
	}
	
	public void setData(int address, int data) throws ArrayIndexOutOfBoundsException {
		if (address >= dataArray.length) {
			throw new ArrayIndexOutOfBoundsException(
					"Index out of bound. Index: " + address + " Size: " + dataArray.length);
		}

		dataArray[address] = data;
	}
}