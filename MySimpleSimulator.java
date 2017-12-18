import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class MySimpleSimulator {

	private CodeMemory codeMem;
	private DataMemory dataMem;
	private RegisterFile regFile;

	private PhysicalRegisterFile phyRegFile;
	private RenameTableEntry[] renameTable;
	
	private ReOrderBuffer rob;
	private LoadStoreQueue lsq;
	private ArrayList<IQEntry> issueQueue;
	
	private PipeLineStage fetchStage;
	private PipeLineStageEx decodeRFStage;
	private PipeLineStage mul2Stage;
	private PipeLineStage mul1Stage;
	private PipeLineStage div1Stage;
	private PipeLineStage div2Stage;
	private PipeLineStage div3Stage;
	private PipeLineStage div4Stage;
	private PipeLineStage addStage;
	private PipeLineStage memoryStage;
	//private PipeLineStage writeBackStage;
	
	private ArrayList<Integer> freeCFIDList;
	private ArrayList<CFInstructionInfo> CFInstructionOrder;
	
	private int lastCFID;
	
	private static int memStageCycle = 0;
	
	private static Scanner scanner = new Scanner(System.in);
	
	private boolean isInitialized;
	
	// This will be set by branch instructions in IntFU if the branch is taken
	// and it will be used by decodeRF to stall itself and control flow change handling logic
	private boolean controlFlowAltered;
	private int controlFlowInstrLabel;
	private int controlFlowInstrRobIndex;
	
	// This will be set after squashing all instructions along the wrong path
	// and it will be used by decodeRF to resume itself and fetch stage
	private boolean controlFlowHandled;

	private String committedInstructions;
	
	
	public MySimpleSimulator() {
		isInitialized = false;
	}
	
	private void fetch() throws InvalidCodeMemoryAccessedException {
	
		// If the stage is stalled do nothing.
		if (fetchStage.isStalled()) {
			return;
		}

		int pc = fetchStage.getInputInstruction().getProgramCounter();	
		// Return if PC is invalid
		if (-1 == pc) {
			fetchStage.getOutputInstruction().reset();
			return;
		}

		assertCodeMemoryValidity(pc);
		
		// Get instruction pointed by PC
		CodeLine codeLine = codeMem.getCodeLine(pc);
		if (null == codeLine) {
			fetchStage.getOutputInstruction().reset();
			return;
		}
		
		String instructionStr = codeLine.getInstruction();
		
		// Copy data into output latch
		fetchStage.getOutputInstruction().reset();
		fetchStage.getOutputInstruction().setProgramCounter(pc);
		fetchStage.getOutputInstruction().setInstruction(instructionStr);

//		// Copy output data into input of next stage
//		copyData(decodeRFStage.getInputInstruction(), fetchStage.getOutputInstruction());
		
		// Adjust PC value for next fetch
		fetchStage.getInputInstruction().setProgramCounter(
				fetchStage.getInputInstruction().getProgramCounter() + IConstants.INSTRUCTION_SIZE);
	}
	
	private void decodeRFEx() throws InvalidRegisterAddressException, InvalidInstructionFormatException {
		
		String tempStr = new String("");
		String src1Str = new String("");
		String src2Str = new String("");
		String destStr = new String("");
		String src3Str = new String("");

		if (this.controlFlowHandled) {
			this.controlFlowHandled = false;
			
			//+ TODO: Uncomment this if we want decodeRF and Fetch to stall when flushing is ongoing
//			decodeRFStage.setStalled(false);
//			fetchStage.setStalled(false);
			//-

			// In this cycle, after handling the control flow change, some other branch instruction
			// that executed in intFU may have again caused control flow change. SO handle following case explicitly.
			if (this.controlFlowAltered) {
				//+ TODO: Uncomment this if we want decodeRF and Fetch to stall when flushing is ongoing
//				// If control flow is changed in this cycle, decode RF is flushed.
//				// Stall decodeRF and fetch in this cycle, until instructions along the wrong path are squashed
//				// in the next cycle.
//				decodeRFStage.setStalled(true);
//				fetchStage.setStalled(true);
				//-
			}

			//+ TODO: Uncomment this if we want decodeRF and Fetch to stall when flushing is ongoing
//			// In this cycle we have handled the control flow change and squashing.
//			// At the end of this cycle, decodeRF and fetch will be unstalled. So return now as we can only execute
//			// in the next cycle.
//			return;
			//-
		}

		if (this.controlFlowAltered) {
			//+ TODO: Uncomment this. if we want decodeRF and Fetch to stall when flushing is ongoing
//			// If control flow is changed in this cycle, decode RF is flushed.
//			// Stall decodeRF and fetch, until instructions along the wrong path are squashed
//			// in the next cycle.
//			decodeRFStage.setStalled(true);
//			fetchStage.setStalled(true);
			//-
		}

		String instructionStr = decodeRFStage.getInputInstruction().getInstruction();
		if (null == instructionStr) {
			decodeRFStage.getOutputInstruction().reset();
			
			//+
			// Verify this check. Check if we need to copy this input only if decodeRF is not stalled
			if (false == decodeRFStage.isStalled()) {
			//-
			// Copy output instruction of fetch stage to input of decodeRF stage for next cycle
			copyData(decodeRFStage.getInputInstruction(), fetchStage.getOutputInstruction());
			
			//+
			}
			//-
			return;
		}
		
		int lsqIndex = -1;
		int robIndex = -1;
		boolean allocateCFID = false;
		
		Instruction instr = parseInstruction(instructionStr);

		if (false == decodeRFStage.isStalled()) {
			decodeRFStage.getOutputInstruction().reset();
			decodeRFStage.getOutputInstruction().setProgramCounter(
					decodeRFStage.getInputInstruction().getProgramCounter());
			decodeRFStage.getOutputInstruction().setInstruction(instructionStr);
			decodeRFStage.getOutputInstruction().setOpCode(instr.getOpCode());
			// Set the tag of this instruction as the CFID of the last dispatched control flow instruction
			// Copy it to output instruction here, so that it will be copied to LSQ, IQ and ROB automatically
			decodeRFStage.getOutputInstruction().setTagCFID(lastCFID);
		}
		
		int functionalUnit = IConstants.IFunctionalUnits.INTFU;
		boolean isCurrentlyStalled = false;
		boolean isArithmeticInstr = false;
		boolean receivedPSWZeroFlag = false;
		
		try {
			switch(instr.getOpCode()) {
				case IConstants.IOperations.LOAD:
				{
					//functionalUnit = IConstants.IFunctionalUnits.INTFU;
					
					int destReg = Integer.parseInt(instr.getArg1().substring(1));
					int srcReg1Addr = Integer.parseInt(instr.getArg2().substring(1));
					int srcReg2Value = Integer.parseInt(instr.getArg3());
					int srcReg1Value = -1;
					
					assertRegisterValidity(srcReg1Addr);
					
					if (!decodeRFStage.isReceivedForwardedSrc1()) {
						// Lookup rename table to find most recent instance corresponding to source 1
						if (IConstants.ARCHITECTURAL_REGISTER == renameTable[srcReg1Addr].archOrPhyRegIndicator) {
							decodeRFStage.getOutputInstruction().setSourceReg1Addr(renameTable[srcReg1Addr].regAddress);
							srcReg1Value = regFile.getRegister(srcReg1Addr).getValue();
							decodeRFStage.getOutputInstruction().setSourceReg1Value(srcReg1Value);
							decodeRFStage.setReceivedForwardedSrc1(true);
							src1Str = new String("R" + renameTable[srcReg1Addr].regAddress);
						} else {
							src1Str = new String("P" + renameTable[srcReg1Addr].regAddress);
							decodeRFStage.getOutputInstruction().setSourceReg1Addr(renameTable[srcReg1Addr].regAddress);
							if (phyRegFile.getRegister(renameTable[srcReg1Addr].regAddress).getStatus()) {
//								decodeRFStage.getOutputInstruction().setSourceReg1Addr(srcReg1Addr);
								srcReg1Value = phyRegFile.getRegister(renameTable[srcReg1Addr].regAddress).getValue();
								decodeRFStage.getOutputInstruction().setSourceReg1Value(srcReg1Value);
								decodeRFStage.setReceivedForwardedSrc1(true);
							} else {
								// Handle forwarding
								Integer value = getForwardedData(renameTable[srcReg1Addr].regAddress);
								if (null != value) {
									decodeRFStage.getOutputInstruction().setSourceReg1Addr(srcReg1Addr);
									decodeRFStage.getOutputInstruction().setSourceReg1Value(value);
									decodeRFStage.setReceivedForwardedSrc1(true);
								}
							}
						}
					}
					
					decodeRFStage.getOutputInstruction().setSourceReg2Value(srcReg2Value);
					decodeRFStage.setReceivedForwardedSrc2(true);

					src2Str = new String("#" + srcReg2Value);
					
					if (-1 == decodeRFStage.getOutputInstruction().getDestRegAddr()) {
						// Check validity of dest reg address
						assertRegisterValidity(destReg);

						decodeRFStage.setArchRegAddr(destReg);
						decodeRFStage.setReceivedArchRegAddr(true);
						
						// Retrieve a physical register to be allocated to hold most recent value of this dest arch reg
						int phyRegIndex = getFreePhysicalReg();
						if (-1 == phyRegIndex) {
							isCurrentlyStalled = true;
						} else {
							renameRegister(destReg, phyRegIndex);

							decodeRFStage.getOutputInstruction().setDestRegAddr(phyRegIndex);
							destStr = new String("P" + phyRegIndex);
						}
					}

					// Check if LSQ is full
					if (lsq.isFull()) {
						isCurrentlyStalled = true;
					}
					
					tempStr = new String(instr.getOpCode() + ", " + destStr + ", " + src1Str + ", " + src2Str);
	
					break;
				}
				
				case IConstants.IOperations.STORE:
				{
					//functionalUnit = IConstants.IFunctionalUnits.INTFU;
					
					int srcReg1Addr = Integer.parseInt(instr.getArg1().substring(1));
					int srcReg2Addr = Integer.parseInt(instr.getArg2().substring(1));
					int srcReg3Value = Integer.parseInt(instr.getArg3());
					int srcReg1Value = -1;
					int srcReg2Value = -1;
					
					assertRegisterValidity(srcReg1Addr);
					
					if (!decodeRFStage.isReceivedForwardedSrc1()) {
						// Lookup rename table to find most recent instance corresponding to source 1
						if (IConstants.ARCHITECTURAL_REGISTER == renameTable[srcReg1Addr].archOrPhyRegIndicator) {
							decodeRFStage.getOutputInstruction().setSourceReg1Addr(renameTable[srcReg1Addr].regAddress);
							srcReg1Value = regFile.getRegister(srcReg1Addr).getValue();
							decodeRFStage.getOutputInstruction().setSourceReg1Value(srcReg1Value);
							decodeRFStage.setReceivedForwardedSrc1(true);
							src1Str = new String("R" + renameTable[srcReg1Addr].regAddress);
						} else {
							src1Str = new String("P" + renameTable[srcReg1Addr].regAddress);
							decodeRFStage.getOutputInstruction().setSourceReg1Addr(renameTable[srcReg1Addr].regAddress);
							if (phyRegFile.getRegister(renameTable[srcReg1Addr].regAddress).getStatus()) {
//								decodeRFStage.getOutputInstruction().setSourceReg1Addr(srcReg1Addr);
								srcReg1Value = phyRegFile.getRegister(renameTable[srcReg1Addr].regAddress).getValue();
								decodeRFStage.getOutputInstruction().setSourceReg1Value(srcReg1Value);
								decodeRFStage.setReceivedForwardedSrc1(true);
							} else {
								// Handle forwarding
								Integer value = getForwardedData(renameTable[srcReg1Addr].regAddress);
								if (null != value) {
									decodeRFStage.getOutputInstruction().setSourceReg1Addr(srcReg1Addr);
									decodeRFStage.getOutputInstruction().setSourceReg1Value(value);
									decodeRFStage.setReceivedForwardedSrc1(true);
								}
							}
						}
					}
					
					assertRegisterValidity(srcReg2Addr);
					
					if (!decodeRFStage.isReceivedForwardedSrc2()) {
						// Lookup rename table to find most recent instance corresponding to source 1
						if (IConstants.ARCHITECTURAL_REGISTER == renameTable[srcReg2Addr].archOrPhyRegIndicator) {
							decodeRFStage.getOutputInstruction().setSourceReg2Addr(renameTable[srcReg2Addr].regAddress);
							srcReg2Value = regFile.getRegister(srcReg2Addr).getValue();
							decodeRFStage.getOutputInstruction().setSourceReg2Value(srcReg2Value);
							decodeRFStage.setReceivedForwardedSrc2(true);
							src2Str = new String("R" + renameTable[srcReg2Addr].regAddress);
						} else {
							src2Str = new String("P" + renameTable[srcReg2Addr].regAddress);
							decodeRFStage.getOutputInstruction().setSourceReg2Addr(renameTable[srcReg2Addr].regAddress);
							if (phyRegFile.getRegister(renameTable[srcReg2Addr].regAddress).getStatus()) {
//								decodeRFStage.getOutputInstruction().setSourceReg2Addr(srcReg2Addr);
								srcReg2Value = phyRegFile.getRegister(renameTable[srcReg2Addr].regAddress).getValue();
								decodeRFStage.getOutputInstruction().setSourceReg2Value(srcReg2Value);
								decodeRFStage.setReceivedForwardedSrc2(true);
							} else {
								// Handle forwarding
								Integer value = getForwardedData(renameTable[srcReg2Addr].regAddress);
								if (null != value) {
									decodeRFStage.getOutputInstruction().setSourceReg2Addr(srcReg2Addr);
									decodeRFStage.getOutputInstruction().setSourceReg2Value(value);
									decodeRFStage.setReceivedForwardedSrc2(true);
								}
							}
						}
					}

					src3Str = new String("#" + srcReg3Value);
					decodeRFStage.getOutputInstruction().setSourceReg3Value(srcReg3Value);
					
					if (lsq.isFull()) {
						isCurrentlyStalled = true;
					}
				
					tempStr = new String(instr.getOpCode() + ", " + src1Str + ", " + src2Str + ", " + src3Str);

					break;
				}
				
				case IConstants.IOperations.ADD:
				case IConstants.IOperations.SUB:
				case IConstants.IOperations.MUL:
				case IConstants.IOperations.DIV:
				case IConstants.IOperations.AND:
				case IConstants.IOperations.OR:
				case IConstants.IOperations.EXOR:
				case IConstants.IOperations.JAL:
				{

					if (
							IConstants.IOperations.ADD.equalsIgnoreCase(instr.getOpCode())	||
							IConstants.IOperations.SUB.equalsIgnoreCase(instr.getOpCode())	||
							IConstants.IOperations.MUL.equalsIgnoreCase(instr.getOpCode())	||
							IConstants.IOperations.DIV.equalsIgnoreCase(instr.getOpCode())
							) {
						isArithmeticInstr = true;
					}
					
					if (IConstants.IOperations.DIV.equalsIgnoreCase(instr.getOpCode())) {
						functionalUnit = IConstants.IFunctionalUnits.DIVFU;
					} else if (IConstants.IOperations.MUL.equalsIgnoreCase(instr.getOpCode())){
						functionalUnit = IConstants.IFunctionalUnits.MULFU;
					} else {
						functionalUnit = IConstants.IFunctionalUnits.INTFU;
					}
					
					
					int destReg = Integer.parseInt(instr.getArg1().substring(1));
					int srcReg1Addr = Integer.parseInt(instr.getArg2().substring(1));
					//int srcReg2Addr = Integer.parseInt(instr.getArg3().substring(1));						
					//int srcReg2Value = -1;
					//+
					int srcReg2Addr = -1;
					int srcReg2Value = -1;
					if (IConstants.IOperations.JAL.equalsIgnoreCase(instr.getOpCode())) {
						srcReg2Value = Integer.parseInt(instr.getArg3());
					} else {
						srcReg2Addr = Integer.parseInt(instr.getArg3().substring(1));						
						srcReg2Value = -1;
					}
					//-
					
					int srcReg1Value = -1;
					
					assertRegisterValidity(srcReg1Addr);
					
					if (!decodeRFStage.isReceivedForwardedSrc1()) {
						// Lookup rename table to find most recent instance corresponding to source 1
						if (IConstants.ARCHITECTURAL_REGISTER == renameTable[srcReg1Addr].archOrPhyRegIndicator) {
							decodeRFStage.getOutputInstruction().setSourceReg1Addr(renameTable[srcReg1Addr].regAddress);
							srcReg1Value = regFile.getRegister(srcReg1Addr).getValue();
							decodeRFStage.getOutputInstruction().setSourceReg1Value(srcReg1Value);
							decodeRFStage.setReceivedForwardedSrc1(true);
							src1Str = new String("R" + renameTable[srcReg1Addr].regAddress);
						} else {
							src1Str = new String("P" + renameTable[srcReg1Addr].regAddress);
							decodeRFStage.getOutputInstruction().setSourceReg1Addr(renameTable[srcReg1Addr].regAddress);
							if (phyRegFile.getRegister(renameTable[srcReg1Addr].regAddress).getStatus()) {
								//decodeRFStage.getOutputInstruction().setSourceReg1Addr(srcReg1Addr);
								srcReg1Value = phyRegFile.getRegister(renameTable[srcReg1Addr].regAddress).getValue();
								decodeRFStage.getOutputInstruction().setSourceReg1Value(srcReg1Value);
								decodeRFStage.setReceivedForwardedSrc1(true);
							} else {
								// Handle forwarding
								Integer value = getForwardedData(renameTable[srcReg1Addr].regAddress);
								if (null != value) {
									decodeRFStage.getOutputInstruction().setSourceReg1Addr(srcReg1Addr);
									decodeRFStage.getOutputInstruction().setSourceReg1Value(value);
									decodeRFStage.setReceivedForwardedSrc1(true);
								}
							}
						}
					}
					
//					assertRegisterValidity(srcReg2Addr);
					
					//+
					if (IConstants.IOperations.JAL.equalsIgnoreCase(instr.getOpCode())) {
						src2Str = new String("#" + srcReg2Value);
						decodeRFStage.getOutputInstruction().setSourceReg2Value(srcReg2Value);
						decodeRFStage.setReceivedForwardedSrc2(true);
					} else {
					//-					

						assertRegisterValidity(srcReg2Addr);
						
						if (!decodeRFStage.isReceivedForwardedSrc2()) {
							// Lookup rename table to find most recent instance corresponding to source 1
							if (IConstants.ARCHITECTURAL_REGISTER == renameTable[srcReg2Addr].archOrPhyRegIndicator) {
								decodeRFStage.getOutputInstruction().setSourceReg2Addr(renameTable[srcReg2Addr].regAddress);
								srcReg2Value = regFile.getRegister(srcReg2Addr).getValue();
								decodeRFStage.getOutputInstruction().setSourceReg2Value(srcReg2Value);
								decodeRFStage.setReceivedForwardedSrc2(true);
								src2Str = new String("R" + renameTable[srcReg2Addr].regAddress);
							} else {
								src2Str = new String("P" + renameTable[srcReg2Addr].regAddress);
								decodeRFStage.getOutputInstruction().setSourceReg2Addr(renameTable[srcReg2Addr].regAddress);
								if (phyRegFile.getRegister(renameTable[srcReg2Addr].regAddress).getStatus()) {
	//								decodeRFStage.getOutputInstruction().setSourceReg2Addr(srcReg2Addr);
									srcReg2Value = phyRegFile.getRegister(renameTable[srcReg2Addr].regAddress).getValue();
									decodeRFStage.getOutputInstruction().setSourceReg2Value(srcReg2Value);
									decodeRFStage.setReceivedForwardedSrc2(true);
								} else {
									// Handle forwarding
									Integer value = getForwardedData(renameTable[srcReg2Addr].regAddress);
									if (null != value) {
										decodeRFStage.getOutputInstruction().setSourceReg2Addr(srcReg2Addr);
										decodeRFStage.getOutputInstruction().setSourceReg2Value(value);
										decodeRFStage.setReceivedForwardedSrc2(true);
									}
								}
							}
						}
					}

					if (-1 == decodeRFStage.getOutputInstruction().getDestRegAddr()) {
						// Check validity of dest reg address
						assertRegisterValidity(destReg);

						decodeRFStage.setArchRegAddr(destReg);
						decodeRFStage.setReceivedArchRegAddr(true);
						
						// Retrieve a physical register to be allocated to hold most recent value of this dest arch reg
						int phyRegIndex = getFreePhysicalReg();
						if (-1 == phyRegIndex) {
							isCurrentlyStalled = true;
						} else {
							renameRegister(destReg, phyRegIndex);
							decodeRFStage.getOutputInstruction().setDestRegAddr(phyRegIndex);
						}
						
						destStr = new String("P" + phyRegIndex);
					}
				

					// If this is "JAL" instruction, allocate check if free CFID is available
					if (IConstants.IOperations.JAL.equalsIgnoreCase(instr.getOpCode())) {
						allocateCFID = true;
						
						// Check if CFID can be allocated. If not, stall this instruction
						if (0 == freeCFIDList.size()) {
							isCurrentlyStalled = true;
						}
					}

					tempStr = new String(instr.getOpCode() + ", " + destStr + ", " + src1Str + ", " + src2Str);
					break;
				}

				case IConstants.IOperations.MOVC:
				{
					int destReg = Integer.parseInt(instr.getArg1().substring(1));
					int srcReg1Value = Integer.parseInt(instr.getArg2());
					
					decodeRFStage.getOutputInstruction().setSourceReg1Value(srcReg1Value);
					decodeRFStage.setReceivedForwardedSrc1(true);
					decodeRFStage.getOutputInstruction().setSourceReg2Value(-1);
					decodeRFStage.setReceivedForwardedSrc2(true);
					
					assertRegisterValidity(destReg);
					
					if (-1 == decodeRFStage.getOutputInstruction().getDestRegAddr()) {
						// Check validity of dest reg address
						assertRegisterValidity(destReg);

						decodeRFStage.setArchRegAddr(destReg);
						decodeRFStage.setReceivedArchRegAddr(true);
						
						// Retrieve a physical register to be allocated to hold most recent value of this dest arch reg
						int phyRegIndex = getFreePhysicalReg();
						if (-1 == phyRegIndex) {
							isCurrentlyStalled = true;
						} else {
							renameRegister(destReg, phyRegIndex);
							decodeRFStage.getOutputInstruction().setDestRegAddr(phyRegIndex);
						}
						
						destStr = new String("P" + phyRegIndex);
					}
					
					tempStr = new String(instr.getOpCode() + ", " + destStr + ", #" + srcReg1Value);

					break;
				}
				
				case IConstants.IOperations.BZ:
				case IConstants.IOperations.BNZ:
				{
					int srcReg1Value = Integer.parseInt(instr.getArg1());					
					decodeRFStage.getOutputInstruction().setSourceReg1Value(srcReg1Value);
					decodeRFStage.setReceivedForwardedSrc1(true);
					decodeRFStage.setReceivedForwardedSrc2(true);
					
					// Lookup rename table to find most recent instance corresponding to source 1
					if (IConstants.ARCHITECTURAL_REGISTER == renameTable[IConstants.PSW_ARCH_REGISTER_INDEX].archOrPhyRegIndicator) {
						int tempReg = renameTable[IConstants.PSW_ARCH_REGISTER_INDEX].regAddress;
						decodeRFStage.getOutputInstruction().setSourcePSWZeroFlagReg(tempReg);
						decodeRFStage.getOutputInstruction().setSourcePSWZeroFlagValue(regFile.getRegister(tempReg).isZeroFlag());
						receivedPSWZeroFlag = true;
					} else {
						int tempReg = renameTable[IConstants.PSW_ARCH_REGISTER_INDEX].regAddress;
						decodeRFStage.getOutputInstruction().setSourcePSWZeroFlagReg(tempReg);
						if (phyRegFile.getRegister(tempReg).getStatus()) {
							//decodeRFStage.getOutputInstruction().setSourceReg1Addr(srcReg1Addr);
							decodeRFStage.getOutputInstruction().setSourcePSWZeroFlagValue(phyRegFile.getRegister(tempReg).isZeroFlag());
							receivedPSWZeroFlag = true;
						} else {
							// Handle forwarding of PSW flag
							Boolean value = getForwardedPSWZeroFlag(tempReg);
							if (null != value) {
								decodeRFStage.getOutputInstruction().setSourcePSWZeroFlagValue(value.booleanValue());
								receivedPSWZeroFlag = true;
							}
						}
					}
					
					allocateCFID = true;
					// Check if CFID can be allocated. If not, stall this instruction
					if (0 == freeCFIDList.size()) {
						isCurrentlyStalled = true;
					}
					
					// Handle branch instructions
					tempStr = new String(decodeRFStage.getInputInstruction().getInstruction());
					break;

					
				}
				
				case IConstants.IOperations.JUMP:
				{
					int srcReg1Addr = Integer.parseInt(instr.getArg1().substring(1));
					int srcReg2Value = Integer.parseInt(instr.getArg2());
					int srcReg1Value = -1;
					
					assertRegisterValidity(srcReg1Addr);
					
					if (!decodeRFStage.isReceivedForwardedSrc1()) {
						// Lookup rename table to find most recent instance corresponding to source 1
						if (IConstants.ARCHITECTURAL_REGISTER == renameTable[srcReg1Addr].archOrPhyRegIndicator) {
							decodeRFStage.getOutputInstruction().setSourceReg1Addr(renameTable[srcReg1Addr].regAddress);
							srcReg1Value = regFile.getRegister(srcReg1Addr).getValue();
							decodeRFStage.getOutputInstruction().setSourceReg1Value(srcReg1Value);
							decodeRFStage.setReceivedForwardedSrc1(true);
							src1Str = new String("R" + renameTable[srcReg1Addr].regAddress);
						} else {
							src1Str = new String("P" + renameTable[srcReg1Addr].regAddress);
							decodeRFStage.getOutputInstruction().setSourceReg1Addr(renameTable[srcReg1Addr].regAddress);
							if (phyRegFile.getRegister(renameTable[srcReg1Addr].regAddress).getStatus()) {
								//decodeRFStage.getOutputInstruction().setSourceReg1Addr(srcReg1Addr);
								srcReg1Value = phyRegFile.getRegister(renameTable[srcReg1Addr].regAddress).getValue();
								decodeRFStage.getOutputInstruction().setSourceReg1Value(srcReg1Value);
								decodeRFStage.setReceivedForwardedSrc1(true);
							} else {
								// Handle forwarding
								Integer value = getForwardedData(renameTable[srcReg1Addr].regAddress);
								if (null != value) {
									decodeRFStage.getOutputInstruction().setSourceReg1Addr(srcReg1Addr);
									decodeRFStage.getOutputInstruction().setSourceReg1Value(value);
									decodeRFStage.setReceivedForwardedSrc1(true);
								}
							}
						}
					}

					decodeRFStage.getOutputInstruction().setSourceReg2Value(srcReg2Value);
					decodeRFStage.setReceivedForwardedSrc2(true);
					
					allocateCFID = true;
					
					// Check if CFID can be allocated. If not, stall this instruction
					if (0 == freeCFIDList.size()) {
						isCurrentlyStalled = true;
					}
					
					tempStr = new String(instr.getOpCode() + ", " + src1Str + ", #" + srcReg2Value);
					break;
				}

				case IConstants.IOperations.HALT:
				{
					// Handle HALT
					fetchStage.getInputInstruction().setProgramCounter(-1);
					fetchStage.getOutputInstruction().reset();

					tempStr = new String(decodeRFStage.getInputInstruction().getInstruction());
					break;
				}

			}
		} catch (NumberFormatException e) {
			throw new InvalidInstructionFormatException("Invalid numeric value specified in instruction: " + instructionStr);
		}

		if (!decodeRFStage.isReceivedRenamedInstruction()) {
			tempStr = tempStr + "  (" + decodeRFStage.getInputInstruction().getInstruction() + ")";
			decodeRFStage.getOutputInstruction().setInstruction(tempStr);
			decodeRFStage.setReceivedRenamedInstruction(true);
		}
		
		// Check if IQ entry can be obtained
		if (!IConstants.IOperations.HALT.equalsIgnoreCase(instr.getOpCode()) && IConstants.ISSUE_QUEUE_CAPACITY <= issueQueue.size()) {
			isCurrentlyStalled = true;
		}

		// Check if ROB entry can be obtained
		if (rob.isFull()) {
			isCurrentlyStalled = true;
		}
		
		if (false == isCurrentlyStalled) {
			
			// Set up ROB entry
			ROBEntry robEntry = new ROBEntry();
			copyData(robEntry, decodeRFStage.getOutputInstruction());
			robEntry.setResultReady(false);
			robEntry.setArchRegAddr(decodeRFStage.getArchRegAddr());
			robEntry.setAllocated(true);
			//robEntry.setLsqIndex(lsqIndex);
			robEntry.setDispatchCycle(Stats.getCycle() + 1);			
			robIndex = rob.getTail();
			rob.addEntry(robEntry);

			// Set up LSQ entry
			if (IConstants.IOperations.STORE.equalsIgnoreCase(instr.getOpCode())) {
				
				lsqIndex = Stats.getCycle() + 1; // Currently use cycle count as 
				LSQEntry lsqEntry = new LSQEntry(lsqIndex);
				copyData(lsqEntry, decodeRFStage.getOutputInstruction());
				lsqEntry.setTargetMemoryValue(decodeRFStage.getOutputInstruction().getSourceReg1Value());
				lsqEntry.setTargetMemoryValueReady(decodeRFStage.isReceivedForwardedSrc1());
				lsqEntry.setDispatchCycle(Stats.getCycle() + 1);			
				lsqEntry.setRobIndex(robIndex);
				lsqEntry.setAllocated(true);;
				lsq.addEntry(lsqEntry);
			} else if (IConstants.IOperations.LOAD.equalsIgnoreCase(instr.getOpCode())) {
				// Set up LSQ entry
				lsqIndex = Stats.getCycle() + 1; // Currently use cycle count as 
				LSQEntry lsqEntry = new LSQEntry(lsqIndex);
				copyData(lsqEntry, decodeRFStage.getOutputInstruction());
				lsqEntry.setDispatchCycle(Stats.getCycle() + 1);			
				lsqEntry.setRobIndex(robIndex);
				lsqEntry.setAllocated(true);
				lsq.addEntry(lsqEntry);
			}
			
			if (!IConstants.IOperations.HALT.equalsIgnoreCase(instr.getOpCode())) {
				// Set up IQ Entry 
				IQEntry iqEntry = new IQEntry();
				copyData(iqEntry, decodeRFStage.getOutputInstruction());
				iqEntry.setFuType(functionalUnit);
				iqEntry.setLsqIndex(lsqIndex);
				iqEntry.setRobIndex(robIndex);
				iqEntry.setSrc1Status(decodeRFStage.isReceivedForwardedSrc1());
				iqEntry.setSrc2Status(decodeRFStage.isReceivedForwardedSrc2());
				iqEntry.setPSWZeroFlagStatus(receivedPSWZeroFlag);
				iqEntry.setStatus(false);
				iqEntry.setDispatchCycle(Stats.getCycle() + 1);			
				issueQueue.add(iqEntry);
			}

			// If it is a branch instruction, allocate a new CFID and copy it into global lastCFID
			if (allocateCFID) {
				Integer newCFID = freeCFIDList.remove(0);
				CFInstructionInfo cfInfo = new CFInstructionInfo(Stats.getCycle() + 1, newCFID, robIndex,
						decodeRFStage.getInputInstruction().getInstruction());
				CFInstructionOrder.add(cfInfo);
				this.lastCFID = newCFID.intValue();
				
				// We also need to take backup of the current rename table and physical register file.
				rob.getROBEntry(robIndex).backupPhysicalRegisterFile(phyRegFile);
				rob.getROBEntry(robIndex).backupRenameTable(renameTable);
			}
	
			// If it is an arithmetic instruction, set destination register as the register holding the most recent
			// value of PSW flag zero
			if (isArithmeticInstr) {
				renameTable[IConstants.PSW_ARCH_REGISTER_INDEX].regAddress = decodeRFStage.getOutputInstruction().getDestRegAddr();
				renameTable[IConstants.PSW_ARCH_REGISTER_INDEX].archOrPhyRegIndicator = IConstants.PHYSICAL_REGISTER;
			}
			
			decodeRFStage.setReceivedForwardedSrc1(false);
			decodeRFStage.setReceivedForwardedSrc2(false);
			decodeRFStage.setReceivedForwardedPSW(false);
			decodeRFStage.setReceivedRenamedInstruction(false);
			decodeRFStage.setReceivedArchRegAddr(false);
			decodeRFStage.setArchRegAddr(-1);
			
			decodeRFStage.setStalled(false);
			fetchStage.setStalled(false);
			
			// Copy data from fetch into input of decodeRF for next cycle
			copyData(decodeRFStage.getInputInstruction(), fetchStage.getOutputInstruction());
		} else {
			decodeRFStage.setStalled(true);
			fetchStage.setStalled(true);
		}

		return;
	}
	
	private void renameRegister(int destReg, int phyRegIndex) {
		PhysicalRegister temp = phyRegFile.getRegister(phyRegIndex);
		temp.reset();
		temp.setAllocated(true);
		temp.setStatus(false);
		phyRegFile.setRegister(phyRegIndex, temp);
		
		// Record the previous physical reg and mark its renamed bit
		// It is cleared when this phy reg is allocated next time to some other arch reg.(Line 2 of this function resets it)
		int renamedRegAddress = renameTable[destReg].regAddress;
		if (IConstants.PHYSICAL_REGISTER == renameTable[destReg].archOrPhyRegIndicator) {
			phyRegFile.getRegister(renamedRegAddress).setRenamed(true);
		}
		
		// Update rename table
		renameTable[destReg].regAddress = phyRegIndex;
		renameTable[destReg].archOrPhyRegIndicator = IConstants.PHYSICAL_REGISTER;
	}
	
	private Integer getForwardedData(int srcReg) {
	
		if (srcReg == addStage.getOutputInstruction().getDestRegAddr()) {
			return new Integer(addStage.getOutputInstruction().getDestRegValue());
		}
		
		if (srcReg == mul2Stage.getOutputInstruction().getDestRegAddr()) {
			return new Integer(mul2Stage.getOutputInstruction().getDestRegValue());
		}
		
		if (srcReg == div4Stage.getOutputInstruction().getDestRegAddr()) {
			return new Integer(div4Stage.getOutputInstruction().getDestRegValue());
		}
		
		if (srcReg == memoryStage.getOutputInstruction().getDestRegAddr()) {
			return new Integer(memoryStage.getOutputInstruction().getTargetMemoryValue());
		}

		// Forwarding from the LOAD instruction that picked up the memory value from STORE ahead of it in LSQ
		if (srcReg == lsq.getTempHeadEntry().getDestRegAddr() && lsq.getTempHeadEntry().isTargetMemoryValueReady()) {
			return new Integer(lsq.getTempHeadEntry().getTargetMemoryValue());
		}
		
		return null;
	}

	private Boolean getForwardedPSWZeroFlag(int srcReg) {
		
		if (srcReg == addStage.getOutputInstruction().getDestRegAddr()) {
			return addStage.getOutputInstruction().isSourcePSWZeroFlagValue();
		}
		
		if (srcReg == mul2Stage.getOutputInstruction().getDestRegAddr()) {
			return mul2Stage.getOutputInstruction().isSourcePSWZeroFlagValue();
		}
		
		if (srcReg == div4Stage.getOutputInstruction().getDestRegAddr()) {
			return div4Stage.getOutputInstruction().isSourcePSWZeroFlagValue();
		}
				
		return null;
	}

	private void restoreRenameTable(RenameTableEntry[] backupRenameTable) {
		// Perform cloning or deep copy while taking backup
		int size = backupRenameTable.length;
		
		renameTable = new RenameTableEntry[size];
		for (int cnt = 0; cnt < size; cnt++) {
			renameTable[cnt] = new RenameTableEntry(backupRenameTable[cnt].getRegAddress(),
					backupRenameTable[cnt].getArchOrPhyRegIndicator());
		}
	}
	
	private void restorePhyisicalRegisterFile(PhysicalRegisterFile backupphyRegFile) {
		// Perform cloning or deep copy while taking backup
		phyRegFile = new PhysicalRegisterFile(backupphyRegFile);
	}
	
	private void processROB() throws InterruptedException {

		this.committedInstructions = "";
		StringBuilder tempStr = new StringBuilder("Commit:");
		
		for (int level = 0; level < 2; level++) {
			if (rob.isEmpty()) {
				return;
			}

			ROBEntry entry = rob.peek();
			if (entry.isAllocated()) {
				switch (entry.getOpCode()) {
					case IConstants.IOperations.LOAD:
					case IConstants.IOperations.ADD:
					case IConstants.IOperations.SUB:
					case IConstants.IOperations.MUL:
					case IConstants.IOperations.DIV:
					case IConstants.IOperations.AND:
					case IConstants.IOperations.OR:
					case IConstants.IOperations.EXOR:
					case IConstants.IOperations.MOVC:
					{
						if (!entry.isResultReady()) {
							// Cannot retire any instruction as instruction at the head has not completed.
							return;
						}
						
						// Copy the value of physical register into architectural register
						regFile.getRegister(entry.getArchRegAddr()).setValue(
								phyRegFile.getRegister(entry.getDestRegAddr()).getValue());
						regFile.getRegister(entry.getArchRegAddr()).setStatus(true);
						
						// Free the renamed physical register and make changes in rename table to indicate that
						// most recent value is with the architectural register itself
						phyRegFile.getRegister(entry.getDestRegAddr()).setAllocated(false);
						
						// If this physical reg is not renamed yet, it is still the most recent instance of the
						// architectural register. So, update rename table.
						// If it is renamed, do not update rename table.
						if (false == phyRegFile.getRegister(entry.getDestRegAddr()).isRenamed()) {
							renameTable[entry.getArchRegAddr()].regAddress = entry.getArchRegAddr();
							renameTable[entry.getArchRegAddr()].archOrPhyRegIndicator = IConstants.ARCHITECTURAL_REGISTER;
						}
						
						//+ Perform this update to all the rename table backups stored with entries in ROB dispatched after this instruction
//						boolean cfInstrFound = false;
						int cfInstrOrderIndex = -1;
						int cfIdTag = entry.getTagCFID();
						cfIdTag = cfIdTag + 1;
						for (int cnt = 0; cnt < CFInstructionOrder.size(); cnt++) {
							// Search for the CFID in the CF Instruction order queue. Ideally it should be at the 0th location i.e. head
							if (cfIdTag == CFInstructionOrder.get(cnt).getCFID()) {
								cfInstrOrderIndex = cnt;
//								cfInstrFound = true;
								break;
							}
						}
						
						if (-1 != cfInstrOrderIndex) {
							for (int cnt = cfInstrOrderIndex; cnt < CFInstructionOrder.size(); cnt++) {
								int tempRobIndex = CFInstructionOrder.get(cnt).getRobIndex();
								
								PhysicalRegisterFile tempPhyRegFile = rob.getROBEntry(tempRobIndex).getPhyRegFileBackup();
								tempPhyRegFile.getRegister(entry.getDestRegAddr()).setAllocated(false);

								if (false == tempPhyRegFile.getRegister(entry.getDestRegAddr()).isRenamed()) {
								//if (false == phyRegFile.getRegister(entry.getDestRegAddr()).isRenamed()) {
									RenameTableEntry[] tempRenameTable = rob.getROBEntry(tempRobIndex).getRenameTableBackup();
									tempRenameTable[entry.getArchRegAddr()].regAddress = entry.getArchRegAddr();
									tempRenameTable[entry.getArchRegAddr()].archOrPhyRegIndicator = IConstants.ARCHITECTURAL_REGISTER;
								//}
								}
							}
						}
						//-
						
						// Handle renaming of PSW produced by ADD, SUB, MUL or DIV instructions
						if (
								IConstants.IOperations.ADD.equalsIgnoreCase(entry.getOpCode()) 			||
								IConstants.IOperations.SUB.equalsIgnoreCase(entry.getOpCode())			||								
								IConstants.IOperations.MUL.equalsIgnoreCase(entry.getOpCode()) 			||
								IConstants.IOperations.DIV.equalsIgnoreCase(entry.getOpCode())								
								) {
							// If physical register retiring with this instruction still holds the most recent value of PSW flag,
							// update rename table to indicate that the most recent PSW value is with architectural register.
							if (
								IConstants.PHYSICAL_REGISTER == renameTable[IConstants.PSW_ARCH_REGISTER_INDEX].getArchOrPhyRegIndicator() &&
								entry.getDestRegAddr() == renameTable[IConstants.PSW_ARCH_REGISTER_INDEX].getRegAddress()) {
								// Copy the PSW zero flag value into architectural register
								regFile.getRegister(entry.getArchRegAddr()).setZeroFlag(
										phyRegFile.getRegister(entry.getDestRegAddr()).isZeroFlag());
								
								// Set the flag in rename table indicating that PSW is with architectural register
								renameTable[IConstants.PSW_ARCH_REGISTER_INDEX].setArchOrPhyRegIndicator(
										IConstants.ARCHITECTURAL_REGISTER);
								
								// Copy the address of the above architectural register into rename table
								renameTable[IConstants.PSW_ARCH_REGISTER_INDEX].setRegAddress(entry.getArchRegAddr());
							}

							// Update backups
							//+ Perform this update to all the rename table backups stored with entries in ROB dispatched after this instruction
							if (-1 != cfInstrOrderIndex) {
								for (int cnt = cfInstrOrderIndex; cnt < CFInstructionOrder.size(); cnt++) {
									int tempRobIndex = CFInstructionOrder.get(cnt).getRobIndex();
									
									RenameTableEntry[] tempRenameTable = rob.getROBEntry(tempRobIndex).getRenameTableBackup();
									if (
										IConstants.PHYSICAL_REGISTER == tempRenameTable[IConstants.PSW_ARCH_REGISTER_INDEX].getArchOrPhyRegIndicator() &&
										entry.getDestRegAddr() == tempRenameTable[IConstants.PSW_ARCH_REGISTER_INDEX].getRegAddress()) {
										
										// Set the flag in rename table indicating that PSW is with architectural register
										tempRenameTable[IConstants.PSW_ARCH_REGISTER_INDEX].setArchOrPhyRegIndicator(
												IConstants.ARCHITECTURAL_REGISTER);
										
										// Copy the address of the above architectural register into rename table
										tempRenameTable[IConstants.PSW_ARCH_REGISTER_INDEX].setRegAddress(entry.getArchRegAddr());
									}
								}
							}
							//-
							
						}
						
						break;
					}
					
					case IConstants.IOperations.STORE:
					{
						if (!entry.isResultReady()) {
							// Cannot retire any instruction as instruction at the head has not completed.
							return;
						}
						
						// We don't have to handle or save anything in case of STORE instruction.
						break;
					}
					
					case IConstants.IOperations.BNZ:
					case IConstants.IOperations.BZ:
					case IConstants.IOperations.JUMP:
					case IConstants.IOperations.JAL:
					{						
						if (!entry.isResultReady()) {
							// Cannot retire any instruction as instruction at the head has not completed.
							return;
						}
						
						// Free the CFID
						int CFLabel = entry.getDispatchCycle();
						
						for (int cnt = 0; cnt < CFInstructionOrder.size(); cnt++) {
							// Search for the CFID in the CF Instruction order queue. Ideally it should be at the 0th location i.e. head
							if (CFLabel == CFInstructionOrder.get(cnt).getDispatchCycleLabel())
							{
								// Remove it from CF Instruction order queue and add it to free list of CFIDs
								CFInstructionInfo cfInfo = CFInstructionOrder.remove(cnt);
								freeCFIDList.add(cfInfo.getCFID());
							}
						}
						
						if (IConstants.IOperations.JAL.equalsIgnoreCase(entry.getOpCode())) {
							// Copy the value of physical register into architectural register
							regFile.getRegister(entry.getArchRegAddr()).setValue(
									phyRegFile.getRegister(entry.getDestRegAddr()).getValue());
							regFile.getRegister(entry.getArchRegAddr()).setStatus(true);
							
							// Free the renamed physical register and make changes in rename table to indicate that
							// most recent value is with the architectural register itself
							phyRegFile.getRegister(entry.getDestRegAddr()).setAllocated(false);
							
							// If this physical reg is not renamed yet, it is still the most recent instance of the
							// architectural register. So, update rename table.
							// If it is renamed, do not update rename table.
							if (false == phyRegFile.getRegister(entry.getDestRegAddr()).isRenamed()) {
								renameTable[entry.getArchRegAddr()].regAddress = entry.getArchRegAddr();
								renameTable[entry.getArchRegAddr()].archOrPhyRegIndicator = IConstants.ARCHITECTURAL_REGISTER;
							}
							
							//+ Perform this update to all the rename table backups as well dispatched after this instruction
//							boolean cfInstrFound = false;
							int cfInstrOrderIndex = -1;
							int cfIdTag = entry.getTagCFID();
							cfIdTag = cfIdTag + 1;
							for (int cnt = 0; cnt < CFInstructionOrder.size(); cnt++) {
								// Search for the CFID in the CF Instruction order queue. Ideally it should be at the 0th location i.e. head
								if (cfIdTag == CFInstructionOrder.get(cnt).getCFID()) {
									cfInstrOrderIndex = cnt;
//									cfInstrFound = true;
									break;
								}
							}
							
							// Update physical register file backup to free physical register and update rename table backup
							if (-1 != cfInstrOrderIndex) {
								for (int cnt = cfInstrOrderIndex; cnt < CFInstructionOrder.size(); cnt++) {
									int tempRobIndex = CFInstructionOrder.get(cnt).getRobIndex();
									
									PhysicalRegisterFile tempPhyRegFile = rob.getROBEntry(tempRobIndex).getPhyRegFileBackup();
									tempPhyRegFile.getRegister(entry.getDestRegAddr()).setAllocated(false);

									if (false == tempPhyRegFile.getRegister(entry.getDestRegAddr()).isRenamed()) {
									//if (false == phyRegFile.getRegister(entry.getDestRegAddr()).isRenamed()) {
										RenameTableEntry[] tempRenameTable = rob.getROBEntry(tempRobIndex).getRenameTableBackup();
										tempRenameTable[entry.getArchRegAddr()].regAddress = entry.getArchRegAddr();
										tempRenameTable[entry.getArchRegAddr()].archOrPhyRegIndicator = IConstants.ARCHITECTURAL_REGISTER;
									//}
									}
								}
							}
							//-

						}
						
						break;
					}
					
					case IConstants.IOperations.HALT:
					{
//						if (0 < level) {
//							// TODO: Verify this, I don't think this needs to be handled. Even if we dont handle it, it wont generate a problem
//							// HALT is a special case and it can not retire when it is not at the head of rob
//							return;
//						}
						
						// It may happen that last STORE operation is still going on but its ROB entry
						// has been committed as soon as the memory operation started. So, wait for the
						// memory operation to complete.
						if (memoryStage.isBusy()) {
							return;
						} 

						// Clear memory stage output as we will end simulation here but memory stage will not be
						// executed and last output may remain there forever
						memoryStage.getOutputInstruction().reset();
						addStage.getOutputInstruction().reset();
						mul2Stage.getOutputInstruction().reset();
						div4Stage.getOutputInstruction().reset();
						// Similarly clear decodeRFStage output
						decodeRFStage.getOutputInstruction().reset();
						
						throw new InterruptedException("Encountered HALT instruction while retiring instructions from the head of ROB");
					}
						
				}
		
				tempStr.append("\n");
				tempStr.append("     > " +
						("(I" + (codeMem.getFileLineNumber(entry.getProgramCounter()) - 1) + ")\t") +
						(entry.getInstruction()));
				
				this.committedInstructions = tempStr.toString();

				Stats.incrementCommittedInstructions();
				
				// Remove the entry that was committed just now.
				rob.remove();
			} else {
				rob.remove();
				// It was NOP may, so decrement the level as we dont want to waste cycle on processing NOP
				level--;
			}
		}
		
//		if ("Commit:".length() == tempStr.length()) {
//			tempStr = new StringBuilder("");
//		}
//
//		this.committedInstructions = tempStr.toString();
	}
	
	private void processLSQ() {
	
		LSQEntry temp;
		
		// TODO: Get forwarded data from last stages of FU. Currently not needed as described in project description 
		
		// Check if temporary head location has valid instruction.
		if (lsq.tempHeadEntry.isAllocated() && lsq.tempHeadEntry.isLoadInstruction()) {
			// Check if it is LOAD which has got value calculated from previous STORE
			if (lsq.tempHeadEntry.isTargetMemoryValueReady()) {
				// Clear the entry as forwarding has already been done 
				lsq.tempHeadEntry.reset();
			} else {
				// This block of code will never be executed after changing LSQ to arraylist, So ignore it or remove it
				// The entry at temp head wants access to memory stage
				copyData(memoryStage.getInputInstruction(), lsq.tempHeadEntry);
				lsq.tempHeadEntry.reset();
				return;
			}
		}

		// Check if memory stage is busy
		if (memoryStage.isBusy()) {
			return;
		}
		
		// It means no LOAD bypassing has happened in the previous cycle;so, now entry at the head
		// of the LSQ gets chance to move ahead to memory stage
		do {
			temp = lsq.peek();
			if (null == temp) {
				// LSQ is empty. So reset input of memory stage and return
				memoryStage.getInputInstruction().reset();
				return;
			}
			
			if (false == temp.isAllocated()) {
				lsq.remove();
				continue;
			}
			
			break;
		} while (true);
		
		boolean ready = false;
		int robIndex = -1;
		
		switch (temp.getOpCode()) {
			case IConstants.IOperations.LOAD:
			{
				ready = temp.isTargetMemoryAddressReady();
				break;
			}
				
			case IConstants.IOperations.STORE:
			{
				robIndex = temp.getRobIndex();
				// STORE can move ahead to Mem stage only if ROB entry for the STORE is at the ROB head
				if (rob.getHead() == robIndex) {
					// Check if it has target memory address calculated and if the value to be written to this
					// memory location is ready
					ready = temp.isTargetMemoryAddressReady() && temp.isTargetMemoryValueReady();
					
					if (ready) {
						// Mark value of Physical register as valid in ROB entry.
						// so that ROB entry will retire without waiting for the mem operation to complete.
						// This is assuming that memory operation can never fail.
						rob.getROBEntry(temp.getRobIndex()).setResultReady(true);
					}
				}
				
				break;
			}
		}
		
		if (ready) {
			lsq.remove();
			copyData(memoryStage.getInputInstruction(), temp);
		} else {
			memoryStage.getInputInstruction().reset();
		}
		
		return;
	}
	
	private void processIQ() {
		
		// Get forwarded data for all IQ entries
		Integer value;
		for (int index = 0; index < issueQueue.size(); index++) {
			IQEntry temp = issueQueue.get(index);
			if (false == temp.getSrc1Status()) {
				value = getForwardedData(temp.getSourceReg1Addr());
				if (null != value) {
					temp.setSourceReg1Value(value.intValue());
					temp.setSrc1Status(true);
				}
			}

			if (false == temp.getSrc2Status()) {
				value = getForwardedData(temp.getSourceReg2Addr());
				if (null != value) {
					temp.setSourceReg2Value(value.intValue());
					temp.setSrc2Status(true);
				}
			}
			
			// If it is BZ or BNZ, check if PSW flag is received. If not, check for forwarding
			if (
					IConstants.IOperations.BZ.equalsIgnoreCase(temp.getOpCode()) ||
					IConstants.IOperations.BNZ.equalsIgnoreCase(temp.getOpCode()) 
					) {
				if (false == temp.getPSWZeroFlagStatus()) {
					Boolean flagValue = getForwardedPSWZeroFlag(temp.getSourcePSWZeroFlagReg());
					if (null != flagValue) {
						temp.setSourcePSWZeroFlagValue(flagValue.booleanValue());
						temp.setPSWZeroFlagStatus(true);
					}
				}
			}
		}
		
		// Get all eligible entries for INTFU
		int selectedEntry = -1;
		List<Integer> eligibleEntries = getEligibleIQEntries(IConstants.IFunctionalUnits.INTFU);
		if (0 < eligibleEntries.size()) {
			selectedEntry = iqEntrySelector(eligibleEntries);
			copyData(addStage.getInputInstruction(), issueQueue.get(selectedEntry));
			issueQueue.remove(selectedEntry);
		} else {
			addStage.getInputInstruction().reset();
		}
		
		// Get all eligible entries for MULFU
		eligibleEntries = getEligibleIQEntries(IConstants.IFunctionalUnits.MULFU);
		if (0 < eligibleEntries.size()) {
			selectedEntry = iqEntrySelector(eligibleEntries);
			copyData(mul1Stage.getInputInstruction(), issueQueue.get(selectedEntry));
			issueQueue.remove(selectedEntry);
		} else {
			mul1Stage.getInputInstruction().reset();
		}

		// Get all eligible entries for DIVFU
		eligibleEntries = getEligibleIQEntries(IConstants.IFunctionalUnits.DIVFU);
		if (0 < eligibleEntries.size()) {
			selectedEntry = iqEntrySelector(eligibleEntries);
			copyData(div1Stage.getInputInstruction(), issueQueue.get(selectedEntry));
			issueQueue.remove(selectedEntry);
		} else {
			div1Stage.getInputInstruction().reset();
		}

		return;
	}
	
	private void handleControlFlowChange() {
		
		if (this.controlFlowAltered) {

			int cfInstrIndex = -1;
			int cfInstrTag = -1;
			
			// Search the tag for branch instruction that caused branch in the CFinstructionOrder
			for (int cnt = 0; cnt < CFInstructionOrder.size(); cnt++) {
				if (this.controlFlowInstrLabel == CFInstructionOrder.get(cnt).getDispatchCycleLabel()) {
					cfInstrIndex = cnt;
					break;
				}
			}
			
			CFInstructionInfo cfInfo;
			//int tempTagCFID = cfInstrIndex;
			
			for (int cnt = CFInstructionOrder.size() - 1; cnt >= cfInstrIndex; cnt--) {
				cfInstrTag = CFInstructionOrder.get(cnt).getCFID();

				// Squash all entries from LSQ
				for (int lsqIndex = 0; lsqIndex < lsq.getLsq().size(); lsqIndex++) {
					if (lsq.getLsq().get(lsqIndex).isAllocated() && cfInstrTag == lsq.getLsq().get(lsqIndex).getTagCFID()) {
						lsq.getLsq().remove(lsqIndex);
						lsqIndex--;
					}
				}
				
				
				// Squash all entries from IQ
				for (int iqIndex = 0; iqIndex < issueQueue.size(); iqIndex++) {
					if (/*issueQueue.get(iqIndex).getStatus() &&*/ cfInstrTag == issueQueue.get(iqIndex).getTagCFID()) {
						issueQueue.remove(iqIndex);
						iqIndex--;
					}
				}
				
				// Squash all entries from DIVFU
				if (cfInstrTag == div1Stage.getInputInstruction().getTagCFID()) {
					div1Stage.getInputInstruction().reset();
					//div1Stage.getOutputInstruction().reset();
				}
				
				if (cfInstrTag == div2Stage.getInputInstruction().getTagCFID()) {
					div2Stage.getInputInstruction().reset();
					//div2Stage.getOutputInstruction().reset();
				}
				
				if (cfInstrTag == div3Stage.getInputInstruction().getTagCFID()) {
					div3Stage.getInputInstruction().reset();
					//div3Stage.getOutputInstruction().reset();
				}
				
				if (cfInstrTag == div4Stage.getInputInstruction().getTagCFID()) {
					div4Stage.getInputInstruction().reset();
					//div4Stage.getOutputInstruction().reset();
				}

				// Squash entries from MULFU
				if (cfInstrTag == mul1Stage.getInputInstruction().getTagCFID()) {
					mul1Stage.getInputInstruction().reset();
					//mul1Stage.getOutputInstruction().reset();
				}

				if (cfInstrTag == mul2Stage.getInputInstruction().getTagCFID()) {
					mul2Stage.getInputInstruction().reset();
					//mul2Stage.getOutputInstruction().reset();
				}

				// Squash entry from INTFU
				if (cfInstrTag == addStage.getInputInstruction().getTagCFID()) {
					addStage.getInputInstruction().reset();
					//addStage.getOutputInstruction().reset();
				}

				// Remove the entry from the CFInstruction order queue
				if (cnt > cfInstrIndex) {
					cfInfo = CFInstructionOrder.remove(cnt);

					// Add the freed CFID to free list of CFIDs
					freeCFIDList.add(cfInfo.getCFID());
				}
			}

//			// Restore rename table and physical register file
//			restoreRenameTable(rob.getROBEntry(controlFlowInstrRobIndex).getRenameTableBackup());
//			restorePhyisicalRegisterFile(rob.getROBEntry(controlFlowInstrRobIndex).getPhyRegFileBackup());
			
			// Finally flush all unwanted entries from ROB and adjust the tail pointer
			int tempRobIndex = this.controlFlowInstrRobIndex;
			tempRobIndex = (tempRobIndex + 1) % rob.getRob().length;
			int tailIndex = rob.getTail();
			if (tempRobIndex == tailIndex) {
				// Do nothing
			} else {
				//tempRobIndex = (tempRobIndex + 1) % rob.getRob().length;
				while (tempRobIndex != tailIndex) {
					rob.getROBEntry(tempRobIndex).reset();
					tempRobIndex = (tempRobIndex + 1) % rob.getRob().length;
				}
				//rob.getROBEntry(tempRobIndex).reset();
				tempRobIndex = (this.controlFlowInstrRobIndex + 1) % rob.getRob().length;
				rob.setTail(tempRobIndex);
			}
			
			// Set this flag to false, to indicate that it is the next cycle that is responsible for handling the
			// control change and decodeRF & fetch can be resumed at the end of this cycle but can not execute in this cycle.
			this.controlFlowAltered = false;
			this.controlFlowInstrLabel = -1;
			this.controlFlowInstrRobIndex = -1;
			this.controlFlowHandled = true;
		}
	}
	
	private List<Integer> getEligibleIQEntries(int fuType) {
		ArrayList<Integer> indices = new ArrayList<Integer>();
		
		for (int index = 0; index < issueQueue.size(); index++) {
			if (
					fuType == issueQueue.get(index).getFuType()		&&
					issueQueue.get(index).getSrc1Status()			&&
					issueQueue.get(index).getSrc2Status()
					) {
				if (
						IConstants.IOperations.BZ.equalsIgnoreCase(issueQueue.get(index).getOpCode()) 			||
						IConstants.IOperations.BNZ.equalsIgnoreCase(issueQueue.get(index).getOpCode())
						) {
					// If it is BZ or BNZ, check if PSW Zero flag is received.
					// Add it to eligible entries only if PSW Zero flag is received.
					if (issueQueue.get(index).getPSWZeroFlagStatus()) {
						indices.add(index);
					}
				} else {
					indices.add(index);
				}
			}
		}
		
		return indices;
	}
	
	private int iqEntrySelector(List<Integer> indices) {
		
		if (null != indices && 0 < indices.size()) {
			int iqIndex = -1;
			int selectedIndex = indices.get(0);
			
			for (int index = 0; index < indices.size(); index++) {
				iqIndex = indices.get(index);
				if (issueQueue.get(iqIndex).getDispatchCycle() < issueQueue.get(selectedIndex).getDispatchCycle()) {
					selectedIndex = iqIndex;
				}
			}
			
			return selectedIndex;
		}
		
		return 0;
	}
	
	private int getFreePhysicalReg() {
	
		for (int index = 0; index < IConstants.PHYSICAL_REGISTER_COUNT; index++) {
			if (false == phyRegFile.getRegister(index).isAllocated()) {
				return index;
			}
		}
		
		return -1;
	}
	

	private void executeEx() throws DivideByZeroException {
		
		div4FU();
		
		multiply2FU();
		
		intFU();

		div3FU();
		
		div2FU();
		
		div1FU();
		
		multiply1FU();

		//sequencer();		
	}

	
	private void intFU() {

		// If the stage is stalled do nothing.
		if (addStage.isStalled()) {
			return;
		}

		// Reset the output latch
		addStage.getOutputInstruction().reset();

		String instructionStr = addStage.getInputInstruction().getInstruction();
		if (null == instructionStr) {

			//memoryStage.getInputInstruction().reset();

			// No instruction in the pipeline stage. Return.
			return;
		}
		
		int destRegAddr = -1;
		int destRegValue = -1;
		//boolean setPSWFlags = false;
		String opCode = addStage.getInputInstruction().getOpCode();

		addStage.getOutputInstruction().setInstruction(
				addStage.getInputInstruction().getInstruction());
		addStage.getOutputInstruction().setProgramCounter(
				addStage.getInputInstruction().getProgramCounter());
		addStage.getOutputInstruction().setOpCode(opCode);
		addStage.getOutputInstruction().setUpdatePSW(
				addStage.getInputInstruction().canUpdatePSW());
		
		boolean updatebackups = false;
		
		switch (opCode) {
			case IConstants.IOperations.LOAD:
			{
				destRegAddr = addStage.getInputInstruction().getDestRegAddr();			
				// Set status of the dest register as "Invalid" to stall all dependent instructions
				// It will set back to "Valid" in WB stage.
				phyRegFile.getRegister(destRegAddr).setStatus(false);
	
				int src1Value = addStage.getInputInstruction().getSourceReg1Value();
				int src2Value = addStage.getInputInstruction().getSourceReg2Value();
				
				int memAddr = src1Value + src2Value;
		
				// Don't set dest reg addr here, otherwise forwarding may give -1 as a value of this dest reg to some
				// instruction as actual dest reg value will be loaded in reg in mem stage
//				addStage.getOutputInstruction().setDestRegAddr(
//						addStage.getInputInstruction().getDestRegAddr());
				addStage.getOutputInstruction().setTargetMemoryAddr(memAddr);
						
				updatebackups = true;
				
				// Write the computed memory address to LSQ entry and
				// Perform LoadBypassing and STORE-LOAD forwarding
				forwardToLSQ(memAddr, addStage.getInputInstruction());
				break;
			}
	
			case IConstants.IOperations.STORE:
			{
//				int srcRegValue = addStage.getInputInstruction().getDestRegValue();
//				int src1Value = addStage.getInputInstruction().getSourceReg1Value();
//				int src2Value = addStage.getInputInstruction().getSourceReg2Value();
//				int memAddr = src1Value + src2Value;
//				addStage.getOutputInstruction().setTargetMemoryAddr(memAddr);
//				addStage.getOutputInstruction().setTargetMemoryValue(srcRegValue);

				int src1Value = addStage.getInputInstruction().getSourceReg1Value();
				int src2Value = addStage.getInputInstruction().getSourceReg2Value();
				int src3Value = addStage.getInputInstruction().getSourceReg3Value();
				
				int memAddr = src2Value + src3Value;

				addStage.getOutputInstruction().setTargetMemoryAddr(memAddr);
				addStage.getOutputInstruction().setTargetMemoryValue(src1Value);
				
				// Perform LoadBypassing and STORE-LOAD forwarding
				forwardToLSQ(memAddr, addStage.getInputInstruction());
				break;	
			}
			
			case IConstants.IOperations.ADD:
			{
				destRegAddr = addStage.getInputInstruction().getDestRegAddr();
				// Set status of the dest register as "Invalid" to stall all dependent instructions
				// It will set back to "Valid" in WB stage.
				phyRegFile.getRegister(destRegAddr).setStatus(false);
				//regFile.getPSW().setStatus(false);
	
				int src1Value = addStage.getInputInstruction().getSourceReg1Value();
				int src2Value = addStage.getInputInstruction().getSourceReg2Value();
				
				destRegValue = src1Value + src2Value;
				//setPSWFlags = true;
				
				addStage.getOutputInstruction().setDestRegAddr(destRegAddr);
				addStage.getOutputInstruction().setDestRegValue(destRegValue);
				
				// Write value to Physical register and mark it as Valid.
				phyRegFile.getRegister(destRegAddr).setValue(
						destRegValue);
				
				// Write the value of PSW zero flag in physical register
				if (0 == destRegValue) {
					phyRegFile.getRegister(destRegAddr).setZeroFlag(true);
					// Also copy this value to output for data forwarding
					addStage.getOutputInstruction().setSourcePSWZeroFlagValue(true);	
				} else {
					phyRegFile.getRegister(destRegAddr).setZeroFlag(false);
					// Also copy this value to output for data forwarding
					addStage.getOutputInstruction().setSourcePSWZeroFlagValue(false);	
				}
				
				phyRegFile.getRegister(destRegAddr).setStatus(true);
				
				updatebackups = true;
				
				// Mark value of Physical register as valid in ROB entry.
				//rob.getROBEntry(addStage.getInputInstruction().getRobIndex()).setResultReady(true);
				break;
			}
	
			case IConstants.IOperations.SUB:
			{
				destRegAddr = addStage.getInputInstruction().getDestRegAddr();
				// Set status of the dest register as "Invalid" to stall all dependent instructions
				// It will set back to "Valid" in WB stage.
				phyRegFile.getRegister(destRegAddr).setStatus(false);
				//regFile.getPSW().setStatus(false);
	
				int src1Value = addStage.getInputInstruction().getSourceReg1Value();
				int src2Value = addStage.getInputInstruction().getSourceReg2Value();
	
				destRegValue = src1Value - src2Value;
				//setPSWFlags = true;

				addStage.getOutputInstruction().setDestRegAddr(destRegAddr);
				addStage.getOutputInstruction().setDestRegValue(destRegValue);
				
				// Write value to Physical register and mark it as Valid.
				phyRegFile.getRegister(destRegAddr).setValue(
						destRegValue);
				
				// Write the value of PSW zero flag in physical register
				if (0 == destRegValue) {
					phyRegFile.getRegister(destRegAddr).setZeroFlag(true);
					// Also copy this value to output for data forwarding
					addStage.getOutputInstruction().setSourcePSWZeroFlagValue(true);
				} else {
					phyRegFile.getRegister(destRegAddr).setZeroFlag(false);
					// Also copy this value to output for data forwarding
					addStage.getOutputInstruction().setSourcePSWZeroFlagValue(false);	
				}
				
				phyRegFile.getRegister(destRegAddr).setStatus(true);
				
				updatebackups = true;
				
				// Mark value of Physical register as valid in ROB entry.
				//rob.getROBEntry(addStage.getInputInstruction().getRobIndex()).setResultReady(true);
				
				break;
			}
				
			case IConstants.IOperations.MOVC:
			{
				destRegAddr = addStage.getInputInstruction().getDestRegAddr();
				// Set status of the dest register as "Invalid" to stall all dependent instructions
				// It will set back to "Valid" in WB stage.
				phyRegFile.getRegister(destRegAddr).setStatus(false);
	
				int src1Value = addStage.getInputInstruction().getSourceReg1Value();
	
				destRegValue = src1Value + 0;
	
				addStage.getOutputInstruction().setDestRegAddr(destRegAddr);
				addStage.getOutputInstruction().setDestRegValue(destRegValue);
				
				// Write value to Physical register and mark it as Valid.
				phyRegFile.getRegister(destRegAddr).setValue(
						destRegValue);
				phyRegFile.getRegister(destRegAddr).setStatus(true);
				
				updatebackups = true;
				
				// Mark value of Physical register as valid in ROB entry.
				//rob.getROBEntry(addStage.getInputInstruction().getRobIndex()).setResultReady(true);
				
				break;
			}
	
			case IConstants.IOperations.AND:
			{
				destRegAddr = addStage.getInputInstruction().getDestRegAddr();
				// Set status of the dest register as "Invalid" to stall all dependent instructions
				// It will set back to "Valid" in WB stage.
				phyRegFile.getRegister(destRegAddr).setStatus(false);
				//regFile.getPSW().setStatus(false);
	
				int src1Value = addStage.getInputInstruction().getSourceReg1Value();
				int src2Value = addStage.getInputInstruction().getSourceReg2Value();
				
				destRegValue = src1Value & src2Value;
				//setPSWFlags = true;
	
				addStage.getOutputInstruction().setDestRegAddr(destRegAddr);
				addStage.getOutputInstruction().setDestRegValue(destRegValue);
				
				// Write value to Physical register and mark it as Valid.
				phyRegFile.getRegister(destRegAddr).setValue(
						destRegValue);
				phyRegFile.getRegister(destRegAddr).setStatus(true);
				
				updatebackups = true;
				
				// Mark value of Physical register as valid in ROB entry.
				//rob.getROBEntry(addStage.getInputInstruction().getRobIndex()).setResultReady(true);
				
				break;
			}
				
			case IConstants.IOperations.OR:
			{
				destRegAddr = addStage.getInputInstruction().getDestRegAddr();
				// Set status of the dest register as "Invalid" to stall all dependent instructions
				// It will set back to "Valid" in WB stage.
				phyRegFile.getRegister(destRegAddr).setStatus(false);
				//regFile.getPSW().setStatus(false);
	
				int src1Value = addStage.getInputInstruction().getSourceReg1Value();
				int src2Value = addStage.getInputInstruction().getSourceReg2Value();

				destRegValue = src1Value | src2Value;
				//setPSWFlags = true;
	
				addStage.getOutputInstruction().setDestRegAddr(destRegAddr);
				addStage.getOutputInstruction().setDestRegValue(destRegValue);
				
				// Write value to Physical register and mark it as Valid.
				phyRegFile.getRegister(destRegAddr).setValue(
						destRegValue);
				phyRegFile.getRegister(destRegAddr).setStatus(true);
				
				updatebackups = true;
				
				// Mark value of Physical register as valid in ROB entry.
				//rob.getROBEntry(addStage.getInputInstruction().getRobIndex()).setResultReady(true);
				
				break;
			}
	
			case IConstants.IOperations.EXOR:
			{
				destRegAddr = addStage.getInputInstruction().getDestRegAddr();
				// Set status of the dest register as "Invalid" to stall all dependent instructions
				// It will set back to "Valid" in WB stage.
				phyRegFile.getRegister(destRegAddr).setStatus(false);
				//regFile.getPSW().setStatus(false);
	
				int src1Value = addStage.getInputInstruction().getSourceReg1Value();
				int src2Value = addStage.getInputInstruction().getSourceReg2Value();
					
				destRegValue = src1Value ^ src2Value;
				//setPSWFlags = true;
	
				addStage.getOutputInstruction().setDestRegAddr(destRegAddr);
				addStage.getOutputInstruction().setDestRegValue(destRegValue);
				
				// Write value to Physical register and mark it as Valid.
				phyRegFile.getRegister(destRegAddr).setValue(
						destRegValue);
				phyRegFile.getRegister(destRegAddr).setStatus(true);
				
				updatebackups = true;
				
				// Mark value of Physical register as valid in ROB entry.
				//rob.getROBEntry(addStage.getInputInstruction().getRobIndex()).setResultReady(true);
				
				break;
			}
	
			case IConstants.IOperations.BZ:
			{
				int src1Value = addStage.getInputInstruction().getSourceReg1Value();
	
//				if (true == regFile.getPSW().isZero()) {
				if (true == addStage.getInputInstruction().isSourcePSWZeroFlagValue()) {
					int currentPC = addStage.getInputInstruction().getProgramCounter();
					int newPC = currentPC + src1Value;
					
					// Set new PC
					fetchStage.getInputInstruction().setProgramCounter(newPC);
	
					// Flush output of fetch stage otherwise it will be copied to decodeRF stage at the end of current cycle
					fetchStage.getOutputInstruction().reset();

					// Flush all previous stages
					decodeRFStage.getInputInstruction().reset();
					
					//+
					// Set the label and ROB entry index of this instruction in global variables
					// that will be used by control flow handling logic to squash all unwanted instructions.
					this.controlFlowInstrLabel = addStage.getInputInstruction().getDispatchCycle();
					this.controlFlowInstrRobIndex = addStage.getInputInstruction().getRobIndex();
				
					// Restore rename table and physical register file
					restoreRenameTable(rob.getROBEntry(this.controlFlowInstrRobIndex).getRenameTableBackup());
					restorePhyisicalRegisterFile(rob.getROBEntry(this.controlFlowInstrRobIndex).getPhyRegFileBackup());
					//-

					// Set the flag to indicate control flow change and trigger squashing
					controlFlowAltered = true;
				}
				
				break;
			}
			
			case IConstants.IOperations.BNZ:
			{
				int src1Value = addStage.getInputInstruction().getSourceReg1Value();
	
//				if (true == regFile.getPSW().isZero()) {
				if (false == addStage.getInputInstruction().isSourcePSWZeroFlagValue()) {
					int currentPC = addStage.getInputInstruction().getProgramCounter();
					int newPC = currentPC + src1Value;
					
					// Set new PC
					fetchStage.getInputInstruction().setProgramCounter(newPC);
	
					// Flush output of fetch stage otherwise it will be copied to decodeRF stage at the end of current cycle
					fetchStage.getOutputInstruction().reset();

					// Flush all previous stages
					decodeRFStage.getInputInstruction().reset();

					//+
					// Set the label and ROB entry index of this instruction in global variables
					// that will be used by control flow handling logic to squash all unwanted instructions.
					this.controlFlowInstrLabel = addStage.getInputInstruction().getDispatchCycle();
					this.controlFlowInstrRobIndex = addStage.getInputInstruction().getRobIndex();
				
					// Restore rename table and physical register file
					restoreRenameTable(rob.getROBEntry(this.controlFlowInstrRobIndex).getRenameTableBackup());
					restorePhyisicalRegisterFile(rob.getROBEntry(this.controlFlowInstrRobIndex).getPhyRegFileBackup());
					//-

					// Set the flag to indicate control flow change and trigger squashing
					controlFlowAltered = true;
				}
				
				break;
			}
	
			case IConstants.IOperations.JUMP:
			{
				int srcReg1Value = addStage.getInputInstruction().getSourceReg1Value();
				int srcReg2Value = addStage.getInputInstruction().getSourceReg2Value();
				
				int newPC = srcReg1Value + srcReg2Value;
				//System.out.println("New PC: " + newPC);

				// Set new PC
				fetchStage.getInputInstruction().setProgramCounter(newPC);
	
				// Flush output of fetch stage otherwise it will be copied to decodeRF stage at the end of current cycle
				fetchStage.getOutputInstruction().reset();
				
				// Resume fetch stage as it may be stalled previously. Now we are flushing instruction in D/RF so Fetch should resume.
				//fetchStage.setStalled(false);

//				// Flush all previous stages
//				if (!decodeRFStage.getInputInstruction().getInstruction().contains(IConstants.IOperations.HALT)) {
					decodeRFStage.getInputInstruction().reset();
//				}
				
				//+
				// Set the label and ROB entry index of this instruction in global variables
				// that will be used by control flow handling logic to squash all unwanted instructions.
				this.controlFlowInstrLabel = addStage.getInputInstruction().getDispatchCycle();
				this.controlFlowInstrRobIndex = addStage.getInputInstruction().getRobIndex();
			
				// Restore rename table and physical register file
				restoreRenameTable(rob.getROBEntry(this.controlFlowInstrRobIndex).getRenameTableBackup());
				restorePhyisicalRegisterFile(rob.getROBEntry(this.controlFlowInstrRobIndex).getPhyRegFileBackup());
				//-

				// Set the flag to indicate control flow change and trigger squashing
				controlFlowAltered = true;

				break;
			}
	
			case IConstants.IOperations.JAL:
			{
				destRegAddr = addStage.getInputInstruction().getDestRegAddr();
				int srcReg1Value = addStage.getInputInstruction().getSourceReg1Value();
				int srcReg2Value = addStage.getInputInstruction().getSourceReg2Value();
				
				int newPC = srcReg1Value + srcReg2Value;
				//System.out.println("New PC: " + newPC);
				
				destRegValue = addStage.getInputInstruction().getProgramCounter() + IConstants.INSTRUCTION_SIZE;
				//System.out.println("Register R" + destRegAddr + ": " + destRegValue);

				//+
				// Set the label and ROB entry index of this instruction in global variables
				// that will be used by control flow handling logic to squash all unwanted instructions.
				this.controlFlowInstrLabel = addStage.getInputInstruction().getDispatchCycle();
				this.controlFlowInstrRobIndex = addStage.getInputInstruction().getRobIndex();
			
				// Restore rename table and physical register file
				restoreRenameTable(rob.getROBEntry(this.controlFlowInstrRobIndex).getRenameTableBackup());
				restorePhyisicalRegisterFile(rob.getROBEntry(this.controlFlowInstrRobIndex).getPhyRegFileBackup());
				//-

				// Set status of the dest register as "Invalid" to stall all dependent instructions
				// It will set back to "Valid" in WB stage.
				phyRegFile.getRegister(destRegAddr).setStatus(false);
				
				addStage.getOutputInstruction().setDestRegAddr(destRegAddr);
				addStage.getOutputInstruction().setDestRegValue(destRegValue);

				// Write value to Physical register and mark it as Valid.
				phyRegFile.getRegister(destRegAddr).setValue(
						destRegValue);
				phyRegFile.getRegister(destRegAddr).setStatus(true);
				
				// Set new PC
				fetchStage.getInputInstruction().setProgramCounter(newPC);
	
				// Flush output of fetch stage otherwise it will be copied to decodeRF stage at the end of current cycle
				fetchStage.getOutputInstruction().reset();
				
				// Resume fetch stage as it may be stalled previously. Now we are flushing instruction in D/RF so Fetch should resume.
				//fetchStage.setStalled(false);

//				// Flush all previous stages
//				if (!decodeRFStage.getInputInstruction().getInstruction().contains(IConstants.IOperations.HALT)) {
					decodeRFStage.getInputInstruction().reset();
//				}
				
				// Set the flag to indicate control flow change and trigger squashing
				controlFlowAltered = true;
				updatebackups = true;

				break;
			}
	
			case IConstants.IOperations.HALT:
				break;
	
			default:
				break;
		}

		if (
			!IConstants.IOperations.LOAD.equalsIgnoreCase(addStage.getInputInstruction().getOpCode()) &&
			!IConstants.IOperations.STORE.equalsIgnoreCase(addStage.getInputInstruction().getOpCode())
			) {
			// Mark the result as ready in ROB entry
			// For LOAD/STORE this will be done in memory stage when memory operation completes successfully
			rob.getROBEntry(addStage.getInputInstruction().getRobIndex()).setResultReady(true);
		}
		
		if (true == this.controlFlowAltered) {
			// Set the label and ROB entry index of this instruction in global variables
			// that will be used by control flow handling logic to squash all unwanted instructions.
			this.controlFlowInstrLabel = addStage.getInputInstruction().getDispatchCycle();
			this.controlFlowInstrRobIndex = addStage.getInputInstruction().getRobIndex();
		
			//decodeRFStage.setStalled(true);
			//fetchStage.setStalled(true);
		}
		
		//+ Perform this update to all the rename table backups as they were taken after dispatching this instruction
		// but did not include this update
		if (true == updatebackups) {
			int cfInstrOrderIndex = -1;
			int cfIdTag = addStage.getInputInstruction().getTagCFID();
			cfIdTag = cfIdTag + 1;
			for (int cnt = 0; cnt < CFInstructionOrder.size(); cnt++) {
				// Search for the CFID in the CF Instruction order queue. Ideally it should be at the 0th location i.e. head
				if (cfIdTag == CFInstructionOrder.get(cnt).getCFID()) {
					cfInstrOrderIndex = cnt;
					//cfInstrFound = true;
					break;
				}
			}
			
			if (-1 != cfInstrOrderIndex) {
				for (int cnt = cfInstrOrderIndex; cnt < CFInstructionOrder.size(); cnt++) {
					int tempRobIndex = CFInstructionOrder.get(cnt).getRobIndex();
					
					PhysicalRegisterFile tempPhyRegFile = rob.getROBEntry(tempRobIndex).getPhyRegFileBackup();
					// Write value to Physical register and mark it as Valid.
					tempPhyRegFile.getRegister(destRegAddr).setValue(
							destRegValue);
					tempPhyRegFile.getRegister(destRegAddr).setStatus(true);

					if (
							IConstants.IOperations.ADD.equalsIgnoreCase(opCode) ||
							IConstants.IOperations.SUB.equalsIgnoreCase(opCode) ||
							IConstants.IOperations.MUL.equalsIgnoreCase(opCode) ||
							IConstants.IOperations.DIV.equalsIgnoreCase(opCode)
							) {
						if (0 == destRegValue) {
							tempPhyRegFile.getRegister(destRegAddr).setZeroFlag(true);
						} else {
							tempPhyRegFile.getRegister(destRegAddr).setZeroFlag(false);
						}
					}
					
				}
			}
		}
		//-
//		if (true == setPSWFlags) {
//			if (0 == destRegValue) {
//				addStage.getOutputInstruction().setZeroPSW(true);
//			} else {
//				addStage.getOutputInstruction().setZeroPSW(false);
//			}
//			
//			if (0 > destRegValue) {
//				addStage.getOutputInstruction().setNegativePSW(true);
//			} else {
//				addStage.getOutputInstruction().setNegativePSW(false);
//			}
//			
//			addStage.getOutputInstruction().setCarryPSW(false);
//		}
		// Do not copy output latches into input latches for next stage here,
		// It will be done by sequencer logic 
	}
	
	private void forwardToLSQ(int targetMemoryAddress, InstructionInfo inputInstructionInfo) {
		
		int lsqIndex = inputInstructionInfo.getLsqIndex();
		switch (inputInstructionInfo.getOpCode()) {
			case IConstants.IOperations.STORE:
			{
				// Forward Source 1 value i.e. target memory value
				if (!lsq.getLSQEntry(lsqIndex).isTargetMemoryValueReady()) {
					lsq.getLSQEntry(lsqIndex).setTargetMemoryValue(inputInstructionInfo.getSourceReg1Value());
					lsq.getLSQEntry(lsqIndex).setTargetMemoryValueReady(true);
				}
				
				lsq.getLSQEntry(lsqIndex).setTargetMemoryAddr(targetMemoryAddress);
				lsq.getLSQEntry(lsqIndex).setTargetMemoryAddressReady(true);
				
				
				//+ TODO: remove. For testing purpose
				lsq.getLSQEntry(lsqIndex).setInstruction(lsq.getLSQEntry(lsqIndex).getInstruction() + " [" + targetMemoryAddress + "]");
				
				break;
			}
			
			case IConstants.IOperations.LOAD:			
			{
				lsq.getLSQEntry(lsqIndex).setTargetMemoryAddr(targetMemoryAddress);
				lsq.getLSQEntry(lsqIndex).setTargetMemoryAddressReady(true);
				
				//+ TODO: remove. For testing purpose
				lsq.getLSQEntry(lsqIndex).setInstruction(lsq.getLSQEntry(lsqIndex).getInstruction() + " [" + targetMemoryAddress + "]");

				// Perform LOAD bypassing and STORE-LOAD forwarding
				byPassLoad(lsqIndex);
				
				break;
			}
		}
	}
	
	private void byPassLoad(int lsqIndex) {
		
		boolean canByPass = true;
		//int temp = -1;
		int currentPosition = lsq.getPosition(lsqIndex);
		int tempIndex = currentPosition;
		tempIndex--;

		//+ TODO: Confirm whether this is needed with professor and TA
//		if (0 == currentPosition) {
//			// It means in next cycle, it should be selected for memory operation
//			if (false == memoryStage.isBusy()) {
//				copyData(memoryStage.getInputInstruction(), lsq.getLSQEntry(lsqIndex));
//				lsq.remove();
//				return;
//			}
//		}
		
		ArrayList<LSQEntry> lsqTemp = (ArrayList<LSQEntry>) lsq.getLsq();
//		int lsqSize = lsq.getLsq().size();
		while (tempIndex >= 0) {
//		while (tempIndex != lsq.getHead()) {
			if (lsqTemp.get(tempIndex).isAllocated() && !lsqTemp.get(tempIndex).isLoadInstruction()) {
				if (lsqTemp.get(tempIndex).isTargetMemoryAddressReady()) {
					if (
					lsqTemp.get(tempIndex).isTargetMemoryAddressReady() &&
					lsqTemp.get(tempIndex).getTargetMemoryAddr() == lsq.getLSQEntry(lsqIndex).getTargetMemoryAddr() &&
					lsqTemp.get(tempIndex).isTargetMemoryValueReady()) {
						
						// Perform STORE-LOAD forwarding
						lsq.getLSQEntry(lsqIndex).setTargetMemoryValue(lsqTemp.get(tempIndex).getTargetMemoryValue());
						lsq.getLSQEntry(lsqIndex).setTargetMemoryValueReady(true);
						lsq.setTempHeadEntry(lsq.getLSQEntry(lsqIndex));
						
						// Write value to Physical register and mark it as Valid.
						phyRegFile.getRegister(lsq.getLSQEntry(lsqIndex).getDestRegAddr()).setValue(
								lsq.getLSQEntry(lsqIndex).getTargetMemoryValue());
						phyRegFile.getRegister(lsq.getLSQEntry(lsqIndex).getDestRegAddr()).setStatus(true);
						
						// Write value of Physical register to ROB entry.
						rob.getROBEntry(lsq.getLSQEntry(lsqIndex).getRobIndex()).setResultReady(true);
						
						// Remove the LOAD from LSQ that has got STORE forwarded data.
						//lsq.getLSQEntry(lsqIndex).reset();
						lsqTemp.remove(currentPosition);
						
						// Return as we have forwarded target value from STORE to LOAD, so there is no need for bypassing.
						return;
					}
				} else {
					canByPass = false;
				}
			}
			
			tempIndex--;
		}
		
//		// If bypassing can be done and if memory stage is not busy, perform load bypassing
//		if (canByPass && !memoryStage.isBusy()) {
//			lsq.setTempHeadEntry(lsq.getLSQEntry(lsqIndex));
//			lsq.getLSQEntry(lsqIndex).reset();
//		}
		
		// If bypassing can be done, search the position to insert the LOAD entry
		if (canByPass) {
			tempIndex = 0;
			while (tempIndex < currentPosition) {
				if (lsqTemp.get(tempIndex).isLoadInstruction() && lsqTemp.get(tempIndex).isTargetMemoryAddressReady()) {
					tempIndex++;
					continue;
				}
				
				LSQEntry tempEntry = new LSQEntry(lsq.getLSQEntry(lsqIndex));
				// Remove the entry for LOAD that has bypassed STORE
				lsqTemp.remove(currentPosition);
				
				lsqTemp.add(tempIndex, tempEntry);
				break;
			}
		}
		
		return;
	}
	
	private void multiply1FU() {
		
		// If the stage is stalled do nothing.
		if (mul1Stage.isStalled()) {
			return;
		}

		// Reset the output latch
		mul1Stage.getOutputInstruction().reset();
		
		String instructionStr = mul1Stage.getInputInstruction().getInstruction();
		if (null == instructionStr) {

			// Reset the input of next stage
			mul2Stage.getInputInstruction().reset();

			// No instruction in the pipeline stage. Return.
			return;
		}
		
		String opCode = mul1Stage.getInputInstruction().getOpCode();

		mul1Stage.getOutputInstruction().setInstruction(
				mul1Stage.getInputInstruction().getInstruction());
		mul1Stage.getOutputInstruction().setProgramCounter(
				mul1Stage.getInputInstruction().getProgramCounter());
		mul1Stage.getOutputInstruction().setOpCode(opCode);
		mul1Stage.getOutputInstruction().setUpdatePSW(
				mul1Stage.getInputInstruction().canUpdatePSW());
		mul1Stage.getOutputInstruction().setLsqIndex(
				mul1Stage.getInputInstruction().getLsqIndex());
		mul1Stage.getOutputInstruction().setRobIndex(
				mul1Stage.getInputInstruction().getRobIndex());
		mul1Stage.getOutputInstruction().setTagCFID(
				mul1Stage.getInputInstruction().getTagCFID());
		mul1Stage.getOutputInstruction().setDispatchCycle(
				mul1Stage.getInputInstruction().getDispatchCycle());

		switch (opCode) {
			case IConstants.IOperations.MUL:
			{
				int destRegAddr = mul1Stage.getInputInstruction().getDestRegAddr();
				// Set status of the dest register as "Invalid" to stall all dependent instructions
				// It will set back to "Valid" in WB stage.
				phyRegFile.getRegister(destRegAddr).setStatus(false);
				//regFile.getPSW().setStatus(false);
	
				int src1Value = mul1Stage.getInputInstruction().getSourceReg1Value();
				int src2Value = mul1Stage.getInputInstruction().getSourceReg2Value();
				
				int destRegValue = src1Value * src2Value;
	
				mul1Stage.getOutputInstruction().setDestRegAddr(destRegAddr);
				mul1Stage.getOutputInstruction().setDestRegValue(destRegValue);
				
				break;
			}
	
			case IConstants.IOperations.HALT:
				break;
	
			default:
				break;
		}
		
		// Copy output latches of this stage into input latches of next stage for next cycle,
		// only if the current stage is not stalled
		copyData(mul2Stage.getInputInstruction(), mul1Stage.getOutputInstruction());
	}
	
	private void multiply2FU() {
		// If the stage is stalled do nothing.
		if (mul2Stage.isStalled()) {
			return;
		}

		// Reset the output latch
		mul2Stage.getOutputInstruction().reset();
		
		String instructionStr = mul2Stage.getInputInstruction().getInstruction();
		if (null == instructionStr) {

			//memoryStage.getInputInstruction().reset();

			mul2Stage.getOutputInstruction().reset();

			// No instruction in the pipeline stage. Return.
			return;
		}
		
		int destRegValue = -1;
		//boolean setPSWFlags = false;
		String opCode = mul2Stage.getInputInstruction().getOpCode();

		mul2Stage.getOutputInstruction().setInstruction(
				mul2Stage.getInputInstruction().getInstruction());
		mul2Stage.getOutputInstruction().setProgramCounter(
				mul2Stage.getInputInstruction().getProgramCounter());
		mul2Stage.getOutputInstruction().setOpCode(opCode);
		mul2Stage.getOutputInstruction().setUpdatePSW(
				mul2Stage.getInputInstruction().canUpdatePSW());
		mul2Stage.getOutputInstruction().setLsqIndex(
				mul2Stage.getInputInstruction().getLsqIndex());
		mul2Stage.getOutputInstruction().setRobIndex(
				mul2Stage.getInputInstruction().getRobIndex());
		mul2Stage.getOutputInstruction().setTagCFID(
				mul2Stage.getInputInstruction().getTagCFID());
		mul2Stage.getOutputInstruction().setDispatchCycle(
				mul2Stage.getInputInstruction().getDispatchCycle());
		
		switch (opCode) {
			case IConstants.IOperations.MUL:
			{
				int destRegAddr = mul2Stage.getInputInstruction().getDestRegAddr();
//				// Set status of the dest register as "Invalid" to stall all dependent instructions
//				// It will set back to "Valid" in WB stage.
//				regFile.getRegister(destRegAddr).setStatus(false);
//				regFile.getPSW().setStatus(false);

				destRegValue = mul2Stage.getInputInstruction().getDestRegValue();
				destRegValue = destRegValue * 1;
				//setPSWFlags = true;
				
				// Write value to Physical register and mark it as Valid.
				phyRegFile.getRegister(mul2Stage.getInputInstruction().getDestRegAddr()).setValue(
						destRegValue);
				
				//+ Perform this update to all the rename table backups as well dispatched after this instruction
				int cfInstrOrderIndex = -1;
				int cfIdTag = mul2Stage.getInputInstruction().getTagCFID();
				cfIdTag = cfIdTag + 1;
				for (int cnt = 0; cnt < CFInstructionOrder.size(); cnt++) {
					// Search for the CFID in the CF Instruction order queue. Ideally it should be at the 0th location i.e. head
					if (cfIdTag == CFInstructionOrder.get(cnt).getCFID()) {
						cfInstrOrderIndex = cnt;
						//cfInstrFound = true;
						break;
					}
				}
				
				if (-1 != cfInstrOrderIndex) {
					for (int cnt = cfInstrOrderIndex; cnt < CFInstructionOrder.size(); cnt++) {
						int tempRobIndex = CFInstructionOrder.get(cnt).getRobIndex();
						
						PhysicalRegisterFile tempPhyRegFile = rob.getROBEntry(tempRobIndex).getPhyRegFileBackup();
						// Write value to Physical register and mark it as Valid.
						tempPhyRegFile.getRegister(destRegAddr).setValue(
								destRegValue);
						tempPhyRegFile.getRegister(destRegAddr).setStatus(true);
						
						if (0 == destRegValue) {
							tempPhyRegFile.getRegister(destRegAddr).setZeroFlag(true);
						} else {
							tempPhyRegFile.getRegister(destRegAddr).setZeroFlag(false);
						}

					}
				}
				//-
				
				// Write the value of PSW zero flag in physical register
				if (0 == destRegValue) {
					phyRegFile.getRegister(destRegAddr).setZeroFlag(true);
					// Also copy this value to output for data forwarding
					mul2Stage.getOutputInstruction().setSourcePSWZeroFlagValue(true);
				} else {
					phyRegFile.getRegister(destRegAddr).setZeroFlag(false);
					// Also copy this value to output for data forwarding
					mul2Stage.getOutputInstruction().setSourcePSWZeroFlagValue(false);
				}
				
				phyRegFile.getRegister(mul2Stage.getInputInstruction().getDestRegAddr()).setStatus(true);
				
				mul2Stage.getOutputInstruction().setDestRegAddr(destRegAddr);
				mul2Stage.getOutputInstruction().setDestRegValue(destRegValue);
				
				break;
			}

			case IConstants.IOperations.HALT:
				break;
	
			default:
				break;
		}
		
		// Mark the result as ready in ROB entry
		rob.getROBEntry(mul2Stage.getInputInstruction().getRobIndex()).setResultReady(true);

		// Do not copy output latches into input latches for next stage here,
		// It will be done by sequencer logic 
	}
	
	private void div1FU() throws DivideByZeroException {
		
		// If the stage is stalled do nothing.
		if (div1Stage.isStalled()) {
			return;
		}

		// Reset the output latch
		div1Stage.getOutputInstruction().reset();
		
		String instructionStr = div1Stage.getInputInstruction().getInstruction();
		if (null == instructionStr) {

			// Reset the input of next stage
			div2Stage.getInputInstruction().reset();

			// No instruction in the pipeline stage. Return.
			return;
		}
		
		String opCode = div1Stage.getInputInstruction().getOpCode();

		div1Stage.getOutputInstruction().setInstruction(
				div1Stage.getInputInstruction().getInstruction());
		div1Stage.getOutputInstruction().setProgramCounter(
				div1Stage.getInputInstruction().getProgramCounter());
		div1Stage.getOutputInstruction().setOpCode(opCode);
		div1Stage.getOutputInstruction().setUpdatePSW(
				div1Stage.getInputInstruction().canUpdatePSW());
		div1Stage.getOutputInstruction().setLsqIndex(
				div1Stage.getInputInstruction().getLsqIndex());
		div1Stage.getOutputInstruction().setRobIndex(
				div1Stage.getInputInstruction().getRobIndex());
		div1Stage.getOutputInstruction().setTagCFID(
				div1Stage.getInputInstruction().getTagCFID());
		div1Stage.getOutputInstruction().setDispatchCycle(
				div1Stage.getInputInstruction().getDispatchCycle());

		switch (opCode) {
			case IConstants.IOperations.DIV:
			{
				int destRegAddr = div1Stage.getInputInstruction().getDestRegAddr();
				// Set status of the dest register as "Invalid" to stall all dependent instructions
				// It will be set back to "Valid" in WB stage.
				phyRegFile.getRegister(destRegAddr).setStatus(false);
				//regFile.getPSW().setStatus(false);
	
				int src1Value = div1Stage.getInputInstruction().getSourceReg1Value();
				int src2Value = div1Stage.getInputInstruction().getSourceReg2Value();
				
				if (0 == src2Value) {
					// Handle divide by zero case
					throw new DivideByZeroException("Attempting to divide by zero");
				}
				
				int destRegValue = src1Value / src2Value;
	
				div1Stage.getOutputInstruction().setDestRegAddr(destRegAddr);
				div1Stage.getOutputInstruction().setDestRegValue(destRegValue);
				
				break;
			}
	
			case IConstants.IOperations.HALT:
				break;
	
			default:
				break;
		}
		
		// Copy output latches of this stage into input latches of next stage for next cycle,
		// only if the current stage is not stalled
		copyData(div2Stage.getInputInstruction(), div1Stage.getOutputInstruction());
	}

	private void div2FU() throws DivideByZeroException {
		
		// If the stage is stalled do nothing.
		if (div2Stage.isStalled()) {
			return;
		}

		// Reset the output latch
		div2Stage.getOutputInstruction().reset();
		
		String instructionStr = div2Stage.getInputInstruction().getInstruction();
		if (null == instructionStr) {

			// Reset the input of next stage
			div3Stage.getInputInstruction().reset();

			// No instruction in the pipeline stage. Return.
			return;
		}
		
		String opCode = div2Stage.getInputInstruction().getOpCode();

		div2Stage.getOutputInstruction().setInstruction(
				div2Stage.getInputInstruction().getInstruction());
		div2Stage.getOutputInstruction().setProgramCounter(
				div2Stage.getInputInstruction().getProgramCounter());
		div2Stage.getOutputInstruction().setOpCode(opCode);
		div2Stage.getOutputInstruction().setUpdatePSW(
				div2Stage.getInputInstruction().canUpdatePSW());
		div2Stage.getOutputInstruction().setUpdatePSW(
				div2Stage.getInputInstruction().canUpdatePSW());
		div2Stage.getOutputInstruction().setLsqIndex(
				div2Stage.getInputInstruction().getLsqIndex());
		div2Stage.getOutputInstruction().setRobIndex(
				div2Stage.getInputInstruction().getRobIndex());
		div2Stage.getOutputInstruction().setTagCFID(
				div2Stage.getInputInstruction().getTagCFID());
		div2Stage.getOutputInstruction().setDispatchCycle(
				div2Stage.getInputInstruction().getDispatchCycle());

		switch (opCode) {
			case IConstants.IOperations.DIV:
			{
				int destRegAddr = div2Stage.getInputInstruction().getDestRegAddr();
				int destRegValue = (div2Stage.getInputInstruction().getDestRegValue()) / 1;
				
				div2Stage.getOutputInstruction().setDestRegAddr(destRegAddr);
				div2Stage.getOutputInstruction().setDestRegValue(destRegValue);				
				break;
			}
	
			case IConstants.IOperations.HALT:
				break;
	
			default:
				break;
		}
		
		// Copy output latches of this stage into input latches of next stage for next cycle,
		// only if the current stage is not stalled
		copyData(div3Stage.getInputInstruction(), div2Stage.getOutputInstruction());
	}

	private void div3FU() throws DivideByZeroException {
		
		// If the stage is stalled do nothing.
		if (div3Stage.isStalled()) {
			return;
		}

		// Reset the output latch
		div3Stage.getOutputInstruction().reset();
		
		String instructionStr = div3Stage.getInputInstruction().getInstruction();
		if (null == instructionStr) {

			// Reset the input of next stage
			div4Stage.getInputInstruction().reset();

			// No instruction in the pipeline stage. Return.
			return;
		}
		
		String opCode = div3Stage.getInputInstruction().getOpCode();

		div3Stage.getOutputInstruction().setInstruction(
				div3Stage.getInputInstruction().getInstruction());
		div3Stage.getOutputInstruction().setProgramCounter(
				div3Stage.getInputInstruction().getProgramCounter());
		div3Stage.getOutputInstruction().setOpCode(opCode);
		div3Stage.getOutputInstruction().setUpdatePSW(
				div3Stage.getInputInstruction().canUpdatePSW());
		div3Stage.getOutputInstruction().setLsqIndex(
				div3Stage.getInputInstruction().getLsqIndex());
		div3Stage.getOutputInstruction().setRobIndex(
				div3Stage.getInputInstruction().getRobIndex());
		div3Stage.getOutputInstruction().setTagCFID(
				div3Stage.getInputInstruction().getTagCFID());
		div3Stage.getOutputInstruction().setDispatchCycle(
				div3Stage.getInputInstruction().getDispatchCycle());

		switch (opCode) {
			case IConstants.IOperations.DIV:
			{
				int destRegAddr = div3Stage.getInputInstruction().getDestRegAddr();
				int destRegValue = (div3Stage.getInputInstruction().getDestRegValue()) / 1;
				
				div3Stage.getOutputInstruction().setDestRegAddr(destRegAddr);
				div3Stage.getOutputInstruction().setDestRegValue(destRegValue);				
				break;
			}
	
			case IConstants.IOperations.HALT:
				break;
	
			default:
				break;
		}
		
		// Copy output latches of this stage into input latches of next stage for next cycle,
		// only if the current stage is not stalled
		copyData(div4Stage.getInputInstruction(), div3Stage.getOutputInstruction());
	}
	
	private void div4FU() throws DivideByZeroException {
		
		// If the stage is stalled do nothing.
		if (div4Stage.isStalled()) {
			return;
		}

		// Reset the output latch
		div4Stage.getOutputInstruction().reset();
		
		String instructionStr = div4Stage.getInputInstruction().getInstruction();
		if (null == instructionStr) {
			// No instruction in the pipeline stage. Return.
			return;
		}
		
		int destRegValue = -1;
		//boolean setPSWFlags = false;
		String opCode = div4Stage.getInputInstruction().getOpCode();

		div4Stage.getOutputInstruction().setInstruction(
				div4Stage.getInputInstruction().getInstruction());
		div4Stage.getOutputInstruction().setProgramCounter(
				div4Stage.getInputInstruction().getProgramCounter());
		div4Stage.getOutputInstruction().setOpCode(opCode);
		div4Stage.getOutputInstruction().setUpdatePSW(
				div4Stage.getInputInstruction().canUpdatePSW());
		div4Stage.getOutputInstruction().setLsqIndex(
				div4Stage.getInputInstruction().getLsqIndex());
		div4Stage.getOutputInstruction().setRobIndex(
				div4Stage.getInputInstruction().getRobIndex());
		div4Stage.getOutputInstruction().setTagCFID(
				div4Stage.getInputInstruction().getTagCFID());
		div4Stage.getOutputInstruction().setDispatchCycle(
				div4Stage.getInputInstruction().getDispatchCycle());

		switch (opCode) {
			case IConstants.IOperations.DIV:
			{
				int destRegAddr = div4Stage.getInputInstruction().getDestRegAddr();
				destRegValue = (div4Stage.getInputInstruction().getDestRegValue()) / 1;
				//setPSWFlags = true;
				
				// Write value to Physical register and mark it as Valid.
				phyRegFile.getRegister(div4Stage.getInputInstruction().getDestRegAddr()).setValue(
						destRegValue);
				
				//+ Perform this update to all the rename table backups as well dispatched after this instruction
				int cfInstrOrderIndex = -1;
				int cfIdTag = div4Stage.getInputInstruction().getTagCFID();
				cfIdTag = cfIdTag + 1;
				for (int cnt = 0; cnt < CFInstructionOrder.size(); cnt++) {
					// Search for the CFID in the CF Instruction order queue. Ideally it should be at the 0th location i.e. head
					if (cfIdTag == CFInstructionOrder.get(cnt).getCFID()) {
						cfInstrOrderIndex = cnt;
						//cfInstrFound = true;
						break;
					}
				}
				
				if (-1 != cfInstrOrderIndex) {
					for (int cnt = cfInstrOrderIndex; cnt < CFInstructionOrder.size(); cnt++) {
						int tempRobIndex = CFInstructionOrder.get(cnt).getRobIndex();
						
						PhysicalRegisterFile tempPhyRegFile = rob.getROBEntry(tempRobIndex).getPhyRegFileBackup();
						// Write value to Physical register and mark it as Valid.
						tempPhyRegFile.getRegister(destRegAddr).setValue(
								destRegValue);
						tempPhyRegFile.getRegister(destRegAddr).setStatus(true);
						
						if (0 == destRegValue) {
							tempPhyRegFile.getRegister(destRegAddr).setZeroFlag(true);
						} else {
							tempPhyRegFile.getRegister(destRegAddr).setZeroFlag(false);
						}
					}
				}
				//-
				
				// Write the value of PSW zero flag in physical register
				if (0 == destRegValue) {
					phyRegFile.getRegister(destRegAddr).setZeroFlag(true);
					// Also copy this value to output for data forwarding
					div4Stage.getOutputInstruction().setSourcePSWZeroFlagValue(true);					
				} else {
					phyRegFile.getRegister(destRegAddr).setZeroFlag(false);
					// Also copy this value to output for data forwarding
					div4Stage.getOutputInstruction().setSourcePSWZeroFlagValue(false);	
				}
				
				phyRegFile.getRegister(div4Stage.getInputInstruction().getDestRegAddr()).setStatus(true);
				
				div4Stage.getOutputInstruction().setDestRegAddr(destRegAddr);				
				div4Stage.getOutputInstruction().setDestRegValue(destRegValue);				
				break;
			}
	
			case IConstants.IOperations.HALT:
				break;
	
			default:
				break;
		}
		
		// Mark the result as ready in ROB entry
		rob.getROBEntry(div4Stage.getInputInstruction().getRobIndex()).setResultReady(true);

		// Do not copy output latches into input latches for next stage here,
		// It will be done by sequencer logic 
	}
	
	//+ New memory stage
	private void memory() throws InvalidDataMemoryAccessedException {
		
		// If the stage is stalled do nothing.
		if (memoryStage.isStalled()) {
			return;
		}
		
		String instructionStr = memoryStage.getInputInstruction().getInstruction();
		if (null == instructionStr) {
			//writeBackStage.getInputInstruction().reset();
			MySimpleSimulator.memStageCycle = 0;
			// No instruction in the pipeline stage. Return.
			memoryStage.getOutputInstruction().reset();
			return;
		}

		// We have to spend 3 cycles in memory stage. So increase the counter and keep track of it
		MySimpleSimulator.memStageCycle++;

		String opCode = memoryStage.getInputInstruction().getOpCode();

		if (1 == MySimpleSimulator.memStageCycle) {
			// Reset the output latch
			memoryStage.getOutputInstruction().reset();
			memoryStage.setBusy(true);
	
			memoryStage.getOutputInstruction().setInstruction(
					memoryStage.getInputInstruction().getInstruction());
			memoryStage.getOutputInstruction().setProgramCounter(
					memoryStage.getInputInstruction().getProgramCounter());
			memoryStage.getOutputInstruction().setOpCode(opCode);
			memoryStage.getOutputInstruction().setUpdatePSW(
					memoryStage.getInputInstruction().canUpdatePSW());
			memoryStage.getOutputInstruction().setZeroPSW(
					memoryStage.getInputInstruction().isZeroPSW());
			memoryStage.getOutputInstruction().setCarryPSW(
					memoryStage.getInputInstruction().isCarryPSW());
			memoryStage.getOutputInstruction().setNegativePSW(
					memoryStage.getInputInstruction().isNegativePSW());
		}
		
		if (IConstants.MEMORY_STAGE_LATENCY == MySimpleSimulator.memStageCycle) {
			switch (opCode) {
				case IConstants.IOperations.LOAD:
				{
					int targetMemAddr = memoryStage.getInputInstruction().getTargetMemoryAddr();
					assertDataMemoryValidity(targetMemAddr);
	
					targetMemAddr = targetMemAddr / 4;
					
					int targetMemValue = dataMem.getData(targetMemAddr);
	
					memoryStage.getOutputInstruction().setDestRegAddr(
							memoryStage.getInputInstruction().getDestRegAddr());
					//memoryStage.getOutputInstruction().setDestRegValue(targetMemValue);
					memoryStage.getOutputInstruction().setTargetMemoryValue(targetMemValue);
									
					// Write value to Physical register and mark it as Valid.
					phyRegFile.getRegister(memoryStage.getInputInstruction().getDestRegAddr()).setValue(
							targetMemValue);
					phyRegFile.getRegister(memoryStage.getInputInstruction().getDestRegAddr()).setStatus(true);
					
					//+ Perform this update to all the rename table backups as well dispatched after this instruction
					int cfInstrOrderIndex = -1;
					int cfIdTag = memoryStage.getInputInstruction().getTagCFID();
					cfIdTag = cfIdTag + 1;
					for (int cnt = 0; cnt < CFInstructionOrder.size(); cnt++) {
						// Search for the CFID in the CF Instruction order queue. Ideally it should be at the 0th location i.e. head
						if (cfIdTag == CFInstructionOrder.get(cnt).getCFID()) {
							cfInstrOrderIndex = cnt;
							//cfInstrFound = true;
							break;
						}
					}
					
					if (-1 != cfInstrOrderIndex) {
						for (int cnt = cfInstrOrderIndex; cnt < CFInstructionOrder.size(); cnt++) {
							int tempRobIndex = CFInstructionOrder.get(cnt).getRobIndex();
							
							PhysicalRegisterFile tempPhyRegFile = rob.getROBEntry(tempRobIndex).getPhyRegFileBackup();
							// Write value to Physical register and mark it as Valid.
							tempPhyRegFile.getRegister(memoryStage.getInputInstruction().getDestRegAddr()).setValue(
									targetMemValue);
							tempPhyRegFile.getRegister(memoryStage.getInputInstruction().getDestRegAddr()).setStatus(true);
						}
					}
					//-

					// Mark value of Physical register as valid in ROB entry.
					rob.getROBEntry(memoryStage.getInputInstruction().getRobIndex()).setResultReady(true);
					
					break;
				}
		
				case IConstants.IOperations.STORE:
				{
					int targetMemAddr = memoryStage.getInputInstruction().getTargetMemoryAddr();
					assertDataMemoryValidity(targetMemAddr);
	
					targetMemAddr = targetMemAddr / 4;
					
					int targetMemValue = memoryStage.getInputInstruction().getTargetMemoryValue();
					
					dataMem.setData(targetMemAddr, targetMemValue);
					
					// Mark value of Physical register as valid in ROB entry.
					//rob.getROBEntry(memoryStage.getInputInstruction().getRobIndex()).setResultReady(true);

					break;
				}
		
				// TODO: Remove all cases except LOAD/STORE
				case IConstants.IOperations.ADD:
				case IConstants.IOperations.SUB:
				case IConstants.IOperations.MUL:
				case IConstants.IOperations.DIV:
				case IConstants.IOperations.AND:
				case IConstants.IOperations.OR:
				case IConstants.IOperations.EXOR:
				case IConstants.IOperations.MOVC:
				case IConstants.IOperations.JAL:
				{
					memoryStage.getOutputInstruction().setDestRegAddr(
							memoryStage.getInputInstruction().getDestRegAddr());
					memoryStage.getOutputInstruction().setDestRegValue(
							memoryStage.getInputInstruction().getDestRegValue());
					
					break;
				}
				
		
				case IConstants.IOperations.BZ:
				case IConstants.IOperations.BNZ:
				case IConstants.IOperations.JUMP:
				case IConstants.IOperations.HALT:
					break;
		
				default:
					break;
			}
			
			memoryStage.setBusy(false);
			memStageCycle = 0;
			
			// Copy output latches of this stage into input latches of next stage for next cycle,
			// only if the current stage is not stalled
			//copyData(writeBackStage.getInputInstruction(), memoryStage.getOutputInstruction());		
		}
	}
	//-

	
	public boolean simulate(int cycleCnt) {
	
		if (false == isInitialized) {
			System.out.println("Simulator is not initialized. Please initialize before begining simulation.");
			return false;
		}
		
		int totalCycles = Stats.getCycle() + cycleCnt;
		String issueQueueStr = null;
		String lsqStr = null;
		String robStr = null;
		// Decide terminating condition for loop when all instructions are fetched
		while (Stats.getCycle() < totalCycles) {
			// TODO: Delete this
			if (9 == (Stats.getCycle() + 1)) {
				System.out.println("");
			}
			
			try {
				
				// Handle control flow change by squashing all instructions fetched along the wrong path
				handleControlFlowChange();
				
				fetch();

				//writeBack();
				// Process ROB Entry
				processROB();
				robStr = printROB();
				
				//+
				lsqStr = updatePrintedLSQ();
				//-
				processLSQ();// Process LSQ Entry
				lsqStr = printLSQ(lsqStr);
				//-

				memory();
				
				executeEx();
				
				// Process IQ Entry;
				issueQueueStr = printIssueQueue();
				processIQ();
				
				decodeRFEx();
				//-

//				decodeRF();
	
				System.out.println();
				System.out.println("Cycle: " + (Stats.getCycle() + 1));
				displayCycle(issueQueueStr, lsqStr, robStr);
				
				// Increment cycle
				Stats.incrementCycle();
			} catch (InterruptedException | InvalidRegisterAddressException | InvalidCodeMemoryAccessedException | InvalidDataMemoryAccessedException | DivideByZeroException | InvalidInstructionFormatException e) {
				System.out.println();
				System.out.println("Cycle: " + (Stats.getCycle() + 1));
				issueQueueStr = printIssueQueue();
				lsqStr = printLSQ(updatePrintedLSQ());
				robStr = printROB();
				displayCycle(issueQueueStr, lsqStr, robStr);
				System.out.println();
				System.out.println("Stopping simulation: " + e.getMessage());
				System.out.println();
				System.out.println("Please re-initialize the simulator to get correct results");
				
				// Increment cycle
				Stats.incrementCycle();
				
				// Force to re-initialize the system as global state may be corrupted by invalid access
				isInitialized = false;
				break;
			}
			
		}
		
		System.out.println("");
		System.out.println("Count of committed instructions: " + Stats.getCommittedInstr());
		System.out.println("Count of clock cycles          : " + Stats.getCycle());
		System.out.println("Clock cycles per instruction   : " + (((double)Stats.getCycle())/((double)Stats.getCommittedInstr())));

		return true;
	}
	
	public boolean initialize(String instrFile) throws FileNotFoundException {

		if (null == instrFile) {
			throw new FileNotFoundException("Instructions File Name is null");
		}

		// Read Instructions File and Initialize Code Memory
		File file = new File(instrFile);
		if (!file.exists()) {
			throw new FileNotFoundException("Could not find Instructions File: " + instrFile);
		}
		
		Scanner instrScanner = new Scanner(file);
		
		int lineCount = 0;
		int registerAddress = IConstants.CODE_MEMORY_BASE_ADDRESS;
		ArrayList<CodeLine> codeLines = new ArrayList<CodeLine>();
		
		while (instrScanner.hasNextLine()) {
			String instr = instrScanner.nextLine();
			
			if (null != instr && (!instr.trim().isEmpty())) {
				lineCount++;
				
				CodeLine codeLine = new CodeLine();
				codeLine.setFileLineNumber(lineCount);
				codeLine.setInstAddress(registerAddress);
				registerAddress = registerAddress + IConstants.INSTRUCTION_SIZE;
				
				codeLine.setInstruction(instr.trim());
				codeLines.add(codeLine);
			}
		}

		instrScanner.close();
		
		codeMem = new CodeMemory(codeLines);

		// Memset Data Memory
		dataMem = new DataMemory(IConstants.DATA_MEMORY_BASE_ADDRESS,
				IConstants.DATA_MEMORY_COUNT_WORDS);

		// Create and memset registers including PSW
		regFile = new RegisterFile(IConstants.REGISTER_COUNT);

		// Create and memset physical registers
		phyRegFile = new PhysicalRegisterFile(IConstants.PHYSICAL_REGISTER_COUNT);
		
		// Create and initialize rename table along with entry for PSW
		int cnt = 0;
		renameTable = new RenameTableEntry[IConstants.REGISTER_COUNT + 1];
		for (cnt = 0; cnt < IConstants.REGISTER_COUNT; cnt++){
			renameTable[cnt] = new RenameTableEntry(cnt);
		}
		renameTable[cnt] = new RenameTableEntry(IConstants.PSW_ARCH_REGISTER_INDEX);

		issueQueue = new ArrayList<IQEntry>(IConstants.ISSUE_QUEUE_CAPACITY);
		
		lsq = new LoadStoreQueue(IConstants.LOAD_STORE_QUEUE_CAPACITY);
		
		rob = new ReOrderBuffer(IConstants.ROB_CAPACITY);

		// Create a free list of CFID
		freeCFIDList = new ArrayList<Integer>();
		freeCFIDList.add(0);
		freeCFIDList.add(1);
		freeCFIDList.add(2);
		freeCFIDList.add(3);
		freeCFIDList.add(4);
		freeCFIDList.add(5);
		freeCFIDList.add(6);
		freeCFIDList.add(7);

		CFInstructionOrder = new ArrayList<CFInstructionInfo>();
		lastCFID = -1;
		
		// Create and Flush all latches
		fetchStage = new PipeLineStage();
		decodeRFStage = new PipeLineStageEx();
//		executeStage = new PipeLineStage();
		mul1Stage = new PipeLineStage();
		mul2Stage = new PipeLineStage();
		addStage = new PipeLineStage();
		div1Stage = new PipeLineStage();
		div2Stage = new PipeLineStage();
		div3Stage = new PipeLineStage();
		div4Stage = new PipeLineStage();
		memoryStage = new PipeLineStage();
		//writeBackStage = new PipeLineStage();

		// Initialize cycle count
		Stats.setCycle(0);
		
		fetchStage.getInputInstruction().setProgramCounter(IConstants.CODE_MEMORY_BASE_ADDRESS);
		
		memStageCycle = 0;
		
		this.controlFlowAltered = false;
		this.controlFlowHandled = false;
		
		isInitialized = true;
		
		System.out.println("Initialized the simulator successfully");
		return true;
	}

	public String printROB() {
		StringBuilder robStr = new StringBuilder();
		int index = -1;		
		robStr = new StringBuilder("<ROB>: ");
		
		if (rob.isEmpty()) {
			robStr.append("\t\tEmpty");
			return robStr.toString();
		}
		
		do {
			if (-1 == index) {
				index = rob.getHead();
			} else {
				index = (index + 1) % rob.getRob().length;
				if (index == rob.getHead()) {
					// It means ROB is full. So  we have to stop printing here as we have printed whole ROB
					break;
				}
			}
			
			
			if (rob.getROBEntry(index).isAllocated()) {
				robStr.append("\n");
				robStr.append("     > " +
						("(I" + (codeMem.getFileLineNumber(rob.getROBEntry(index).getProgramCounter()) - 1) + ")\t") +
						(rob.getROBEntry(index).getInstruction()));
			} else {
				//robStr.append("  [" + index + "] Empty");
			}
			
			if (index == rob.getTail() && false == rob.getROBEntry(index).isAllocated()) {
				break;
			}
		} while (true);
		
		return robStr.toString();
	}
	
	public String updatePrintedLSQ() {
		StringBuilder lsqStr = new StringBuilder();
		lsqStr = new StringBuilder("<LSQ>: ");
				
		if (lsq.getTempHeadEntry().isAllocated()) {
			lsqStr.append("\n");
			lsqStr.append("     > " +
					("(I" + (codeMem.getFileLineNumber(lsq.getTempHeadEntry().getProgramCounter()) - 1) + ")\t") +
					(lsq.getTempHeadEntry().getInstruction()) + " (Temp Head created by STORE-LOAD forwarding)");
		}
		
		return lsqStr.toString();
	}
	
	public String printLSQ(String initialLsqStr) {
		if (null == initialLsqStr) {
			initialLsqStr = updatePrintedLSQ();
		}
		
		StringBuilder lsqStr = new StringBuilder(initialLsqStr);
		//lsqStr = new StringBuilder("<LSQ>: ");
		
//		if (lsq.getTempHeadEntry().isAllocated()) {
//			lsqStr.append("\n");
//			lsqStr.append("     > " +
//					("(I" + (codeMem.getFileLineNumber(lsq.getTempHeadEntry().getProgramCounter()) - 1) + ")\t") +
//					(lsq.getTempHeadEntry().getInstruction()) + " (Temp Head for STORE-LOAD forwarding)");
//		}
		
		if (lsq.isEmpty()) {
			if ("<LSQ>: ".length() == lsqStr.length()) {
				lsqStr.append("\t\tEmpty");
			}
			
			return lsqStr.toString();
		}

		int size = lsq.getLsq().size();
		
		for (int cnt = 0; cnt < size; cnt++) {			
			if (lsq.getLsq().get(cnt).isAllocated()) {
				lsqStr.append("\n");
				lsqStr.append("     > " +
						("(I" + (codeMem.getFileLineNumber(lsq.getLsq().get(cnt).getProgramCounter()) - 1) + ")\t") +
						(lsq.getLsq().get(cnt).getInstruction()));
				if (0 == cnt) {
					lsqStr.append(" (Head)");
				}
			} else {
				//robStr.append("  [" + cnt + "] Empty");
			}
		}
		
		return lsqStr.toString();
	}

	public String printIssueQueue() {
		StringBuilder issueQueueStr = new StringBuilder();
		
		issueQueueStr = new StringBuilder("<IQ> : ");
		if (0 == issueQueue.size()) {
			issueQueueStr.append("\t\tEmpty");
		} else {
			for (int index = 0; index < issueQueue.size(); index++) {
				issueQueueStr.append("\n");
				issueQueueStr.append("     > " +
						("(I" + (codeMem.getFileLineNumber(issueQueue.get(index).getProgramCounter()) - 1) + ")\t") +
						issueQueue.get(index).getInstruction());
			}
		}
		
		return issueQueueStr.toString();
	}
	
	public String printRenameTable() {
		StringBuilder renameTableStr = new StringBuilder();
		
		renameTableStr = new StringBuilder("<Rename Table>: ");
		for (int index = 0; index < renameTable.length; index++) {
			if (IConstants.PHYSICAL_REGISTER ==  renameTable[index].archOrPhyRegIndicator) {
				renameTableStr.append("\n");
				renameTableStr.append("     > R" + index + ": P" + renameTable[index].regAddress);
				if (IConstants.PSW_ARCH_REGISTER_INDEX == index) {
					renameTableStr.append(" [PSW]");
				}
			}
		}

		if ("<Rename Table>: ".length() == renameTableStr.length()) {
			renameTableStr.append("Empty");
		}
		
		return renameTableStr.toString();
	}
	
	public String printCFInstructionOrder() {
		StringBuilder cfInstrOrderStr = new StringBuilder("<CFInstOrdr>: ");
		if (0 == CFInstructionOrder.size()) {
			cfInstrOrderStr.append("\tEmpty");
		} else {
			for (int index = 0; index < CFInstructionOrder.size(); index++) {
				cfInstrOrderStr.append("\n");
				cfInstrOrderStr.append("     > " +
						"CFID " + CFInstructionOrder.get(index).getCFID() + ": " + CFInstructionOrder.get(index).getInstr());
			}
		}
		
		return cfInstrOrderStr.toString();
	}
	
	public void displayCycle(String issueQueueStr, String lsqStr, String robStr) {
		System.out.println("F    : "
				+ ((null == fetchStage.getOutputInstruction().getInstruction())?"\t\tEmpty" + (fetchStage.isStalled()?" (Stalled)":""):
					((-1 == fetchStage.getOutputInstruction().getProgramCounter())?"":
					("(I" + (codeMem.getFileLineNumber(fetchStage.getOutputInstruction().getProgramCounter()) - 1) + ")\t") 
					+ fetchStage.getOutputInstruction().getInstruction()
					+ (fetchStage.isStalled()?" (Stalled)":""))));
		System.out.println("D/RF : "
				+ ((null == decodeRFStage.getOutputInstruction().getInstruction())?"\t\tEmpty" + (decodeRFStage.isStalled()?" (Stalled)":""):
					((-1 == decodeRFStage.getOutputInstruction().getProgramCounter())?"":
					("(I" + (codeMem.getFileLineNumber(decodeRFStage.getOutputInstruction().getProgramCounter()) - 1) + ")\t") 
					+ decodeRFStage.getOutputInstruction().getInstruction()
					+ (decodeRFStage.isStalled()?" (Stalled)":""))));
		System.out.println(printRenameTable());
		System.out.println(issueQueueStr);
		System.out.println(robStr);
		if (!this.committedInstructions.isEmpty()) {
			System.out.println(this.committedInstructions);
		}
		System.out.println(lsqStr);
		System.out.println(printCFInstructionOrder());
		System.out.println("INTFU: "
				+ ((null == addStage.getOutputInstruction().getInstruction())?"\t\tEmpty":
					((-1 == addStage.getOutputInstruction().getProgramCounter())?"":
					("(I" + (codeMem.getFileLineNumber(addStage.getOutputInstruction().getProgramCounter()) - 1) + ")\t") 
					+ addStage.getOutputInstruction().getInstruction()
					+ (addStage.isStalled()?" (Stalled)":""))));
		System.out.println("MUL1 : "
				+ ((null == mul1Stage.getOutputInstruction().getInstruction())?"\t\tEmpty":
					((-1 == mul1Stage.getOutputInstruction().getProgramCounter())?"":
					("(I" + (codeMem.getFileLineNumber(mul1Stage.getOutputInstruction().getProgramCounter()) - 1) + ")\t") 
					+ mul1Stage.getOutputInstruction().getInstruction()
					+ (mul1Stage.isStalled()?" (Stalled)":""))));
		System.out.println("MUL2 : "
				+ ((null == mul2Stage.getOutputInstruction().getInstruction())?"\t\tEmpty":
					((-1 == mul2Stage.getOutputInstruction().getProgramCounter())?"":
					("(I" + (codeMem.getFileLineNumber(mul2Stage.getOutputInstruction().getProgramCounter()) - 1) + ")\t") 
					+ mul2Stage.getOutputInstruction().getInstruction()
					+ (mul2Stage.isStalled()?" (Stalled)":""))));
		System.out.println("DIV1 : "
				+ ((null == div1Stage.getOutputInstruction().getInstruction())?"\t\tEmpty":
					((-1 == div1Stage.getOutputInstruction().getProgramCounter())?"":
					("(I" + (codeMem.getFileLineNumber(div1Stage.getOutputInstruction().getProgramCounter()) - 1) + ")\t") 
					+ div1Stage.getOutputInstruction().getInstruction()
					+ (div1Stage.isStalled()?" (Stalled)":""))));
		System.out.println("DIV2 : "
				+ ((null == div2Stage.getOutputInstruction().getInstruction())?"\t\tEmpty":
					((-1 == div2Stage.getOutputInstruction().getProgramCounter())?"":
					("(I" + (codeMem.getFileLineNumber(div2Stage.getOutputInstruction().getProgramCounter()) - 1) + ")\t") 
					+ div2Stage.getOutputInstruction().getInstruction()
					+ (div2Stage.isStalled()?" (Stalled)":""))));
		System.out.println("DIV3 : "
				+ ((null == div3Stage.getOutputInstruction().getInstruction())?"\t\tEmpty":
					((-1 == div3Stage.getOutputInstruction().getProgramCounter())?"":
					("(I" + (codeMem.getFileLineNumber(div3Stage.getOutputInstruction().getProgramCounter()) - 1) + ")\t") 
					+ div3Stage.getOutputInstruction().getInstruction()
					+ (div3Stage.isStalled()?" (Stalled)":""))));
		System.out.println("DIV4 : "
				+ ((null == div4Stage.getOutputInstruction().getInstruction())?"\t\tEmpty":
					((-1 == div4Stage.getOutputInstruction().getProgramCounter())?"":
					("(I" + (codeMem.getFileLineNumber(div4Stage.getOutputInstruction().getProgramCounter()) - 1) + ")\t") 
					+ div4Stage.getOutputInstruction().getInstruction()
					+ (div4Stage.isStalled()?" (Stalled)":""))));
		System.out.println("MEM  : "
				+ ((null == memoryStage.getOutputInstruction().getInstruction())?"\t\tEmpty":
					((-1 == memoryStage.getOutputInstruction().getProgramCounter())?"":
					("(I" + (codeMem.getFileLineNumber(memoryStage.getOutputInstruction().getProgramCounter()) - 1) + ")\t") 
					+ memoryStage.getOutputInstruction().getInstruction()
					+ (memoryStage.isStalled()?" (Stalled)":""))));
//		System.out.println("WB   : "
//				+ ((null == writeBackStage.getOutputInstruction().getInstruction())?"\t\tEmpty":
//					((-1 == writeBackStage.getOutputInstruction().getProgramCounter())?"":
//					("(I" + (codeMem.getFileLineNumber(writeBackStage.getOutputInstruction().getProgramCounter()) - 1) + ")\t") 
//					+ writeBackStage.getOutputInstruction().getInstruction()
//					+ (writeBackStage.isStalled()?" (Stalled)":""))));
	}

	
	public void displayCycleOld(String issueQueueStr, String lsqStr, String robStr) {
		System.out.println("F    : "
				+ ((null == fetchStage.getOutputInstruction().getInstruction())?"\t\tEmpty" + (fetchStage.isStalled()?" (Stalled)":""):
					((-1 == fetchStage.getOutputInstruction().getProgramCounter())?"":
					("(I" + (codeMem.getFileLineNumber(fetchStage.getOutputInstruction().getProgramCounter()) - 1) + ")\t") 
					+ fetchStage.getOutputInstruction().getInstruction()
					+ (fetchStage.isStalled()?" (Stalled)":""))));
		System.out.println("D/RF : "
				+ ((null == decodeRFStage.getOutputInstruction().getInstruction())?"\t\tEmpty" + (decodeRFStage.isStalled()?" (Stalled)":""):
					((-1 == decodeRFStage.getOutputInstruction().getProgramCounter())?"":
					("(I" + (codeMem.getFileLineNumber(decodeRFStage.getOutputInstruction().getProgramCounter()) - 1) + ")\t") 
					+ decodeRFStage.getOutputInstruction().getInstruction()
					+ (decodeRFStage.isStalled()?" (Stalled)":""))));
		System.out.println(issueQueueStr);
		System.out.println("INTFU: "
				+ ((null == addStage.getOutputInstruction().getInstruction())?"\t\tEmpty":
					((-1 == addStage.getOutputInstruction().getProgramCounter())?"":
					("(I" + (codeMem.getFileLineNumber(addStage.getOutputInstruction().getProgramCounter()) - 1) + ")\t") 
					+ addStage.getOutputInstruction().getInstruction()
					+ (addStage.isStalled()?" (Stalled)":""))));
		System.out.println("MUL1 : "
				+ ((null == mul1Stage.getOutputInstruction().getInstruction())?"\t\tEmpty":
					((-1 == mul1Stage.getOutputInstruction().getProgramCounter())?"":
					("(I" + (codeMem.getFileLineNumber(mul1Stage.getOutputInstruction().getProgramCounter()) - 1) + ")\t") 
					+ mul1Stage.getOutputInstruction().getInstruction()
					+ (mul1Stage.isStalled()?" (Stalled)":""))));
		System.out.println("MUL2 : "
				+ ((null == mul2Stage.getOutputInstruction().getInstruction())?"\t\tEmpty":
					((-1 == mul2Stage.getOutputInstruction().getProgramCounter())?"":
					("(I" + (codeMem.getFileLineNumber(mul2Stage.getOutputInstruction().getProgramCounter()) - 1) + ")\t") 
					+ mul2Stage.getOutputInstruction().getInstruction()
					+ (mul2Stage.isStalled()?" (Stalled)":""))));
		System.out.println("DIV1 : "
				+ ((null == div1Stage.getOutputInstruction().getInstruction())?"\t\tEmpty":
					((-1 == div1Stage.getOutputInstruction().getProgramCounter())?"":
					("(I" + (codeMem.getFileLineNumber(div1Stage.getOutputInstruction().getProgramCounter()) - 1) + ")\t") 
					+ div1Stage.getOutputInstruction().getInstruction()
					+ (div1Stage.isStalled()?" (Stalled)":""))));
		System.out.println("DIV2 : "
				+ ((null == div2Stage.getOutputInstruction().getInstruction())?"\t\tEmpty":
					((-1 == div2Stage.getOutputInstruction().getProgramCounter())?"":
					("(I" + (codeMem.getFileLineNumber(div2Stage.getOutputInstruction().getProgramCounter()) - 1) + ")\t") 
					+ div2Stage.getOutputInstruction().getInstruction()
					+ (div2Stage.isStalled()?" (Stalled)":""))));
		System.out.println("DIV3 : "
				+ ((null == div3Stage.getOutputInstruction().getInstruction())?"\t\tEmpty":
					((-1 == div3Stage.getOutputInstruction().getProgramCounter())?"":
					("(I" + (codeMem.getFileLineNumber(div3Stage.getOutputInstruction().getProgramCounter()) - 1) + ")\t") 
					+ div3Stage.getOutputInstruction().getInstruction()
					+ (div3Stage.isStalled()?" (Stalled)":""))));
		System.out.println("DIV4 : "
				+ ((null == div4Stage.getOutputInstruction().getInstruction())?"\t\tEmpty":
					((-1 == div4Stage.getOutputInstruction().getProgramCounter())?"":
					("(I" + (codeMem.getFileLineNumber(div4Stage.getOutputInstruction().getProgramCounter()) - 1) + ")\t") 
					+ div4Stage.getOutputInstruction().getInstruction()
					+ (div4Stage.isStalled()?" (Stalled)":""))));
		System.out.println(lsqStr);
		System.out.println("MEM  : "
				+ ((null == memoryStage.getOutputInstruction().getInstruction())?"\t\tEmpty":
					((-1 == memoryStage.getOutputInstruction().getProgramCounter())?"":
					("(I" + (codeMem.getFileLineNumber(memoryStage.getOutputInstruction().getProgramCounter()) - 1) + ")\t") 
					+ memoryStage.getOutputInstruction().getInstruction()
					+ (memoryStage.isStalled()?" (Stalled)":""))));
		System.out.println(robStr);
//		System.out.println("WB   : "
//				+ ((null == writeBackStage.getOutputInstruction().getInstruction())?"\t\tEmpty":
//					((-1 == writeBackStage.getOutputInstruction().getProgramCounter())?"":
//					("(I" + (codeMem.getFileLineNumber(writeBackStage.getOutputInstruction().getProgramCounter()) - 1) + ")\t") 
//					+ writeBackStage.getOutputInstruction().getInstruction()
//					+ (writeBackStage.isStalled()?" (Stalled)":""))));
	}

	
//	public void displayCycle() {
//		System.out.println("F    : "
//				+ ((null == fetchStage.getOutputInstruction().getInstruction())?"\t\tEmpty":
//					((-1 == fetchStage.getOutputInstruction().getProgramCounter())?"":
//					("(I" + (codeMem.getFileLineNumber(fetchStage.getOutputInstruction().getProgramCounter()) - 1) + ")\t") 
//					+ fetchStage.getOutputInstruction().getInstruction()
//					+ (fetchStage.isStalled()?" (Stalled)":""))));
//		System.out.println("D/RF : "
//				+ ((null == decodeRFStage.getOutputInstruction().getInstruction())?"\t\tEmpty":
//					((-1 == decodeRFStage.getOutputInstruction().getProgramCounter())?"":
//					("(I" + (codeMem.getFileLineNumber(decodeRFStage.getOutputInstruction().getProgramCounter()) - 1) + ")\t") 
//					+ decodeRFStage.getOutputInstruction().getInstruction()
//					+ (decodeRFStage.isStalled()?" (Stalled)":""))));
//		System.out.println("INTFU: "
//				+ ((null == addStage.getOutputInstruction().getInstruction())?"\t\tEmpty":
//					((-1 == addStage.getOutputInstruction().getProgramCounter())?"":
//					("(I" + (codeMem.getFileLineNumber(addStage.getOutputInstruction().getProgramCounter()) - 1) + ")\t") 
//					+ addStage.getOutputInstruction().getInstruction()
//					+ (addStage.isStalled()?" (Stalled)":""))));
//		System.out.println("MUL1 : "
//				+ ((null == mul1Stage.getOutputInstruction().getInstruction())?"\t\tEmpty":
//					((-1 == mul1Stage.getOutputInstruction().getProgramCounter())?"":
//					("(I" + (codeMem.getFileLineNumber(mul1Stage.getOutputInstruction().getProgramCounter()) - 1) + ")\t") 
//					+ mul1Stage.getOutputInstruction().getInstruction()
//					+ (mul1Stage.isStalled()?" (Stalled)":""))));
//		System.out.println("MUL2 : "
//				+ ((null == mul2Stage.getOutputInstruction().getInstruction())?"\t\tEmpty":
//					((-1 == mul2Stage.getOutputInstruction().getProgramCounter())?"":
//					("(I" + (codeMem.getFileLineNumber(mul2Stage.getOutputInstruction().getProgramCounter()) - 1) + ")\t") 
//					+ mul2Stage.getOutputInstruction().getInstruction()
//					+ (mul2Stage.isStalled()?" (Stalled)":""))));
//		System.out.println("DIV1 : "
//				+ ((null == div1Stage.getOutputInstruction().getInstruction())?"\t\tEmpty":
//					((-1 == div1Stage.getOutputInstruction().getProgramCounter())?"":
//					("(I" + (codeMem.getFileLineNumber(div1Stage.getOutputInstruction().getProgramCounter()) - 1) + ")\t") 
//					+ div1Stage.getOutputInstruction().getInstruction()
//					+ (div1Stage.isStalled()?" (Stalled)":""))));
//		System.out.println("DIV2 : "
//				+ ((null == div2Stage.getOutputInstruction().getInstruction())?"\t\tEmpty":
//					((-1 == div2Stage.getOutputInstruction().getProgramCounter())?"":
//					("(I" + (codeMem.getFileLineNumber(div2Stage.getOutputInstruction().getProgramCounter()) - 1) + ")\t") 
//					+ div2Stage.getOutputInstruction().getInstruction()
//					+ (div2Stage.isStalled()?" (Stalled)":""))));
//		System.out.println("DIV3 : "
//				+ ((null == div3Stage.getOutputInstruction().getInstruction())?"\t\tEmpty":
//					((-1 == div3Stage.getOutputInstruction().getProgramCounter())?"":
//					("(I" + (codeMem.getFileLineNumber(div3Stage.getOutputInstruction().getProgramCounter()) - 1) + ")\t") 
//					+ div3Stage.getOutputInstruction().getInstruction()
//					+ (div3Stage.isStalled()?" (Stalled)":""))));
//		System.out.println("DIV4 : "
//				+ ((null == div4Stage.getOutputInstruction().getInstruction())?"\t\tEmpty":
//					((-1 == div4Stage.getOutputInstruction().getProgramCounter())?"":
//					("(I" + (codeMem.getFileLineNumber(div4Stage.getOutputInstruction().getProgramCounter()) - 1) + ")\t") 
//					+ div4Stage.getOutputInstruction().getInstruction()
//					+ (div4Stage.isStalled()?" (Stalled)":""))));
//		System.out.println("MEM  : "
//				+ ((null == memoryStage.getOutputInstruction().getInstruction())?"\t\tEmpty":
//					((-1 == memoryStage.getOutputInstruction().getProgramCounter())?"":
//					("(I" + (codeMem.getFileLineNumber(memoryStage.getOutputInstruction().getProgramCounter()) - 1) + ")\t") 
//					+ memoryStage.getOutputInstruction().getInstruction()
//					+ (memoryStage.isStalled()?" (Stalled)":""))));
//		System.out.println("WB   : "
//				+ ((null == writeBackStage.getOutputInstruction().getInstruction())?"\t\tEmpty":
//					((-1 == writeBackStage.getOutputInstruction().getProgramCounter())?"":
//					("(I" + (codeMem.getFileLineNumber(writeBackStage.getOutputInstruction().getProgramCounter()) - 1) + ")\t") 
//					+ writeBackStage.getOutputInstruction().getInstruction()
//					+ (writeBackStage.isStalled()?" (Stalled)":""))));
//	}
	
	public void display() {

		System.out.println();
		System.out.println("Completed Cycles: " + Stats.getCycle());

		System.out.println();
		System.out.println("Gantt chart for last completed cycle: ");
		displayCycle(printIssueQueue(), printLSQ(null), printROB());

		System.out.println();
		System.out.println("Contents of the Pipeline Stages: ");
		System.out.println("F    : " + fetchStage);
		System.out.println("D/RF : " + decodeRFStage);
		System.out.println("INTFU: " + addStage);
		System.out.println("DIV1 : " + div1Stage);
		System.out.println("DIV2 : " + div2Stage);
		System.out.println("DIV3 : " + div3Stage);
		System.out.println("DIV4 : " + div4Stage);
		System.out.println("MUL1 : " + mul1Stage);
		System.out.println("MUL2 : " + mul2Stage);
		System.out.println("MEM  : " + memoryStage);
		//System.out.println("WB   : " + writeBackStage);

		System.out.println("");
		System.out.println("Architectural Register Contents: ");
		
		if (null == regFile) {
			System.out.println("Register file is not initialized.");
		} else {
			for (int cnt = 0; cnt < regFile.getRegisters().length; cnt++) {
				System.out.println("R" + cnt +
						": \t[Value: " + regFile.getRegister(cnt).getValue() +
						"\tStatus: " + regFile.getRegister(cnt).getStatus() +
						"\tZero Flag: " + regFile.getRegister(cnt).isZeroFlag() +
						"]");			
			}
//			System.out.println("PSW" + ": \t[Zero: " + regFile.getPSW().isZero() +
//					"\tNegative: " + regFile.getPSW().isNegative() +
//					"\t\tCarry: " + regFile.getPSW().isCarry() +
//					"\tStatus: " + regFile.getPSW().getStatus() + "]");
		}

		System.out.println("");
		System.out.println("Physical Register Contents: ");
		
		if (null == phyRegFile) {
			System.out.println("Register file is not initialized.");
		} else {
			for (int cnt = 0; cnt < phyRegFile.getRegisters().length; cnt++) {
				System.out.println("P" + cnt +
						": \t[Allocated: " + phyRegFile.getRegister(cnt).isAllocated() +
						"\tValue: " + phyRegFile.getRegister(cnt).getValue() +
						"\tStatus: " + phyRegFile.getRegister(cnt).getStatus() +
						"\tRenamed: " + phyRegFile.getRegister(cnt).isRenamed() +
						"\tZero Flag: " + phyRegFile.getRegister(cnt).isZeroFlag() + "]");			
			}
//			System.out.println("PSW" + ": \t[Zero: " + regFile.getPSW().isZero() +
//					"\tNegative: " + regFile.getPSW().isNegative() +
//					"\t\tCarry: " + regFile.getPSW().isCarry() +
//					"\tStatus: " + regFile.getPSW().getStatus() + "]");
		}

		System.out.println("");
		System.out.println("Data Memory Contents: ");

		if (null == dataMem) {
			System.out.println("Data memory is not initialized.");
		} else {
			for (int cnt = 0; cnt < IConstants.DISPLAY_MEMORY_WORDS_MAX;) {
				System.out.println("data_mem[" + (cnt * 4) + "]\t\t" + dataMem.getData(cnt) +
						"\t" + dataMem.getData(cnt+1) +
						"\t" + dataMem.getData(cnt+2) +
						"\t" + dataMem.getData(cnt+3)
						);
	
				cnt = cnt + 4;
	//			System.out.println("R" + cnt +
	//					": \t[Value: " + regFile.getRegister(cnt).getValue() + "\tStatus: " + regFile.getRegister(cnt).getStatus() + "]");			
			}
		}
	}
	
	public static void main(String[] args) {
		
		displayHeader();
		
		// Check command line arguments
		if (null == args || args.length != 1) {
			System.out.println("Invalid command line arguments");
			DisplayHelp();
			return;
		}
		
		// Create instance to keep track of context
		MySimpleSimulator obj = new MySimpleSimulator();
		
		String inputFileName = args[0];
		
		String input;
		int command = -1;
		try {
			do {
				command = showCommandsMenu();
	
				switch (command) {
					case 1:
						obj.initialize(inputFileName);
						break;
						
					case 2:
						System.out.println("Enter number of cpu cycles to simulate:");
						int cnt = -1;
						
						while(true) {
							try {
								input = scanner.next();
								cnt = Integer.parseInt(input);
								System.out.println();
								break;
							} catch(InputMismatchException | NumberFormatException e) {
								System.out.println("Invalid number!!! Please enter valid number");
							}
						}

						obj.simulate(cnt);
						break;
						
					case 3:
						obj.display();
						
					case 4:
						break;
					
					default:
						System.out.println("Invalid choice!!! Please enter valid command");
				}
				
			} while(4 != command);
		} catch (FileNotFoundException e) {
			System.err.println(e);
			//scanner.next();
		} catch (NoSuchElementException e) {
			//System.err.println("Input file does not exist.");
			//scanner.next();
		} finally {
			scanner.close();
		}
		
		System.out.println("");
		System.out.println("Exiting from simulator...");
	}

	private static int showCommandsMenu() {
		String input;
		int command = -1;
		
		
		System.out.println();
		System.out.println();
		System.out.println("List of available commands:");
		System.out.println("1. Initialize");
		System.out.println("2. Simulate");
		System.out.println("3. Display");
		System.out.println("4. Exit");
		System.out.println();
		System.out.println("Enter one of the above commands:");
		
		while(true) {
			try {
				input = scanner.next();
				command = Integer.parseInt(input);
				System.out.println();
				break;
			} catch (InputMismatchException | NumberFormatException e) {
				System.out.println("Invalid choice!!! Please enter again: ");
			}
		}
		
		return command;
	}
	
	private void copyData(LSQEntry dest, InstructionInfo source) {
		
		dest.setProgramCounter(source.getProgramCounter());
		dest.setInstruction(source.getInstruction());
		dest.setOpCode(source.getOpCode());
		dest.setDestRegAddr(source.getDestRegAddr());
		dest.setDestRegValue(source.getDestRegValue());
		dest.setSourceReg1Addr(source.getSourceReg1Addr());
		dest.setSourceReg1Value(source.getSourceReg1Value());
		dest.setSourceReg2Addr(source.getSourceReg2Addr());
		dest.setSourceReg2Value(source.getSourceReg2Value());
		dest.setSourceReg3Value(source.getSourceReg3Value());
		dest.setSourcePSWZeroFlagReg(source.getSourcePSWZeroFlagReg());
		dest.setSourcePSWZeroFlagValue(source.isSourcePSWZeroFlagValue());		
		dest.setTargetMemoryAddr(source.getTargetMemoryAddr());
		dest.setTargetMemoryValue(source.getTargetMemoryValue());
		dest.setUpdatePSW(source.canUpdatePSW());
		dest.setZeroPSW(source.isZeroPSW());
		dest.setNegativePSW(source.isNegativePSW());
		dest.setCarryPSW(source.isCarryPSW());
		dest.setLsqIndex(source.getLsqIndex());
		dest.setRobIndex(source.getRobIndex());
		dest.setTagCFID(source.getTagCFID());
		dest.setDispatchCycle(source.getDispatchCycle());
		
		if (IConstants.IOperations.LOAD.equalsIgnoreCase(source.getOpCode())) {
			dest.setLoadInstruction(true);
		}
	}

//	private void copyData(InstructionInfo dest, LSQEntry source) {
//		
//		dest.setProgramCounter(source.getProgramCounter());
//		dest.setInstruction(source.getInstruction());
//		dest.setOpCode(source.getOpCode());
//		dest.setDestRegAddr(source.getDestRegAddr());
//		dest.setDestRegValue(source.getDestRegValue());
//		dest.setSourceReg1Addr(source.getSourceReg1Addr());
//		dest.setSourceReg1Value(source.getSourceReg1Value());
//		dest.setSourceReg2Addr(source.getSourceReg2Addr());
//		dest.setSourceReg2Value(source.getSourceReg2Value());
//		dest.setSourceReg3Value(source.getSourceReg3Value());
//		dest.setTargetMemoryAddr(source.getTargetMemoryAddr());
//		dest.setTargetMemoryValue(source.getTargetMemoryValue());
//		dest.setUpdatePSW(source.canUpdatePSW());
//		dest.setZeroPSW(source.isZeroPSW());
//		dest.setNegativePSW(source.isNegativePSW());
//		dest.setCarryPSW(source.isCarryPSW());
//		dest.setLsqIndex(source.getLsqIndex());
//		dest.setRobIndex(source.getRobIndex());
//	}
//
//	private void copyData(InstructionInfo dest, IQEntry source) {
//		
//		dest.setProgramCounter(source.getProgramCounter());
//		dest.setInstruction(source.getInstruction());
//		dest.setOpCode(source.getOpCode());
//		dest.setDestRegAddr(source.getDestRegAddr());
//		dest.setDestRegValue(source.getDestRegValue());
//		dest.setSourceReg1Addr(source.getSourceReg1Addr());
//		dest.setSourceReg1Value(source.getSourceReg1Value());
//		dest.setSourceReg2Addr(source.getSourceReg2Addr());
//		dest.setSourceReg2Value(source.getSourceReg2Value());
//		dest.setSourceReg3Value(source.getSourceReg3Value());
//		dest.setTargetMemoryAddr(source.getTargetMemoryAddr());
//		dest.setTargetMemoryValue(source.getTargetMemoryValue());
//		dest.setUpdatePSW(source.canUpdatePSW());
//		dest.setZeroPSW(source.isZeroPSW());
//		dest.setNegativePSW(source.isNegativePSW());
//		dest.setCarryPSW(source.isCarryPSW());
//		dest.setLsqIndex(source.getLsqIndex());
//		dest.setRobIndex(source.getRobIndex());
//	}
	
	private void copyData(IQEntry dest, InstructionInfo source) {
		
		dest.setProgramCounter(source.getProgramCounter());
		dest.setInstruction(source.getInstruction());
		dest.setOpCode(source.getOpCode());
		dest.setDestRegAddr(source.getDestRegAddr());
		dest.setDestRegValue(source.getDestRegValue());
		dest.setSourceReg1Addr(source.getSourceReg1Addr());
		dest.setSourceReg1Value(source.getSourceReg1Value());
		dest.setSourceReg2Addr(source.getSourceReg2Addr());
		dest.setSourceReg2Value(source.getSourceReg2Value());
		dest.setSourceReg3Value(source.getSourceReg3Value());
		dest.setSourcePSWZeroFlagReg(source.getSourcePSWZeroFlagReg());
		dest.setSourcePSWZeroFlagValue(source.isSourcePSWZeroFlagValue());
		dest.setTargetMemoryAddr(source.getTargetMemoryAddr());
		dest.setTargetMemoryValue(source.getTargetMemoryValue());
		dest.setUpdatePSW(source.canUpdatePSW());
		dest.setZeroPSW(source.isZeroPSW());
		dest.setNegativePSW(source.isNegativePSW());
		dest.setCarryPSW(source.isCarryPSW());
		dest.setLsqIndex(source.getLsqIndex());
		dest.setRobIndex(source.getRobIndex());
		dest.setTagCFID(source.getTagCFID());
		dest.setDispatchCycle(source.getDispatchCycle());
	}
	
	private void copyData(InstructionInfo dest, LSQEntry source) {
		
		dest.setProgramCounter(source.getProgramCounter());
		dest.setInstruction(source.getInstruction());
		dest.setOpCode(source.getOpCode());
		dest.setDestRegAddr(source.getDestRegAddr());
		dest.setDestRegValue(source.getDestRegValue());
		dest.setSourceReg1Addr(source.getSourceReg1Addr());
		dest.setSourceReg1Value(source.getSourceReg1Value());
		dest.setSourceReg2Addr(source.getSourceReg2Addr());
		dest.setSourceReg2Value(source.getSourceReg2Value());
		if (IConstants.IOperations.STORE.equalsIgnoreCase(source.getOpCode())) {
			dest.setTargetMemoryValue(source.getSourceReg3Value());
		} else {
			dest.setSourceReg3Value(source.getSourceReg3Value());
		}
		dest.setSourcePSWZeroFlagReg(source.getSourcePSWZeroFlagReg());
		dest.setSourcePSWZeroFlagValue(source.isSourcePSWZeroFlagValue());
		dest.setTargetMemoryAddr(source.getTargetMemoryAddr());
		dest.setTargetMemoryValue(source.getTargetMemoryValue());
		dest.setUpdatePSW(source.canUpdatePSW());
		dest.setZeroPSW(source.isZeroPSW());
		dest.setNegativePSW(source.isNegativePSW());
		dest.setCarryPSW(source.isCarryPSW());
		dest.setLsqIndex(source.getLsqIndex());
		dest.setRobIndex(source.getRobIndex());
		dest.setTagCFID(source.getTagCFID());
		dest.setDispatchCycle(source.getDispatchCycle());
	}
	
	private void copyData(InstructionInfo dest, InstructionInfo source) {
		
		dest.setProgramCounter(source.getProgramCounter());
		dest.setInstruction(source.getInstruction());
		dest.setOpCode(source.getOpCode());
		dest.setDestRegAddr(source.getDestRegAddr());
		dest.setDestRegValue(source.getDestRegValue());
		dest.setSourceReg1Addr(source.getSourceReg1Addr());
		dest.setSourceReg1Value(source.getSourceReg1Value());
		dest.setSourceReg2Addr(source.getSourceReg2Addr());
		dest.setSourceReg2Value(source.getSourceReg2Value());
		dest.setSourceReg3Value(source.getSourceReg3Value());
		dest.setSourcePSWZeroFlagReg(source.getSourcePSWZeroFlagReg());
		dest.setSourcePSWZeroFlagValue(source.isSourcePSWZeroFlagValue());
		dest.setTargetMemoryAddr(source.getTargetMemoryAddr());
		dest.setTargetMemoryValue(source.getTargetMemoryValue());
		dest.setUpdatePSW(source.canUpdatePSW());
		dest.setZeroPSW(source.isZeroPSW());
		dest.setNegativePSW(source.isNegativePSW());
		dest.setCarryPSW(source.isCarryPSW());
		dest.setLsqIndex(source.getLsqIndex());
		dest.setRobIndex(source.getRobIndex());
		dest.setTagCFID(source.getTagCFID());
		dest.setDispatchCycle(source.getDispatchCycle());
	}
	
	private static void displayHeader() {
		System.out.println();
		System.out.println("-------------------------------------------------------------------------------");
		System.out.println("CS 520-03 Computer Architecture and Organization");
		System.out.println("Fall 2017 Programming Project");
		System.out.println("APEX Pipeline Simulator - Part 3");
		System.out.println("-------------------------------------------------------------------------------");
	}
	
	private static void DisplayHelp() {
		System.out.println();
		System.out.println("Usage:");
		System.out.println("java MySimpleSimulator <InstructionsFile>");
		System.out.println();
		return;
	}
	
//	private boolean isRegisterValid(int index) {		
//		return regFile.getRegister(index).getStatus();
//	}
	
	private void assertRegisterValidity(int index) throws InvalidRegisterAddressException {
		if (index < IConstants.REGISTER_ADDRESS_MIN || index > IConstants.REGISTER_ADDRESS_MAX) {
			throw new InvalidRegisterAddressException("Invalid register R" + index + " accessed");
		}
	}
	
	private void assertDataMemoryValidity(int index) throws InvalidDataMemoryAccessedException {
		if (index < IConstants.DATA_MEMORY_ADDRESS_MIN || index > IConstants.DATA_MEMORY_ADDRESS_MAX || 0 != (index % 4)) {
			throw new InvalidDataMemoryAccessedException("Invalid data memory with base address " + index + " accessed");
		}
	}
	
	private void assertCodeMemoryValidity(int index) throws InvalidCodeMemoryAccessedException {
		if ((-1 != index && index < IConstants.CODE_MEMORY_ADDRESS_MIN) || 0 != (index % 4)) {
			throw new InvalidCodeMemoryAccessedException("Invalid code memory with base address " + index + " accessed");
		}
	}

	private static Instruction parseInstruction(String instr) {
		
		if (null == instr || instr.isEmpty()) {
			return null;
		}
		
		Instruction parsedInstruction;
		String[] tokens = instr.split("\\s*(=>|,|\\s)\\s*");
		
		switch (tokens.length) {
			case 1:
				parsedInstruction = new Instruction(tokens[0].trim(), null, null, null);
				break;
			case 2:
				parsedInstruction = new Instruction(tokens[0].trim(), tokens[1].trim().replace("#", ""),
						null, null);
				break;
			case 3:
				parsedInstruction = new Instruction(tokens[0].trim(), tokens[1].trim().replace("#", ""),
						tokens[2].trim().replace("#", ""), null);
				break;
			case 4:
				parsedInstruction = new Instruction(tokens[0].trim(), tokens[1].trim().replace("#", ""),
						tokens[2].trim().replace("#", ""), tokens[3].trim().replace("#", ""));
				break;
			default:
				parsedInstruction = null;
		}
		
		return parsedInstruction;
	}
	
}
