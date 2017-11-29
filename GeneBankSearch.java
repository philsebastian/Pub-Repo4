import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;
import java.util.StringTokenizer;


public class GeneBankSearch {

	private static final String COMMANDLINE_INSTRUCTIONS = "This program requires the following startup parameters.\n";
	private static final String FIRST_PARAMETER_ERROR = "The first commandline arguments needs to be either 0 or 1.";
	private static final String PARSE_INT_ERROR = "The cache and debug need to be valid integers.";
	private static final String DEBUG_ERROR = "The debug can only be set to 0 or 1.";
	private static final String FILE_NOT_FOUND = "Unable to locate or read file: ";

	private int debug;
	private BTree theBTree;
	
	private GeneBankSearch(String btreeFile, int cacheSize, int debugLevel){
		this.theBTree = new BTree(btreeFile, cacheSize);
		this.debug = debugLevel;		
	}
	
	
	private void debugPrint(String err) {
		if(debug == 0) {
			System.err.println(err);
			System.exit(1);
		}
	}
	
	public static void main (String[] args) {		
		boolean withCache;
		String btreeFile, queryFile;
		int cacheSize = 0, debugLevel = -1;

		// Parse command line arguments
		// Verify correct number of arguments
		if (args.length < 3 || args.length > 5) {
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
		btreeFile = args[1];	

		// Parse argument three
		queryFile = args[2];

		int arg4 = -1, arg5 = -1;
		switch(args.length){
		case 4:
			try {
				arg4 = Integer.parseInt(args[3]); 
			} catch (NumberFormatException e) {
				System.err.println(PARSE_INT_ERROR);
				System.exit(1);
			}
			cacheSize = arg4;
			debugLevel = arg4;
			break;
		case 5:
			try {
				arg4 = Integer.parseInt(args[3]); 
			} catch (NumberFormatException e) {
				System.err.println(PARSE_INT_ERROR);
				System.exit(1);
			}
			if (arg4 >= 0) {
				cacheSize = arg4;
			} else {
				System.err.println(PARSE_INT_ERROR);
				System.exit(1);
			}
			try {
				arg5 = Integer.parseInt(args[4]); 
			} catch (NumberFormatException e) {
				System.err.println(PARSE_INT_ERROR);
				System.exit(1);
			}
			if (arg5 == 0) {
				debugLevel = arg5;
			} else {
				System.err.println(DEBUG_ERROR);
				System.exit(1);
			}
			break;
		default:
			break;
		}
		
		GeneBankSearch thisSearch = new GeneBankSearch(btreeFile, cacheSize, debugLevel);
		processFile(thisSearch, queryFile);
	}
	
	public static void processFile(GeneBankSearch thisSearch, String fileName) {
		
		BufferedReader reader = null;
		String line;
		StringTokenizer stringLine;
		StringBuilder token = new StringBuilder();
		
		try {
			reader = new BufferedReader(new FileReader(fileName));
		} catch (FileNotFoundException err) {
			thisSearch.debugPrint(err.toString());
			thisSearch.debugPrint(FILE_NOT_FOUND + fileName);
		}
		long lineNumber = 0;
		try {
			line = reader.readLine();
			while (line != null) {
				if (thisSearch.debug == 0) {
					//System.err.println(++lineNumber);
				}
				stringLine = new StringTokenizer(line);
				while (stringLine.hasMoreTokens()) {
					token = new StringBuilder(stringLine.nextToken());
					if (token.length() != thisSearch.theBTree.sequenceLength()) {	
						System.err.println("Sequence " + token.toString() + " is not the correct length.");
					} else {
						int frequency = thisSearch.theBTree.frequency(token.toString());
						if (frequency > 0) {
							System.out.println(token.toString().toLowerCase() + ": " + frequency);
						}
					}
					
				}
				line = reader.readLine();				
			}				
		} catch (IOException err) {
			thisSearch.debugPrint(err.toString());
		}
		try {
			reader.close();
		} catch (IOException err) {
			thisSearch.debugPrint(err.toString());
		}
	}
}	
	