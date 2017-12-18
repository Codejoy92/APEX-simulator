
interface IConstants {
	public static int DATA_MEMORY_BASE_ADDRESS = 0;
	public static int DATA_MEMORY_COUNT_WORDS = 1000;
	public static int REGISTER_COUNT = 16;
	public static int PHYSICAL_REGISTER_COUNT = 32;
	public static int CODE_MEMORY_BASE_ADDRESS = 4000;
	public static int INSTRUCTION_SIZE = 4;
	
	public static int REGISTER_ADDRESS_MIN = 0;
	public static int REGISTER_ADDRESS_MAX = 15;
	
	public static int DATA_MEMORY_ADDRESS_MIN = 0;
	public static int DATA_MEMORY_ADDRESS_MAX = 3999;
	
	public static int CODE_MEMORY_ADDRESS_MIN = 4000;
	
	public static int PSW_ARCH_REGISTER_INDEX = 16;
	
	public static int MEMORY_STAGE_LATENCY = 3;

	public static int ISSUE_QUEUE_CAPACITY = 16;
	public static int LOAD_STORE_QUEUE_CAPACITY = 32;
	public static int ROB_CAPACITY = 32;
	public static int DISPLAY_MEMORY_WORDS_MAX = 100;

	public static byte ARCHITECTURAL_REGISTER = 0;
	public static byte PHYSICAL_REGISTER = 1;
	
	public interface IOperations {
		public static String ADD = "ADD";
		public static String SUB = "SUB";
		public static String MOVC = "MOVC";
		public static String AND = "AND";
		public static String OR = "OR";
		public static String EXOR = "EX-OR";
		public static String MUL = "MUL";
		public static String DIV = "DIV";
		public static String LOAD = "LOAD";
		public static String STORE = "STORE";
		public static String BZ = "BZ";
		public static String BNZ = "BNZ";
		public static String JUMP = "JUMP";
		public static String JAL = "JAL";
		public static String HALT = "HALT";
	}
	
	public interface IFunctionalUnits {
		public static int INTFU = 1;
		public static int MULFU = 2;
		public static int DIVFU = 3;
	}
}