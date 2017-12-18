
class PipeLineStage {
	private InstructionInfo inputInstruction;
	private InstructionInfo outputInstruction;
	private boolean stalled;	// Stalled = True
	private boolean busy;	// Stalled = True
	
	public PipeLineStage() {
		inputInstruction = new InstructionInfo();
		outputInstruction = new InstructionInfo();
		stalled = false;
		busy = false;
	}

	public InstructionInfo getInputInstruction() {
		return inputInstruction;
	}

	public void setInputInstruction(InstructionInfo inputInstruction) {
		this.inputInstruction = inputInstruction;
	}

	public InstructionInfo getOutputInstruction() {
		return outputInstruction;
	}

	public void setOutputInstruction(InstructionInfo outputInstruction) {
		this.outputInstruction = outputInstruction;
	}

	public boolean isStalled() {
		return stalled;
	}

	public void setStalled(boolean stalled) {
		this.stalled = stalled;
	}

	public boolean isBusy() {
		return busy;
	}

	public void setBusy(boolean busy) {
		this.busy = busy;
	}

	@Override
	public String toString() {
		return "[inputInstruction = " + inputInstruction + System.getProperty("line.separator") +
				"\toutputInstruction = " + outputInstruction + System.getProperty("line.separator") +
				"\tstalled = " + stalled + "]";
	}

//	@Override
//	public String toString() {
//		return "PipeLineStage ["
//				+ "\r\ninputInstruction=" + inputInstruction
//				+ "\r\noutputInstruction=" + outputInstruction
//				+ "\r\nstalled=" + stalled + "]";
//	}	
}