import java.util.ArrayList;
import java.util.List;

class CodeMemory {
	private ArrayList<CodeLine> codeMemory;
	
	public CodeMemory() {
		codeMemory = null;
	}
	
	public CodeMemory(List<CodeLine> codeLines) {
		codeMemory = (ArrayList<CodeLine>)codeLines;
	}
	
	public boolean isEmpty() {
		if (null == codeMemory || codeMemory.isEmpty()) {
			return true;
		}
		
		return false;
	}
	
	public List<CodeLine> getCodeMemory() {
		return codeMemory;
	}
	
	public void setCodeMemory(List<CodeLine> codeLines) {
		codeMemory = (ArrayList<CodeLine>)codeLines;
	}
	
	public CodeLine getCodeLine(int address) {
	
		for (CodeLine codeLine: codeMemory) {
			if (address == codeLine.getInstAddress()) {
				return codeLine;
			}
		}
		
		return null;
	}
	
	public int getFileLineNumber(int instAddress) {
		for (CodeLine instr : this.codeMemory) {
			if (instr.getInstAddress() == instAddress) {
				return instr.getFileLineNumber();
			}
		}
		
		return -1;
	}
	
	public CodeLine[] toArray() {
		return (CodeLine[]) codeMemory.toArray();
	}
}