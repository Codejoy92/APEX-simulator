
class PhysicalRegisterFile {
	private PhysicalRegister registers[];
	
	PhysicalRegisterFile(int registerCnt) {
		this.registers = new PhysicalRegister[registerCnt];
		
		for (int cnt = 0; cnt < registerCnt; cnt++){
			registers[cnt] = new PhysicalRegister();
		}
	}
	
	public PhysicalRegisterFile(PhysicalRegisterFile copy) {
		PhysicalRegister[] tempRegs = copy.getRegisters();
		int size = tempRegs.length;
		
		this.registers = new PhysicalRegister[size];

		for (int cnt = 0; cnt < size; cnt++){
			registers[cnt] = new PhysicalRegister(tempRegs[cnt]);
		}
	}
	
	public PhysicalRegister[] getRegisters() {
		return registers;
	}

	public void setRegisters(PhysicalRegister[] registers) {
		this.registers = registers;
	}
	
	public PhysicalRegister getRegister(int index) throws ArrayIndexOutOfBoundsException {
		if (0 > index || index >= registers.length) {
			throw new ArrayIndexOutOfBoundsException("Index not present: " + index);
		}
		
		return registers[index];
	}
	
	public void setRegister(int index, PhysicalRegister reg) throws ArrayIndexOutOfBoundsException {
		if (0 > index || index >= registers.length) {
			throw new ArrayIndexOutOfBoundsException("Index not present: " + index);
		}
		
		registers[index] = reg;
	}
}