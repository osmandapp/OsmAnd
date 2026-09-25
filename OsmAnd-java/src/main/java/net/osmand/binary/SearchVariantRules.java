package net.osmand.binary;

import net.osmand.PlatformUtil;
import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Name variants shared by the OBF writer and spatial search. Locale files override matching rules. */
public final class SearchVariantRules {
	private interface Rule {
	}

	private static final Map<String, SearchVariantRules> CACHE = new ConcurrentHashMap<>();
	private final List<Variant> index;
	private final List<Variant> query;
	private final List<Entry> indexEntries;
	private final List<Entry> queryEntries;

	private SearchVariantRules(Map<String, Rule> indexRules, Map<String, Rule> queryRules) {
		List<Variant> index = new ArrayList<>();
		List<Variant> query = new ArrayList<>();
		List<Entry> indexEntries = new ArrayList<>();
		List<Entry> queryEntries = new ArrayList<>();
		for (Rule rule : indexRules.values()) {
			if (rule instanceof Variant variant) {
				index.add(variant);
			} else if (rule instanceof Entry entry) {
				indexEntries.add(entry);
			}
		}
		for (Rule rule : queryRules.values()) {
			if (rule instanceof Variant variant) {
				query.add(variant);
			} else if (rule instanceof Entry entry) {
				queryEntries.add(entry);
			}
		}
		this.index = Collections.unmodifiableList(index);
		this.query = Collections.unmodifiableList(query);
		this.indexEntries = Collections.unmodifiableList(indexEntries);
		this.queryEntries = Collections.unmodifiableList(queryEntries);
	}

	public static SearchVariantRules forLocale(String locale) {
		String normalized = normalizeLocale(locale);
		return CACHE.computeIfAbsent(normalized, SearchVariantRules::load);
	}

	public List<Variant> index() {
		return index;
	}

	public List<Variant> query() {
		return query;
	}

	public List<Entry> indexEntries() {
		return indexEntries;
	}

	public List<Entry> queryEntries() {
		return queryEntries;
	}

	private static SearchVariantRules load(String locale) {
		Map<String, Rule> index = new LinkedHashMap<>();
		Map<String, Rule> query = new LinkedHashMap<>();
		read("rules.xml", index, query, true);
		if (!locale.isEmpty()) {
			String[] parts = locale.split("_");
			StringBuilder suffix = new StringBuilder();
			for (String part : parts) {
				suffix.append('_').append(part);
				read("rules" + suffix + ".xml", index, query, false);
			}
		}
		return new SearchVariantRules(index, query);
	}

	private static String normalizeLocale(String locale) {
		if (locale == null || locale.isEmpty()) {
			return "";
		}
		String[] parts = locale.replace('-', '_').split("_");
		StringBuilder normalized = new StringBuilder();
		for (int i = 0; i < parts.length; i++) {
			if (!parts[i].matches("[A-Za-z0-9]{2,8}")) {
				throw new IllegalArgumentException("Invalid rules locale: " + locale);
			}
			if (i > 0) {
				normalized.append('_');
			}
			if (i == 0) {
				normalized.append(parts[i].toLowerCase(Locale.ROOT));
			} else if (parts[i].length() == 4) {
				normalized.append(parts[i].substring(0, 1).toUpperCase(Locale.ROOT))
						.append(parts[i].substring(1).toLowerCase(Locale.ROOT));
			} else {
				normalized.append(parts[i].toUpperCase(Locale.ROOT));
			}
		}
		return normalized.toString();
	}

	private static void read(String file, Map<String, Rule> index, Map<String, Rule> query, boolean required) {
		try (InputStream input = SearchVariantRules.class.getResourceAsStream(file)) {
			if (input == null) {
				if (required) {
					throw new IllegalStateException("Missing search rules: " + file);
				}
				return;
			}
			XmlPullParser parser = PlatformUtil.newXMLPullParser();
			parser.setInput(input, "UTF-8");
			Map<String, Rule> section = null;
			boolean root = false;
			int event;
			while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if (event == XmlPullParser.START_TAG) {
					String tag = parser.getName();
					if ("rules".equals(tag)) {
						if (root || !"1".equals(parser.getAttributeValue(null, "version"))) {
							throw new IllegalArgumentException("Unsupported search rules in " + file);
						}
						root = true;
					} else if ("index".equals(tag)) {
						section = index;
					} else if ("query".equals(tag)) {
						section = query;
					} else if ("variant".equals(tag) && section != null) {
						String object = requiredAttribute(parser, "object", file);
						String source = requiredAttribute(parser, "source", file);
						String key = "variant:" + object + ':' + source;
						if ("false".equals(parser.getAttributeValue(null, "enabled"))) {
							section.remove(key);
						} else {
							String mode = parser.getAttributeValue(null, "mode");
							section.put(key, new Variant(object, mode == null ? "Single" : mode, source,
									requiredAttribute(parser, "target", file)));
						}
					} else if (("direction".equals(tag) || "search".equals(tag)
							|| "building".equals(tag) || "conjunction".equals(tag)) && section != null) {
						boolean valid = section == index ? "direction".equals(tag) || "search".equals(tag)
								: !"direction".equals(tag);
						if (!valid) {
							throw new IllegalArgumentException("Invalid section for " + tag + " in " + file);
						}
						String word = requiredAttribute(parser, "key", file);
						String key = tag + ':' + word;
						if ("false".equals(parser.getAttributeValue(null, "enabled"))) {
							section.remove(key);
						} else {
							section.put(key, new Entry(tag, word, parser.getAttributeValue(null, "value")));
						}
					} else {
						throw new IllegalArgumentException("Unexpected search rule tag " + tag + " in " + file);
					}
				} else if (event == XmlPullParser.END_TAG &&
						("index".equals(parser.getName()) || "query".equals(parser.getName()))) {
					section = null;
				}
			}
			if (!root) {
				throw new IllegalArgumentException("Missing rules root in " + file);
			}
		} catch (Exception e) {
			throw new IllegalStateException("Cannot load search rules " + file, e);
		}
	}

	private static String requiredAttribute(XmlPullParser parser, String key, String file) {
		String value = parser.getAttributeValue(null, key);
		if (value == null || value.isEmpty()) {
			throw new IllegalArgumentException("Missing " + key + " in " + file);
		}
		return value;
	}

	public static final class Entry implements Rule {
		public final String kind;
		public final String key;
		public final String value;

		private Entry(String kind, String key, String value) {
			if (!kind.equals("conjunction") && (value == null || value.isEmpty())) {
				throw new IllegalArgumentException("Missing value for " + kind + ':' + key);
			}
			this.kind = kind;
			this.key = key;
			this.value = value;
		}
	}

	public static final class Variant implements Rule {
		public final String object;
		private final boolean all;
		private final Pattern source;
		private final String target;

		private Variant(String object, String mode, String source, String target) {
			this.object = object;
			if (!"All".equals(mode) && !"Single".equals(mode)) {
				throw new IllegalArgumentException("Invalid mode for " + object + ':' + source + ": " + mode);
			}
			this.all = "All".equals(mode);
			this.source = Pattern.compile(source);
			this.target = target;
		}

		public boolean appliesTo(String owner) {
			for (String type : object.split(",")) {
				if (type.trim().equals(owner) || type.trim().equals("*")) {
					return true;
				}
			}
			return false;
		}

		/** Returns null when the expression does not apply or does not change the text. */
		public String apply(String text) {
			Matcher matcher = source.matcher(text);
			if (!matcher.find()) {
				return null;
			}
			if (!all && matcher.find()) {
				return null;
			}
			matcher.reset();
			String result = all ? matcher.replaceAll(target) : matcher.replaceFirst(target);
			return result.equals(text) ? null : result;
		}
	}
}
