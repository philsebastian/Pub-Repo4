import java.util.ArrayList;

public class DoubleQueue {
	
	private ArrayList<StringBuilder> theQueue;
	private int maxSize;
	private BTree totalTree;
	
	public DoubleQueue(int size, BTree theTree) {
		this.theQueue = new ArrayList<StringBuilder>();
		this.maxSize = size;
		this.totalTree = theTree;
	}
	
	public void emptyQueue() {
		this.theQueue = new ArrayList<StringBuilder>();
	}
	
	public void add(String e) {
		theQueue.add(new StringBuilder(e));
		if (theQueue.size() == maxSize) {
			ArrayList<StringBuilder> newQueue = new ArrayList<StringBuilder>();
			StringBuilder outputString = new StringBuilder();
			boolean doneFirst = false;
			StringBuilder retString;
			while (theQueue.size() > 0){
				retString = theQueue.remove(0);
				if (doneFirst) {
					outputString.append(retString);
					newQueue.add(retString);
				} else {
					outputString.append(retString);
					doneFirst = true;
				}
			}
			theQueue = newQueue;
			
			totalTree.insert(outputString.toString());	
		}
	}

}
