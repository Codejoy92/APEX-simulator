
class RegisterFile {
	private Register registers[];

	//private PSWRegister PSW;
	
	RegisterFile(int registerCnt) {
		this.registers = new Register[registerCnt];
		
		for (int cnt = 0; cnt < registerCnt; cnt++){
			registers[cnt] = new Register();
		}
		
		//PSW = new PSWRegister();
	}
	
	public Register[] getRegisters() {
		return registers;
	}

	public void setRegisters(Register[] registers) {
		this.registers = registers;
	}
	
	public Register getRegister(int index) throws ArrayIndexOutOfBoundsException {
		if (0 > index || index >= registers.length) {
			throw new ArrayIndexOutOfBoundsException("Index not present: " + index);
		}
		
		return registers[index];
	}
	
//	public PSWRegister getPSW() {
//		return PSW;
//	}
//
//	public void setPSW(PSWRegister psw) {
//		PSW = psw;
//	}
}