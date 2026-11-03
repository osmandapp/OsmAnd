package net.osmand.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CollectionUtilsTest {

	@Test
	public void extractEntriesMovesEntriesInKeysOrder() {
		List<String> first = new ArrayList<>(List.of("a"));
		List<String> third = new ArrayList<>(List.of("c"));
		Map<String, List<String>> source = new LinkedHashMap<>();
		source.put("first", first);
		source.put("second", List.of("b"));
		source.put("third", third);

		Map<String, List<String>> extracted = CollectionUtils.extractEntries(source, "third", "absent", "first");

		assertEquals(List.of("third", "first"), new ArrayList<>(extracted.keySet()));
		assertSame(third, extracted.get("third"));
		assertSame(first, extracted.get("first"));
		assertEquals(List.of("second"), new ArrayList<>(source.keySet()));
	}

	@Test
	public void extractEntriesKeepsNullValues() {
		Map<String, String> source = new LinkedHashMap<>();
		source.put("key", null);

		Map<String, String> extracted = CollectionUtils.extractEntries(source, "key");

		assertTrue(extracted.containsKey("key"));
		assertNull(extracted.get("key"));
		assertTrue(source.isEmpty());
	}
}
