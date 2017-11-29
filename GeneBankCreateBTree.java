import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

public class GeneBankCreateBTree {

	private static final String COMMANDLINE_INSTRUCTIONS = "This program requires the following startup parameters.\n";
	private static final String FIRST_PARAMETER_ERROR = "The first commandline arguments needs to be either 0 or 1.";
	private static final String SECOND_PARAMETER_ERROR = "The second commandline argument needs to be a integer greater or equal to zero.";
	private static final String THIRD_PARAMETER_ERROR = "The fourth commandline argument needs to be a integer between 1 and 31 (inclusive).";
	private static final String PARSE_INT_ERROR = "The cache and debug need to be valid integers.";
	private static final String DEBUG_ERROR = "The debug can only be set to 0 or 1.";
	private static final String FILE_NOT_FOUND = "Unable to locate or read file: ";

	private DoubleQueue newQueue;
	private BTree newBTree;
	private int debug;
	
	private GeneBankCreateBTree (int degree, int sequenceLength, int cacheSize, String fileName, int debugValue) {
		newBTree = new BTree(degree, sequenceLength, cacheSize, (fileName + ".newBTree.data." + sequenceLength + "." + degree));
		newQueue = new DoubleQueue(sequenceLength, newBTree);
		debug = debugValue;
	}
	
	private void add(String s) {
		newQueue.add(s);
	}
	
	private void resetQueue() {
		newQueue.emptyQueue();
	}
	
	private void debugPrint(String err) {
		if(debug == 0) {
			System.err.println(err);
			System.exit(1);
		}
	}
	
	public static void main (String[] args) {		
		boolean withCache;
		String gbkFile;
		int cacheSize = -1, degree = -1, sequenceLength = -1, debugLevel = -1;
		
		// Parses command line arguments
		// Verify correct number of arguments
		if (args.length < 3 || args.length > 6) {	
			System.err.println(COMMANDLINE_INSTRUCTIONS);
			System.exit(1);
		}
		
		// Parse argument one
		int arg0 = -1;
		try {
			arg0 = Integer.parseInt(args[0]); 
		} catch (NumberFormatException e) {
			System.err.println(FIRST_PARAMETER_ERROR);
			System.exit(1);
		}
		if (arg0 < 0 || arg0 > 1) {
			System.err.println(FIRST_PARAMETER_ERROR);
			System.exit(1);
		}
		if (arg0 == 1) {
			withCache = true;
		} else {
			withCache = false;
		}
		
		// Parse argument two
		int arg1 = -1;
		try {
			arg1 = Integer.parseInt(args[1]); 
		} catch (NumberFormatException e) {
			System.err.println(SECOND_PARAMETER_ERROR);
			System.exit(1);
		}
		degree = arg1;
		
		// Parse argument three
		gbkFile = args[2];
		
		// Parse argument four
		int arg4 = -1;
		try {
			arg4 = Integer.parseInt(args[3]); 
		} catch (NumberFormatException e) {
			System.err.println(THIRD_PARAMETER_ERROR);
			System.exit(1);
		}
		if (arg4 < 1 || arg4 > 31) {
			System.err.println(THIRD_PARAMETER_ERROR);
			System.exit(1);
		}
		sequenceLength = arg4;
		
		int arg5 = -1, arg6 = -1;
		switch(args.length){
			case 5:
				try {
					arg5 = Integer.parseInt(args[4]); 
				} catch (NumberFormatException e) {
					System.err.println(PARSE_INT_ERROR);
					System.exit(1);
				}
				if (arg5 > 1) {
					cacheSize = arg5;
				} else if (arg5 > 0) {
					debugLevel = arg5;
				} else {
					System.err.println(PARSE_INT_ERROR);
					System.exit(1);
				}
				break;
			case 6:
				try {
					arg5 = Integer.parseInt(args[4]); 
				} catch (NumberFormatException e) {
					System.err.println(PARSE_INT_ERROR);
					System.exit(1);
				}
				if (arg5 >= 0) {
					cacheSize = arg5;
				} else {
					System.err.println(PARSE_INT_ERROR);
					System.exit(1);
				}
				try {
					arg6 = Integer.parseInt(args[5]); 
				} catch (NumberFormatException e) {
					System.err.println(PARSE_INT_ERROR);
					System.exit(1);
				}
				if (arg6 >= 0 && arg6 < 2) {
					debugLevel = arg6;
				} else {
					System.err.println(DEBUG_ERROR);
					System.exit(1);
				}
				break;
			default:
				break;
		}	
		
		if (!withCache) {
			cacheSize = 0;
		}
		
		GeneBankCreateBTree thisBank = new GeneBankCreateBTree(degree, sequenceLength, cacheSize, gbkFile, debugLevel);

		processFile(thisBank, gbkFile, sequenceLength);
		
		thisBank.debugPrint("File done");
		
		if (debugLevel == 1) {
			thisBank.newBTree.dump();
		}
	}
	

	public static void processFile(GeneBankCreateBTree thisBTree, String fileName, int sequenceLength) {
		
		BufferedReader reader = null;
		String line;
		StringTokenizer stringLine;
		boolean startFound = false;
		char[] charArray =  null;
		StringBuilder token = new StringBuilder();
		
		try {
			reader = new BufferedReader(new FileReader(fileName));
		} catch (FileNotFoundException err) {
			thisBTree.debugPrint(err.toString());
			thisBTree.debugPrint(FILE_NOT_FOUND + fileName);
		}
		long lineNumber = 0;
		try {
			line = reader.readLine();
			while (line != null) {
				if (thisBTree.debug == 0) {
					//System.err.println(++lineNumber);
				}
				stringLine = new StringTokenizer(line);
				while (stringLine.hasMoreTokens()) {
					token = new StringBuilder(stringLine.nextToken());
					if (!startFound) {
						startFound = (token.toString().equals("ORIGIN")); 
					}
					if (startFound) {
						if (!token.toString().equals("ORIGIN")) {
							if (token.toString().equals("//")) {
								if (thisBTree.debug == 0) {
									System.err.println("//");
								}
								startFound = false;
								thisBTree.resetQueue();
							} 
							charArray = line.toLowerCase().toCharArray();
							for (int i = 0; i < charArray.length - sequenceLength; i++) {
								
								switch (charArray[i]) {
									case 'a':
										thisBTree.add("a");
										break;
									case 't':
										thisBTree.add("t");
										break;
									case 'c':
										thisBTree.add("c");
										break;
									case 'g':
										thisBTree.add("g");
										break;
									default:
										break;
								}
							}
						}						
					}
				}
				line = reader.readLine();				
			}				
		} catch (IOException err) {
			thisBTree.debugPrint(err.toString());
		}
		try {
			reader.close();
		} catch (IOException err) {
			thisBTree.debugPrint(err.toString());
		}
	}
}