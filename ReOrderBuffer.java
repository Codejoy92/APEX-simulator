
public class ReOrderBuffer {

	ROBEntry[] rob;
	int head;
	int tail;
	
	ROBEntry tempHeadEntry;
	
	public ReOrderBuffer(int capacity) {
		super();
		this.rob = new ROBEntry[capacity];
		for (int cnt = 0; cnt < capacity; cnt++) {
			this.rob[cnt] = new ROBEntry();
		}
		
		this.head = 0;
		this.tail = 0;
		
		//tempHeadEntry = new ROBEntry();
	}
	
//	public void setTempHeadEntry(LSQEntry tempEntry) {
//		this.tempHeadEntry = new LSQEntry(tempEntry);
//		this.tempHeadEntry.setAllocated(true);
//	}
	
//	public ROBEntry getTempHeadEntry() {
//		return tempHeadEntry;
//	}

	public ROBEntry getROBEntry(int index) {
		if (0 > index || index >= this.rob.length) {
			throw new ArrayIndexOutOfBoundsException("Index not present: " + index);
		}
		
		return this.rob[index];
	}
	
	public boolean setROBEntry(int index, ROBEntry entry) {
		if (0 > index || index >= this.rob.length) {
			throw new ArrayIndexOutOfBoundsException("Index not present: " + index);
		}
		
		if (null != entry) {
			this.rob[index] = entry;
			return true;
		}
		
		return false;
	}

	public boolean addEntry(ROBEntry entry) {
		
		if (this.tail == this.head && (true == this.rob[head].isAllocated())) {
			// It means the queue is full
			return false;
		}
		
		this.rob[tail] = entry;
		this.tail = (this.tail + 1) % this.rob.length;
		return true;
	}
	
	public ROBEntry peekEx(int level) {

		int temp = this.head;
		temp = (temp + level) % this.rob.length;
		if (temp == this.tail && (false == this.rob[temp].isAllocated())) {
			// It means the queue will become empty after "level" elements are removed from the queue
			return null;
		}
		
		ROBEntry entry = new ROBEntry(this.rob[temp]);
		return entry;
	}
	
	public ROBEntry peek() {
		if (this.head == this.tail && (false == this.rob[head].isAllocated())) {
			// It means the queue is empty
			return null;
		}
		
		ROBEntry entry = new ROBEntry(this.rob[this.head]);
		return entry;

	}
	
	public ROBEntry remove() {
		
		if (this.head == this.tail && (false == this.rob[head].isAllocated())) {
			// It means the queue is empty
			return null;
		}
		
		ROBEntry entry = new ROBEntry(this.rob[this.head]);

		this.rob[this.head].reset();
		this.head = (this.head + 1) % this.rob.length;
		
		return entry;
	}
	
	public boolean isEmpty( ) {
		if (this.head == this.tail && (false == this.rob[head].isAllocated())) {
			// It means the queue is empty
			return true;
		}
		
		return false;
	}
	
	public boolean isFull() {
		if (this.tail == this.head && (true == this.rob[head].isAllocated())) {
			// It means the queue is full
			return true;
		}
		
		return false;
	}
	
	public ROBEntry[] getRob() {
		return rob;
	}

	public void setRob(ROBEntry[] rob) {
		this.rob = rob;
	}

	public int getTail() {
		return tail;
	}

	public void setTail(int tail) {
		this.tail = tail;
	}
	
	public int getHead() {
		return this.head;
	}

	public void setHead(int head) {
		this.head = head;
	}
}
