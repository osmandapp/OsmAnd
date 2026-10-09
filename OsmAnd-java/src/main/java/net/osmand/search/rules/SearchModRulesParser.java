package net.osmand.search.rules;

import static net.osmand.search.rules.SearchModLocaleRules.ANY_OBJECT;
import static net.osmand.search.rules.SearchModLocaleRules.BUILDING_OBJECT;
import static net.osmand.search.rules.SearchModLocaleRules.VERSION;

import net.osmand.PlatformUtil;
import net.osmand.search.rules.SearchModLocaleRules.Form;
import net.osmand.search.rules.SearchModLocaleRules.Mirror;
import net.osmand.search.rules.SearchModLocaleRules.Rule;
import net.osmand.search.rules.SearchModLocaleRules.SkipPenalty;
import net.osmand.search.rules.SearchModLocaleRules.Unglue;
import net.osmand.search.rules.SearchModLocaleRules.WordRule;
import net.osmand.util.SearchAlgorithms;
import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads one rules file ({@code rules.xml}, {@code rules_<locale>.xml}) into a {@link Layer}, see
 * {@link SearchModLocaleRules}. The files are checked by the unit tests (SearchModRulesValidator), not here.
 */
final class SearchModRulesParser {

	private final String file;
	private final Layer layer;
	// the table that <locales> of rules.xml fills, null for a file without it
	private final SearchModLocales locales;

	SearchModRulesParser(String file, SearchModLocales locales) {
		this.file = file;
		this.layer = new Layer(file);
		this.locales = locales;
	}

	/** The rules of one file; a disabled rule removes the rule of an upper layer with its key. */
	static final class Layer {
		final String file;
		final Map<String, Rule> index = new LinkedHashMap<>();
		final Map<String, Unglue> unglues = new LinkedHashMap<>();
		final Map<String, Integer> classes = new LinkedHashMap<>();
		final Map<String, WordRule> query = new LinkedHashMap<>();
		final Map<String, SkipPenalty> skipPenalty = new LinkedHashMap<>();
		// key -> text of the rule for messages
		final Map<String, String> disabledIndex = new LinkedHashMap<>();
		final Map<String, String> disabledUnglues = new LinkedHashMap<>();
		final Map<String, String> disabledClasses = new LinkedHashMap<>();
		final Map<String, String> disabledQuery = new LinkedHashMap<>();
		final Map<String, String> disabledSkipPenalty = new LinkedHashMap<>();

		Layer(String file) {
			this.file = file;
		}

		void add(Rule rule, boolean enabled) {
			add(index, disabledIndex, rule.key(), rule, rule.toString(), enabled);
		}

		void add(Unglue unglue, boolean enabled) {
			add(unglues, disabledUnglues, String.valueOf(unglue.glue()), unglue, unglue.toString(), enabled);
		}

		void add(WordRule rule, boolean enabled) {
			add(query, disabledQuery, rule.word(), rule, rule.toString(), enabled);
		}

		private <T> void add(Map<String, T> rules, Map<String, String> disabled, String key, T rule, String text,
				boolean enabled) {
			if (enabled) {
				rules.put(key, rule);
			} else {
				disabled.put(key, text);
			}
		}

		void addClass(int wordClass, List<String> words, boolean enabled) {
			for (String word : words) {
				String w = SearchAlgorithms.alignChars(word);
				if (enabled) {
					classes.put(w, wordClass);
				} else {
					disabledClasses.put(w, "the class of '" + w + "'");
				}
			}
		}

		void addSkipPenalty(String object, List<String> words, boolean enabled) {
			for (String w : words) {
				// a word of a name is looked up aligned, as the words of the classes ("straße" is "strasse")
				SkipPenalty entry = new SkipPenalty(SearchAlgorithms.alignChars(w), object);
				add(skipPenalty, disabledSkipPenalty, entry.key(), entry,
						"<skipPenalty object=\"" + object + "\">" + entry.word() + "</skipPenalty>", enabled);
			}
		}
	}

	/** @return the rules of the file; {@code <locales>} of rules.xml go to the {@link SearchModLocales} of the parser */
	Layer parse(InputStream input) throws Exception {
		XmlPullParser parser = PlatformUtil.newXMLPullParser();
		parser.setInput(input, "UTF-8");
		String section = null;
		int event;
		while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
			if (event == XmlPullParser.START_TAG) {
				String tag = parser.getName();
				int depth = parser.getDepth();
				if (depth == 1 && "rules".equals(tag)) {
					if (!VERSION.equals(parser.getAttributeValue(null, "version"))) {
						throw new IllegalArgumentException("Unsupported search rules version in " + file
								+ ", expected " + VERSION);
					}
				} else if (depth == 2) {
					section = tag;
					if ("rule".equals(tag)) {
						parseMirrorRule(parser);
					}
				} else if (depth == 3 && "index".equals(section)) {
					if ("rule".equals(tag)) {
						parseIndexRule(parser);
					} else if ("unglue".equals(tag)) {
						parseUnglue(parser);
					} else if (tag.startsWith("class")) {
						boolean enabled = enabled(parser);
						layer.addClass(tag.charAt(5) - '0', words(parser.nextText()), enabled);
					}
				} else if (depth == 3 && "query".equals(section)) {
					if ("rule".equals(tag)) {
						parseQueryRule(parser);
					} else if ("skipPenalty".equals(tag)) {
						String object = object(parser.getAttributeValue(null, "object"));
						boolean enabled = enabled(parser);
						layer.addSkipPenalty(object, words(parser.nextText()), enabled);
					}
				} else if (depth == 3 && "locales".equals(section) && locales != null) {
					if ("group".equals(tag)) {
						locales.addGroup(parser.getAttributeValue(null, "id"),
								words(parser.getAttributeValue(null, "languages")),
								words(parser.getAttributeValue(null, "locales")));
					} else if ("map".equals(tag)) {
						locales.addMap(parser.getAttributeValue(null, "locale"),
								words(parser.getAttributeValue(null, "prefixes")),
								parser.getAttributeValue(null, "group"), parser.getAttributeValue(null, "translit"));
					}
				}
			} else if (event == XmlPullParser.END_TAG && parser.getDepth() == 2) {
				if ("locales".equals(section) && locales != null) {
					locales.build();
				}
				section = null;
			}
		}
		return layer;
	}

	private void parseIndexRule(XmlPullParser parser) {
		String object = object(parser.getAttributeValue(null, "object"));
		String from = parser.getAttributeValue(null, "from");
		if (!enabled(parser)) {
			layer.add(new Rule(object, from, "", "Single", false, false, file), false);
			return;
		}
		String mode = parser.getAttributeValue(null, "mode");
		layer.add(new Rule(object, from, parser.getAttributeValue(null, "to"), mode == null ? "Single" : mode,
				Rule.KEYS_ALWAYS.equals(parser.getAttributeValue(null, "keys")), true, file), true);
	}

	private void parseUnglue(XmlPullParser parser) {
		char glue = parser.getAttributeValue(null, "glue").charAt(0);
		if (!enabled(parser)) {
			layer.add(new Unglue(glue, null, 0, file), false);
			return;
		}
		String script = parser.getAttributeValue(null, "script");
		String minPart = parser.getAttributeValue(null, "minPart");
		layer.add(new Unglue(glue, script == null ? null : Character.UnicodeScript.forName(script),
				minPart == null ? 2 : Integer.parseInt(minPart), file), true);
	}

	/**
	 * A mirror pair, a {@code <rule>} directly under {@code <rules>} (rules-spec.md, 3.4): one word and its one form,
	 * a query form both ways and alternative names both ways. It shares the key of a query rule in the layers.
	 */
	private void parseMirrorRule(XmlPullParser parser) {
		String word = parser.getAttributeValue(null, "from").toLowerCase(Locale.ROOT);
		if (!enabled(parser)) {
			layer.add(new WordRule(word, List.of(), new Mirror(file, null)), false);
			return;
		}
		String to = parser.getAttributeValue(null, "to").trim();
		Form form = form(parser.getAttributeValue(null, "object"), to);
		layer.add(new WordRule(word, List.of(form), new Mirror(file, to)), true);
	}

	/**
	 * One form is the attributes of the rule ({@code to}, {@code object}; {@code object="building"} without {@code to}),
	 * several forms are {@code <to>} elements with their own {@code object}.
	 */
	private void parseQueryRule(XmlPullParser parser) throws Exception {
		String word = parser.getAttributeValue(null, "from").toLowerCase(Locale.ROOT);
		String to = parser.getAttributeValue(null, "to");
		String object = parser.getAttributeValue(null, "object");
		boolean enabled = enabled(parser);
		List<Form> forms = new ArrayList<>();
		while (parser.nextTag() == XmlPullParser.START_TAG) {
			forms.add(form(parser.getAttributeValue(null, "object"), parser.nextText().trim()));
		}
		if (!enabled) {
			layer.add(new WordRule(word, List.of()), false);
			return;
		}
		if (forms.isEmpty()) {
			forms.add(form(object, to == null ? "" : to.trim()));
		}
		layer.add(new WordRule(word, forms), true);
	}

	// an empty text is a house-number qualifier (object="building") or an ignorable word
	private Form form(String object, String text) {
		String o = object(object);
		if (o.equals(BUILDING_OBJECT) || text.isEmpty()) {
			return new Form(o, "");
		}
		return new Form(o, SearchAlgorithms.splitAndNormalize(text, false).get(0));
	}

	private String object(String object) {
		return object == null || object.isEmpty() ? ANY_OBJECT : object;
	}

	private boolean enabled(XmlPullParser parser) {
		return !"false".equals(parser.getAttributeValue(null, "enabled"));
	}

	private List<String> words(String value) {
		List<String> result = new ArrayList<>();
		if (value != null) {
			for (String s : value.trim().split("\\s+")) {
				if (!s.isEmpty()) {
					result.add(s.toLowerCase(Locale.ROOT));
				}
			}
		}
		return result;
	}
}
