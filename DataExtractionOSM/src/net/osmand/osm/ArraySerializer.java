package net.osmand.osm;

/**
 * Serializes custom arrays of strings into string using [,] notation with quotation character \ 
 * Examples : [1, 2, 3, [4, 5]]
 * @author victor
 *
 */
public class ArraySerializer {
	
	public static final int START_ARRAY = 1;
	public static final int ELEMENT = 2;
	public static final int END_ARRAY = 3;
	public static final int END = 4;
	
	
	public static class EntityValueTokenizer {
		private String tokenize;
		private int lastReadIndex;
		private String value;
		private boolean firstElementAfterArrayOpened;
		
		public EntityValueTokenizer(){
			tokenize("");
		}
		public void tokenize(String v){
			lastReadIndex = 0;
			tokenize = v;
			value = "";
			firstElementAfterArrayOpened = false;
		}
		
		public int next(){
			value ="";
			int currentInd = lastReadIndex;
			lastReadIndex ++;
			if(currentInd >= tokenize.length()){
				return END;
			}
			// check if current element is opening array
			if(tokenize.charAt(currentInd) == '['){
				firstElementAfterArrayOpened = true;
				return START_ARRAY;
			}
			// check if current element is closing array
			if(tokenize.charAt(currentInd) == ']'){
				return END_ARRAY;
			}
			
			// check if current element is comma
			// 2 ways : skip it and treat as empty element
			int startRead = currentInd;
			if(tokenize.charAt(currentInd) == ','){
				if(firstElementAfterArrayOpened) {
					// special case for : '[,' return empty element
					firstElementAfterArrayOpened = false;
					lastReadIndex --;
					return ELEMENT;
				} else {
					startRead++;
				}
			}
			firstElementAfterArrayOpened = false;
			
			// read characters till stop (, ] [ ) or end
			boolean previousSpecial = tokenize.charAt(currentInd) == '\\';
			// check quotation element \
			// elements to quote : , [ ] and \ (itself) 
			while (++currentInd < tokenize.length()) {
				char c = tokenize.charAt(currentInd);
				if(c == '\\'){
					if(previousSpecial){
						value += tokenize.substring(startRead, currentInd - 1);
						startRead = currentInd;
						previousSpecial = false;
					} else {
						previousSpecial = true;
					}
				} else {
					if (c == ',' || c == ']' || c == '[') {
						if (!previousSpecial) {
							if (c == '[' && value.length() == 0) {
								lastReadIndex ++;
								firstElementAfterArrayOpened = true;
								return START_ARRAY;
							}
							break;
						} else {
							value += tokenize.substring(startRead, currentInd - 1);
							startRead = currentInd;
						}
					}
					previousSpecial = false;
				}
				lastReadIndex ++;
			}
			value += tokenize.substring(startRead, currentInd);
			return ELEMENT;
		}
		
		
		public String value(){
			return value;
		}
	}
	
	public static void startArray(StringBuilder builder, boolean first){
		if(!first){
			builder.append(",");
		}
		builder.append("[");
	}
	
	public static void value(StringBuilder builder, String value, boolean first){
		if(!first){
			builder.append(",");
		}
		value = value.replace("\\", "\\\\");
		value = value.replace("[", "\\[").replace("]", "\\]").replace(",", "\\,");
		builder.append(value);
	}
	
	public static void endArray(StringBuilder builder){
		builder.append("]");
	}
	
	private static void testCase(String value, String[] comments){
		int next;
		EntityValueTokenizer tokenizer = new EntityValueTokenizer();
		tokenizer.tokenize(value);
		int currentInd = 0;
		while ((next = tokenizer.next()) != END) {
			if (next == ELEMENT) {
				assertEquals(comments[currentInd], tokenizer.value(), value + " " + currentInd);
			} else if (next == START_ARRAY) {
				assertEquals(comments[currentInd], "[", value + " " + currentInd);
			} else if (next == END_ARRAY) {
				assertEquals(comments[currentInd], "]", value+ " " + currentInd);
			}
			currentInd++;
		}
		
		// serialization
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (String st : comments) {
			if (st.equals("[")) {
				startArray(builder, first);
				first = true;
			} else if (st.equals("]")) {
				endArray(builder);
			} else {
				value(builder, st, first);
				first = false;
			}
		}
		String res = builder.toString().replace("\\\\", "\\");
		if(!res.equals(value) && !builder.toString().equals(value)){
			System.err.println("Serialization of value is probably wrong : " + res + " original " + value);
		}
	}
	
	private static void assertEquals(String exp, String actual, String msg) {
		if (!exp.equals(actual)) {
			throw new IllegalArgumentException("Expected '" + exp + "' != '" + actual + "' : " + msg);
		}
	}
	
	private static void testCases(){
		testCase("[1,2,[]]", new String[] {"[", "1", "2", "[", "]", "]"});
		testCase("[[1,2],[1,2]]", new String[] {"[", "[","1", "2", "]","[","1", "2", "]", "]"});
		
		// quotation
		testCase("[1, 2, 4,[1,3\\,4]]", new String[] {"[", "1", " 2", " 4", "[", "1", "3,4","]", "]"});
		testCase("[1,4,\\[1,3\\,4\\]]", new String[] {"[", "1", "4", "[1", "3,4]", "]"});
		testCase("[1,\\4,3\\[,4\\]]", new String[] {"[", "1", "\\4", "3[", "4]", "]"});
		testCase("[1,\\4,3\\[,4\\]]", new String[] {"[", "1", "\\4", "3[", "4]", "]"});
		testCase("[1,\\\\,3]", new String[] {"[", "1", "\\", "3", "]"});
//		testCase("[1,\\\\,\\[]", new String[] {"[", "1", "\\", "[", "]"});
		testCase("[\\\\1,\\\\2,333\\\\]", new String[] {"[", "\\1", "\\2", "333\\", "]"});
		
		testCase("[2,]", new String[] {"[", "2", "", "]"});
		testCase("[,2]", new String[] {"[", "",  "2", "]"});
		testCase("[,2,,]", new String[] {"[", "",  "2", "", "", "]"});
		testCase("[,,]", new String[] {"[", "",  "", "", "]"});
		testCase("[1,,[,,]]", new String[] {"[", "1",  "", "[", "", "", "", "]", "]"});
		
		testCase("14555", new String[] {"14555"});
		testCase("\\[1\\]", new String[] {"[1]"});
		testCase("\\[1\\,2", new String[] {"[1,2"});
		System.out.println("All is successfull");
		
//		tokenizer.tokenize("[1,,[,,]]"); // 1, '', ['','','']
		
	}
	
	public static void main(String[] args) {
		testCases();
	}
	
	

}
