import java.util.ArrayList;
import java.util.List;

public class LoadStoreQueue {

	ArrayList<LSQEntry> lsq;
//	int head;
//	int tail;
	
	LSQEntry tempHeadEntry;
	
	public LoadStoreQueue(int capacity) {
		super();
		this.lsq = new ArrayList<LSQEntry>(capacity);
		tempHeadEntry = new LSQEntry(-1);
	}
	
	public void resetTempHeadEntry () {
		tempHeadEntry.reset();
	}
	
	public void setTempHeadEntry(LSQEntry tempEntry) {
		this.tempHeadEntry = new LSQEntry(tempEntry);
		this.tempHeadEntry.setAllocated(true);
	}
	
	public LSQEntry getTempHeadEntry() {
		return tempHeadEntry;
	}
	
	public LSQEntry getLSQEntry(int index) {
//		if (0 > index || index >= this.lsq.length) {
//			throw new ArrayIndexOutOfBoundsException("Index not present: " + index);
//		}
		
		for (int cnt = 0; cnt < lsq.size(); cnt++) {
			if (null != lsq.get(cnt) && index == lsq.get(cnt).getIndex()) {
				return lsq.get(cnt);
			}
		}
		
		// TODO: Throw an exception instead of returning null
		return null;
	}
	
	public boolean setLSQEntry(LSQEntry entry) {
//		if (0 > index || index >= this.lsq.length) {
//			throw new ArrayIndexOutOfBoundsException("Index not present: " + index);
//		}
		
		if (null != entry) {
			this.lsq.add(entry);
			return true;
		}
		
		return false;
	}

	public boolean addEntry(LSQEntry entry) {
		
		if (IConstants.LOAD_STORE_QUEUE_CAPACITY == this.lsq.size()) {
			// It means the queue is full
			return false;
		}
		
		this.lsq.add(entry);
		return true;
	}
	
	public LSQEntry peek() {
		if (0 == this.lsq.size()) {
			// It means the queue is empty
			return null;
		}
		
		LSQEntry entry = this.lsq.get(0);
		return entry;
	}
	
	public LSQEntry remove() {
		
		if (0 == this.lsq.size()) {
			// It means the queue is empty
			return null;
		}

		return this.lsq.remove(0);
	}
	
	public boolean isEmpty( ) {
		if (0 == this.lsq.size()) {
			// It means the queue is empty
			return true;
		}

		
		return false;
	}
	
	public boolean isFull() {
		if (IConstants.LOAD_STORE_QUEUE_CAPACITY == this.lsq.size()) {
			// It means the queue is full
			return true;
		}
		
		return false;
	}
	
	public List<LSQEntry> getLsq() {
		return lsq;
	}

	public void setLsq(List<LSQEntry> lsq) {
		this.lsq = (ArrayList<LSQEntry>) lsq;
	}

	public int getPosition(int lsqIndex) {
		for (int index = 0; index < this.lsq.size(); index++) {
			if (lsqIndex == this.lsq.get(index).getIndex()) {
				return index;
			}
		}
		
		// TODO: Return an exception instead of -1
		return -1;
	}
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
