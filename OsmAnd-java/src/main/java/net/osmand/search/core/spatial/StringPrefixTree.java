package net.osmand.search.core.spatial;

import java.util.ArrayList;
import java.util.List;

import net.osmand.binary.NameIndexReader.NameIndexReaderMatcher;
import net.osmand.util.SearchAlgorithms;

/**
 * A Burst Radix Trie. Leaf buckets compress their common prefixes dynamically
 * and only burst into internal routing nodes when the bucket size strictly
 * exceeds COMPACT_COUNT (5).
 */
public class StringPrefixTree<T> {

	private static final int COMPACT_COUNT = 5;
	private final Node<T> root = new Node<>("");

	/**
	 * Holds the string remainder relative to the parent node's prefix
	 */
	private static class Entry<T> {
		String remainder;
		T value;

		Entry(String remainder, T value) {
			this.remainder = remainder;
			this.value = value;
		}
		
		@Override
		public String toString() {
			if (remainder.length() == 0) {
				return String.valueOf(value);
			}
			return remainder + " - " + value;
		}
	}

	/**
	 * A Radix Node that functions either as a compact multi-entry leaf bucket or a
	 * structural internal routing node.
	 */
	private static class Node<T> {
		String prefix;

		// Leaf Node Data
		List<Entry<T>> entries = new ArrayList<>();

		// Internal Node Data
		List<Node<T>> children = null;

		Node(String prefix) {
			this.prefix = prefix;
		}
	}

	/**
	 * Tokenizes and inserts a name into the tree.
	 */
	public void put(String name, T value) {
		List<String> tokens = SearchAlgorithms.splitAndNormalize(name, false);
		for (String token : tokens) {
			insert(root, token, value);
		}
	}

	
	private void insert(Node<T> node, String word, T value) {
		if (node.children == null) {
			node.entries.add(new Entry<>(word, value));
			// Burst ONLY when this specific bucket grows beyond the allowed count
			if (node.entries.size() > COMPACT_COUNT) {
				burst(node);
			}
		} else {
			if (word.isEmpty()) {
				node.entries.add(new Entry<>(word, value));
				return;
			}
			for (Node<T> child : node.children) {
				int commonLen = getCommonPrefixLength(child.prefix, word);
				if (commonLen > 0) {
					// Partial match detected
					if (commonLen < child.prefix.length()) {
						String commonPrefix = child.prefix.substring(0, commonLen);
						String childRemaining = child.prefix.substring(commonLen);
						if (child.children == null) {
							// Push the chopped prefix down into the entry remainders and update the prefix.
							for (Entry<T> entry : child.entries) {
								entry.remainder = childRemaining + entry.remainder;
							}
							child.prefix = commonPrefix;
							// Insert the new word into this newly expanded leaf bucket
							insert(child, word.substring(commonLen), value);
							return;
						} else {
							// If the child is already internal, execute a standard Radix split
							Node<T> splitNode = new Node<>(childRemaining);
							splitNode.children = child.children;
							splitNode.entries = child.entries;

							child.prefix = commonPrefix;
							child.children = new ArrayList<>();
							child.children.add(splitNode);
							insert(child, word.substring(commonLen), value);
							return;
						}
					} else {
						// Full match of child prefix, route deeper down the tree
						insert(child, word.substring(commonLen), value);
						return;
					}
				}
			}

			// No common prefix found with existing edges, add a clean new leaf branch
			Node<T> newChild = new Node<>(word);
			newChild.entries.add(new Entry<>("", value));
			node.children.add(newChild);
		}
	}

	/**
	 * Converts a leaf into an internal node and redistributes its content.
	 */
	private void burst(Node<T> node) {
		List<Entry<T>> oldEntries = node.entries;
		node.entries = new ArrayList<>();
		node.children = new ArrayList<>();
		for (Entry<T> entry : oldEntries) {
			insert(node, entry.remainder, entry.value);
		}
	}
	
	/**
	 * Performs a lookup. Appending a trailing dot '.' switches it to a prefix
	 * search.
	 */
	public List<T> simpleGet(String token) {
		if (token == null || token.isEmpty()) {
			return new ArrayList<>();
		}

		boolean isPrefixSearch = false;
		String searchWord = token.toLowerCase();

		if (searchWord.endsWith(".")) {
			isPrefixSearch = true;
			searchWord = searchWord.substring(0, searchWord.length() - 1);
		}

		List<T> result = new ArrayList<>();
		simpleSearch(root, searchWord, isPrefixSearch, result);
		return result;
	}
	
	public List<T> match(NameIndexReaderMatcher matcher) {
		List<T> l = new ArrayList<>();
		matchSearch(root, "", matcher, l);
		return l;
	}
	
	private void matchSearch(Node<T> node, String prefix, NameIndexReaderMatcher matcher, List<T> result) {
		for (Entry<T> entry : node.entries) {
			if(matcher.matchKey(prefix + entry.remainder)) {
				result.add(entry.value);
			}
		}
		if (node.children != null) {
			for (Node<T> child : node.children) {
				String p = prefix + child.prefix;
				if(matcher.matchKey(p)) {
					matchSearch(child, p, matcher, result);
				}
			}
		}
	}


	private void simpleSearch(Node<T> node, String word, boolean isPrefixSearch, List<T> result) {
		for (Entry<T> entry : node.entries) {
			if (isPrefixSearch) {
				if (entry.remainder.startsWith(word)) {
					result.add(entry.value);
				}
			} else {
				if (entry.remainder.equals(word)) {
					result.add(entry.value);
				}
			}
		}
		if (node.children != null) {
			for (Node<T> child : node.children) {
				int commonLen = getCommonPrefixLength(child.prefix, word);
				if (commonLen > 0) {
					if (word.length() < child.prefix.length()) {
						if (isPrefixSearch && child.prefix.startsWith(word)) {
							collectAll(child, result);
						}
						return;
					}
					if (commonLen == child.prefix.length()) {
						simpleSearch(child, word.substring(commonLen), isPrefixSearch, result);
						return;
					}
				}
			}
		}
	}

	private List<T> collectAll(Node<T> node, List<T> result) {
		for (Entry<T> entry : node.entries) {
			result.add(entry.value);
		}
		if (node.children != null) {
			for (Node<T> child : node.children) {
				collectAll(child, result);
			}
		}
		return result;
	}

	private int getCommonPrefixLength(String s1, String s2) {
		int i = 0;
		int len1 = s1.length();
		int len2 = s2.length();
		while (i < len1 && i < len2) {
			int cp1 = s1.codePointAt(i);
			int cp2 = s2.codePointAt(i);
			if (cp1 != cp2) {
				break;
			}
			i += Character.charCount(cp1);
		}
		return i;
	}

	public void printTree() {
		printTree(-1);
	}
	public void printTree(int limit) {
		int c = printTree(root, "", 0, limit);
		System.out.println("Total tree entries: " + c);
	}

	private int printTree(Node<T> node, String pprefix, int depth,  int limit) {
		String indent = "  ".repeat(depth);
		String fPrefix = pprefix + node.prefix;
		int allc = 0;
		if (node.children == null) {
			System.out.printf("%s↳ [Leaf %d] '%s': ", indent, node.entries.size(), fPrefix);
			boolean empty = true;
			for (Entry<T> e : node.entries) {
				allc++;
				if (e.remainder.length() == 0 && empty) {
					System.out.print(e.value + ", ");
				} else {
					empty = false;
					System.out.print("\n " + indent + "    - ...\"" + e.remainder + "\" -> " + e.value);
				}
			}
			System.out.println();
		} else {
			allc = limit == 0 ? collectAll(node, new ArrayList<>()).size() : 0;
			String all = allc == 0 ? String.format(" %,d", allc) : "";
			System.out.printf("%s↳ [Trie %d%s] '%s': %s\n", indent, node.children.size(), all, pprefix + node.prefix,
					(!node.entries.isEmpty() ? " (local values: " + node.entries + ")" : ""));
			if (limit != 0) {
				for (Node<T> child : node.children) {
					allc += printTree(child, fPrefix, depth + 1, limit - 1);
				}
			}
		}
		return allc;
	}

	/**
	 * Diagnostic testing suite validating proper bucket compression behavior.
	 */
	public static void main(String[] args) {
		StringPrefixTree<Integer> tree = new StringPrefixTree<>();

		System.out.println("=== STEP 1: Inserting 5 elements sharing prefixes ===");
		tree.put("a", 1);
		tree.put("apple", 10);
		tree.put("apricot", 20);
		tree.put("application", 30);
		tree.put("apartment", 40);
		tree.put("apartment", 45);
		tree.put("apartment", 48);
		tree.put("banana", 50);

		System.out.println("\n--- State Before Root Burst (Size <= 5, completely compact root bucket) ---");
		tree.printTree();

		System.out.println("\n=== STEP 2: Adding items to force Root to burst ===");
		tree.put("apocalypse", 60); // 6th item triggers root burst
		tree.put("appendix", 70); // Added into the newly split structure
		tree.put("apoca", 72); // Added into the newly split structure
		tree.printTree();
		tree.put("apoct", 72);
		tree.put("apocr", 72);
		tree.put("apot", 72);
		tree.put("apocm", 72);
		tree.put("apocp", 72);
//        tree.put("apt", 72);
//        tree.put("apko", 72);
		System.out.println("\n--- State After Root Burst ---");
		tree.printTree();

		System.out.println("\n=== STEP 3: Executing Lookup Assertions ===");
		System.out.println("Exact search 'apple': " + tree.simpleGet("apple"));
		System.out.println("Prefix search 'ap.': " + tree.simpleGet("ap."));
		System.out.println("Prefix search 'a.': " + tree.simpleGet("a."));
		System.out.println("Prefix search 'app.': " + tree.simpleGet("app."));
	}
}
