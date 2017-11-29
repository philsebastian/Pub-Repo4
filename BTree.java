import java.util.LinkedList;
import java.util.ListIterator;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.nio.ByteBuffer;
import java.util.Iterator;


public class BTree {

	private int t;
	private RandomAccessFile file;
	private BTreeNode root;
	private int sequenceLength;
	private Cache cache;

	/**
	 * @param sequenceString string to be converted
	 *            
	 * @return the string represented in long form
	 */
	private long stringToLong(String sequenceString) {
		if (sequenceString.length() != sequenceLength) {
			throw new BTreeException("String");
		}
		long sequenceLong = 0;
		for (int i = 0; i < sequenceString.length(); i++) {

			sequenceLong = sequenceLong << 2;
			char sequenceChar = sequenceString.charAt(i);

			if (sequenceChar == 'a' || sequenceChar == 'A') {
				sequenceLong = sequenceLong | 0x0L;
			}

			else if (sequenceChar == 't' || sequenceChar == 'T') {
				sequenceLong = sequenceLong | 0x3L;
			} 

			else if (sequenceChar == 'c' || sequenceChar == 'C') {
				sequenceLong = sequenceLong | 0x1L;
			} 

			else if (sequenceChar == 'g' || sequenceChar == 'G') {
				sequenceLong = sequenceLong | 0x2L;
			} 

			else {
				throw new BTreeException("Unexpected character: " + sequenceChar);
			}
		}
		return sequenceLong;
	}

	/**
	 * Creates new BTree for the given file
	 * 
	 * @param t degree of the tree
	 * @param sequenceLength the length of the sequence
	 * @param cacheSize size of cache. 0 if not using one.      
	 * @param fileName name of the file
	 */
	public BTree(int t, int sequenceLength, int cacheSize, String fileName) {
		this.t = t;
		this.sequenceLength = sequenceLength;

		if (cacheSize == 0) {
			cache = null;	
		} 

		else if (cacheSize > 0) {
			cache = new Cache(cacheSize);
		} 

		else {
			throw new BTreeException("Negative cache size of " + cacheSize + " was given");
		}

		if (this.t == 0) {
			this.t = (4096 + 12) / 32; //optimum size for t based on node size
		} else if (this.t < 2) {
			throw new BTreeException("Degree " + t + " was given. This is an invalid degree.\n");
		}
		if (sequenceLength < 1 || sequenceLength > 31) {

			throw new BTreeException(
					"Sequence length must be between 1 and 31 (inclusive). " + sequenceLength + " was given.\n");
		}

		try {
			file = new RandomAccessFile(fileName, "rw");

			file.setLength(12);
			file.writeInt(12);
			file.writeInt(this.t);
			file.writeInt(this.sequenceLength);

			root = new BTreeNode();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Opens BTree from disk
	 * 
	 * @param fileName
	 * @param cacheSize           
	 */
	public BTree(String fileName, int cacheSize) {
		if (cacheSize == 0) {
			cache = null;
		} else if (cacheSize > 0) {
			cache = new Cache(cacheSize);
		} else {
			throw new BTreeException("Negative cache size was given");
		}
		try {
			file = new RandomAccessFile(fileName, "r");
			int rootPointer = file.readInt();
			this.t = file.readInt();
			this.sequenceLength = file.readInt();
			root = new BTreeNode(rootPointer);
		} catch (FileNotFoundException e) {
			System.err.println("Could not open query file.");
			System.out.println("java GeneBankSearch <0/1(no/with Cache)> <btree file> <query file> [<cache size>] [<debug level>]");
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}

	/**
	 * Gets length of the sequence
	 * @return the length of the sequence
	 */
	public int sequenceLength() {
		return this.sequenceLength;
	}
	
	/**
	 * @param sequence as a string
	 * @return the frequency of the sequence 
	 */
	public int frequency(String sequence) {
		return root.frequency(stringToLong(sequence));
	}

	/**
	 * Inserts a sequence into the tree 
	 * @param sequence as a String
	 */
	public void insert(String sequence) {
		long seq = stringToLong(sequence);

		if (root.isFull()) {
			
			root.fileUpdate();
			BTreeNode previousRoot = root;
			root = new BTreeNode();
			root.pointerList.add(new Integer(previousRoot.nodePointer));
			root.splitChild(0);
		}
		root.insertNonfull(seq);
	}

	/**
	 * dumps the contents of the file into a "dump" file
	 */
	public void dump() {
		try {
			FileWriter dumpFile = new FileWriter("dump");
			root.dump(dumpFile);
			dumpFile.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Make sure everything is written to disk and close the btree file. BTree
	 * will not be usable after calling this method.
	 */
	public void close() {
		try {
			file.seek(0);
			file.writeInt(root.nodePointer);
			root.fileUpdate();
			
			if (cache != null) {
				cache.empty();
			}
			
			file.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}



	private class Cache {

		private LinkedList<BTreeNode> cacheList;
		private int size;

		/**
		 * @param size of cache
		 */
		public Cache(int size) {
			
			this.size = size;
			cacheList = new LinkedList<BTreeNode>();
		}
		
		/**
		 * Adds node to cache and removes the last node and saves it to disk.
		 * 
		 * @param node         
		 */
		public void add(BTreeNode node) {
			if (cacheList.size() == size) {
				BTreeNode lastinCache = cacheList.removeLast();
				lastinCache.isCached = false;
				lastinCache.fileUpdate();
			}
			node.isCached = true;
			cacheList.addFirst(node);
		}

		/**
		 * Retrieve a node from the cache.
		 * 
		 * @param pointer that points to node being returned
		 *            
		 * @return the node that pointer is assigned to
		 */
		public BTreeNode get(int pointer) {
			
			Iterator<BTreeNode> iterator = cacheList.iterator();
			boolean isFinished = false;
			BTreeNode returnNode = null;
			
			while (!isFinished && iterator.hasNext()) {
				BTreeNode tmp = iterator.next();
				
				if (tmp.nodePointer == pointer) {
					
					isFinished = true;
					returnNode = tmp;
					iterator.remove();
					
					cacheList.addFirst(returnNode);
				}
			}
			return returnNode;
		}


		/**
		 * empty cache and update file
		 */
		public void empty() {
			while (!cacheList.isEmpty()) {
				
				BTreeNode currentNode = cacheList.removeLast();
				currentNode.isCached = false;
				currentNode.fileUpdate();
			}
		}
	}
	
	private class BTreeNode {

		public LinkedList<TreeObject> data; //key values being stored
		public LinkedList<Integer> pointerList; //pointers to the nodes
		public final int nodePointer; //index of node's location
		public boolean isCached; //is the value cached?

		/**
		 * Creates a new empty node on the disk
		 */
		public BTreeNode() {
			int n = 0;
			
			try {
				n = (int) file.length();

				file.setLength(n + 32 * t - 12); //allocate space for node based on size
				
				data = new LinkedList<TreeObject>();
				pointerList = new LinkedList<Integer>();
			} 

			catch (IOException e) {
				e.printStackTrace();
			}
			nodePointer = n;
			isCached = false;
		}

		/**
		 * Constructs a node that is read from disk based on the given pointer.
		 * 
		 * @param pointer of the node
		 *            
		 */
		public BTreeNode(int pointer) {
			
			nodePointer = pointer;
			
			try {
				file.seek(nodePointer);
				
				byte[] bytes = new byte[32 * t - 12];
				file.read(bytes);
				ByteBuffer buffer = ByteBuffer.wrap(bytes);
				data = new LinkedList<TreeObject>();

				for (int i = 0; i < 2 * t - 1; i++) {
					int frequency = buffer.getInt();
					long seq = buffer.getLong();
					
					if (frequency != 0) {
						data.add(new TreeObject(seq, frequency));
					}
				}

				pointerList = new LinkedList<Integer>();

				int first = buffer.getInt();
				if (first != 0) {
					pointerList.add(first);
				}

				if (!isLeaf()) {
					for (int i = 1; i < data.size() + 1; i++) {
						pointerList.add(new Integer(buffer.getInt()));
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			isCached = false;
		}

		/**
		 * @return true if node is full
		 */
		public boolean isFull() {
			return data.size() == 2 * t - 1;
		}

		/**
		 * @return true if node is a leaf
		 */
		public boolean isLeaf() {
			
			return pointerList.size() == 0;
		}

		/**
		 * Returns child node. Also checks if in cache.
		 * 
		 * @param idx index of child
		 * @return child of the given idx
		 */
		public BTreeNode child(int idx) {
			if (isLeaf()) {
				throw new BTreeException("This is a leaf node, of which has no children.");
			}
			int pointer = pointerList.get(idx);
			if (cache == null) {
				return new BTreeNode(pointer);
			} 
			else 
			{
				BTreeNode child = cache.get(pointer);

				if (child != null) 
				{
					return child;
				} 

				else 
				{
					child = new BTreeNode(pointer);
					cache.add(child);
					return child;
				}
			}
		}

		/**
		 * Writes a node to the disk only when it is not in the cache.
		 * TODO -- Doesn't this need write only if not found in tree?
		 */
		public void fileUpdate() {
			if (isCached == false || cache == null ) {
				try {
					ByteBuffer buffer = ByteBuffer.allocate((32 * t) - 12);


					for (int i = 0; i < data.size(); i++) { //writing data to file
						buffer.putInt(data.get(i).getFrequency());
						buffer.putLong(data.get(i).getSequence());
					}

					for (int i = data.size(); i < (2 * t - 1); i++) { //fill out rest of allocated space
						buffer.putInt(0);
						buffer.putLong(0L);
					}

					if (!isLeaf()) {
						for (int i = 0; i < pointerList.size(); i++) { //write pointer array to file
							buffer.putInt(pointerList.get(i));
						}
					}

					byte[] bytes = new byte[32 * t - 12];

					buffer.clear();
					buffer.get(bytes);

					file.seek(nodePointer);
					file.write(bytes);
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		/**
		 * Insert sequence into the non-full node
		 * 
		 * @param sequence what is getting inserted
		 */
		public void insertNonfull(long sequence) {
			if (isFull()) {
				throw new BTreeException("Node must have space");
			}

			if (data.size() == 0 && isLeaf()) {
				data.add(new TreeObject(sequence));
				return;
			}

			ListIterator<TreeObject> iterator = data.listIterator(data.size());


			int i = data.size();
			while (iterator.hasPrevious() && iterator.previous().getSequence() > sequence) {
				i--;
			}

			TreeObject current = iterator.next();

			if (sequence == current.getSequence()) {

				current.incrementFrequency();
				fileUpdate();
			}
			else if (isLeaf()) {

				data.add(i, new TreeObject(sequence));
				fileUpdate();
			} 
			else 
			{ //inserting into child node

				BTreeNode childNode = child(i);
				if (!childNode.isFull()) { //child has room
					childNode.insertNonfull(sequence);
				}

				else { //no room in child
					splitChild(i);

					TreeObject insertedValue = data.get(i);
					if (sequence == insertedValue.getSequence()) {

						insertedValue.incrementFrequency();
						fileUpdate();
					} 
					else if (insertedValue.getSequence() < sequence) {
						child(i + 1).insertNonfull(sequence);
					} 
					else {
						child(i).insertNonfull(sequence);// go ahead and insert into the child node	
					}
				}
			}
		}

		/**
		 * Splits the child and updates the file.
		 */
		public void splitChild(int index) {
			BTreeNode currentChild = child(index);
			BTreeNode newChild = new BTreeNode();

			if (!currentChild.isFull()) {
				throw new BTreeException("This child does not need to be split");
			}

			pointerList.add(index + 1, new Integer(newChild.nodePointer));

			while (currentChild.data.size() > t) {
				newChild.data.addFirst(currentChild.data.removeLast());
			}

			data.add(index, currentChild.data.removeLast());

			while (currentChild.pointerList.size() > t) { //adjust the pointers
				newChild.pointerList.addFirst(currentChild.pointerList.removeLast());
			}


			if (nodePointer != root.nodePointer)
				fileUpdate();
			currentChild.fileUpdate();
			newChild.fileUpdate();
		}

		/**
		 * @param sequence
		 * @return the frequency of the given sequence
		 */
		public int frequency(long sequence) {
			ListIterator<TreeObject> iterator = data.listIterator(data.size());

			int i = data.size();
			while (iterator.hasPrevious() && iterator.previous().getSequence() > sequence) { //find insertion point
				i--;
			}
			TreeObject curr = iterator.next();
			if (sequence == curr.getSequence()) { //if found

				return curr.getFrequency();
			} 
			else if (isLeaf()) {
				return 0;
			} 
			else {
				return child(i).frequency(sequence);
			}
		}

		/**
		 * Dumps the TreeObjects into a dump file
		 * 
		 * @param dumpFile
		 *            
		 */
		public void dump(FileWriter dumpFile) {
			if (!isLeaf()) {
				
				ListIterator<TreeObject> iter = data.listIterator();
				child(iter.nextIndex()).dump(dumpFile);
				
				while (iter.hasNext()) {
					try {
						dumpFile.write(iter.next() + "\n");			
					} 
					catch (IOException e) {
						e.printStackTrace();
					}

					child(iter.nextIndex()).dump(dumpFile);
				}
				
			} 
			else {
				ListIterator<TreeObject> iterator = data.listIterator();
				while (iterator.hasNext()) {
					try {
						dumpFile.write(iterator.next() + "\n");
					} 
					catch (IOException e) {
						e.printStackTrace();
					}
				}
				
			}

		}
	}
	
	
	
	

	private class TreeObject {

		private long sequence;
		private int frequency;

		/**
		 * Constructs a new TreeObject and sets the frequency to 1
		 * 
		 * @param sequence the object's sequence in long form           
		 */
		public TreeObject(long sequence) {
			frequency = 1;
			this.sequence = sequence;
		}

		/**
		 * Construct a TreeObject from the file
		 * 
		 * @param sequence the sequence in long form          
		 * @param frequency of the sequence            
		 */
		public TreeObject(long sequence, int frequency) {
			this.sequence = sequence;
			this.frequency = frequency;
		}

		/**
		 * @return the sequence in binary
		 */
		public long getSequence() {
			return sequence;
		}

		/**
		 * @return the frequency of the sequence
		 */
		public int getFrequency() {
			return frequency;
		}

		/**
		 * Increment the frequency of the sequence
		 */
		public void incrementFrequency() {
			frequency++;
		}

		/**
		 * The toString() of the TreeObject
		 */
		public String toString() {
			char[] sequenceArray = new char[sequenceLength];
			long binMask = 0x3L;
			long sequenceShifter = sequence;
			
			for (int i = sequenceLength - 1; i >= 0; i--) {
				long current = sequenceShifter & binMask;
				
				if (current == 0x0L) {
					sequenceArray[i] = 'A';
				} 
				else if (current == 0x3L) {
					sequenceArray[i] = 'T';
				} 
				else if (current == 0x1L) {
					sequenceArray[i] = 'C';
				} 
				else if (current == 0x2L) {
					sequenceArray[i] = 'G';
				}
				sequenceShifter = sequenceShifter >>> 2;
			}
			return frequency + " " + new String(sequenceArray);
		}
	}

	
}
