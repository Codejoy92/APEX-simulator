
public class LoadStoreQueueOld {

//	LSQEntry[] lsq;
//	int head;
//	int tail;
//	
//	LSQEntry tempHeadEntry;
//	
//	public LoadStoreQueueOld(int capacity) {
//		super();
//		this.lsq = new LSQEntry[capacity];
//		for (int cnt = 0; cnt < capacity; cnt++) {
//			this.lsq[cnt] = new LSQEntry();
//		}
//		
//		this.head = 0;
//		this.tail = 0;
//		
//		tempHeadEntry = new LSQEntry();
//	}
//	
//	public void setTempHeadEntry(LSQEntry tempEntry) {
//		this.tempHeadEntry = new LSQEntry(tempEntry);
//		this.tempHeadEntry.setAllocated(true);
//	}
//	
//	public LSQEntry getTempHeadEntry() {
//		return tempHeadEntry;
//	}
//	
//	public LSQEntry getLSQEntry(int index) {
//		if (0 > index || index >= this.lsq.length) {
//			throw new ArrayIndexOutOfBoundsException("Index not present: " + index);
//		}
//		
//		return this.lsq[index];
//	}
//	
//	public boolean setLSQEntry(int index, LSQEntry entry) {
//		if (0 > index || index >= this.lsq.length) {
//			throw new ArrayIndexOutOfBoundsException("Index not present: " + index);
//		}
//		
//		if (null != entry) {
//			this.lsq[index] = entry;
//			return true;
//		}
//		
//		return false;
//	}
//
//	public boolean addEntry(LSQEntry entry) {
//		
//		if (this.tail == this.head && (true == this.lsq[head].isAllocated())) {
//			// It means the queue is full
//			return false;
//		}
//		
//		this.lsq[tail] = entry;
//		this.tail = (this.tail + 1) % this.lsq.length;
//		return true;
//	}
//	
//	public LSQEntry peek() {
//		if (this.head == this.tail && (false == this.lsq[head].isAllocated())) {
//			// It means the queue is empty
//			return null;
//		}
//		
//		LSQEntry entry = new LSQEntry(this.lsq[this.head]);
//		return entry;
//
//	}
//	
//	public LSQEntry remove() {
//		
//		if (this.head == this.tail && (false == this.lsq[head].isAllocated())) {
//			// It means the queue is empty
//			return null;
//		}
//		
//		LSQEntry entry = new LSQEntry(this.lsq[this.head]);
//
//		this.lsq[this.head].reset();
//		this.head = (this.head + 1) % this.lsq.length;
//		
//		return entry;
//	}
//	
//	public boolean isEmpty( ) {
//		if (this.head == this.tail && (false == this.lsq[head].isAllocated())) {
//			// It means the queue is empty
//			return true;
//		}
//		
//		return false;
//	}
//	
//	public boolean isFull() {
//		if (this.tail == this.head && (true == this.lsq[head].isAllocated())) {
//			// It means the queue is full
//			return true;
//		}
//		
//		return false;
//	}
//	
//	public LSQEntry[] getLsq() {
//		return lsq;
//	}
//
//	public void setLsq(LSQEntry[] lsq) {
//		this.lsq = lsq;
//	}
//
//	public int getTail() {
//		return tail;
//	}
//
//	public void setTail(int tail) {
//		this.tail = tail;
//	}
//	
//	public int getHead() {
//		return this.head;
//	}
//
//	public void setHead(int head) {
//		this.head = head;
//	}
}
