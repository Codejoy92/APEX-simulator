
class Stats {
	private static int cycle = 0;
	private static int committedInstr = 0;

	public synchronized static int incrementCycle() {
		cycle++;
		return cycle;
	}
	
	public static int getCycle() {
		return cycle;
	}
	
	public static void setCycle(int cycleTemp) {
		cycle = cycleTemp;
	}

	public static int getCommittedInstr() {
		return committedInstr;
	}

	public static void setCommittedInstr(int committedInstr) {
		Stats.committedInstr = committedInstr;
	}
	
	public synchronized static int incrementCommittedInstructions() {
		Stats.committedInstr++;
		return Stats.committedInstr;
	}
}