package net.osmand.router.gtfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

public class GtfsCsvTest {

	private GtfsCsv csv(String text) throws IOException {
		return new GtfsCsv(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void quotesCommasAndEscapedQuotes() throws IOException {
		GtfsCsv c = csv("id,name,desc\n1,\"Main St, North\",\"He said \"\"hi\"\"\"\n2,plain,\n");
		assertTrue(c.next());
		assertEquals("Main St, North", c.get(c.column("name")));
		assertEquals("He said \"hi\"", c.get(c.column("desc")));
		assertTrue(c.next());
		assertEquals("plain", c.get(c.column("name")));
		assertTrue(c.isEmpty(c.column("desc")));
		assertFalse(c.next());
	}

	@Test
	public void quotedFieldMayContainNewline() throws IOException {
		GtfsCsv c = csv("a,b\r\n\"line1\r\nline2\",x\r\n");
		assertTrue(c.next());
		assertEquals("line1\r\nline2", c.get(0));
		assertEquals("x", c.get(1));
		assertFalse(c.next());
	}

	@Test
	public void bomCrlfAndBlankLines() throws IOException {
		GtfsCsv c = csv("﻿stop_id,stop_name\r\n\r\nS1,One\r\n\r\n  \r\nS2,Two");
		assertEquals(0, c.column("stop_id")); // the BOM is not part of the first column name
		assertTrue(c.next());
		assertEquals("S1", c.get(0));
		assertTrue(c.next());
		assertEquals("Two", c.get(1)); // last row without a line end
		assertFalse(c.next());
	}

	@Test
	public void spacesAroundValuesAreTrimmed() throws IOException {
		GtfsCsv c = csv(" stop_id , stop_lat,stop_lon\n S1 ,  40.578350, -73.940029\n");
		assertEquals(0, c.column("stop_id"));
		assertTrue(c.next());
		assertEquals("S1", c.get(0));
		assertEquals(40.57835, c.getDouble(c.column("stop_lat"), 0), 1e-9);
		assertEquals(-73.940029, c.getDouble(c.column("stop_lon"), 0), 1e-9);
	}

	@Test
	public void quoteInsideUnquotedFieldIsLiteral() throws IOException {
		GtfsCsv c = csv("a,b\n5\" pipe,x\n");
		assertTrue(c.next());
		assertEquals("5\" pipe", c.get(0));
		assertEquals("x", c.get(1));
	}

	@Test
	public void shortRowsAndMissingColumns() throws IOException {
		GtfsCsv c = csv("a,b,c\n1\n");
		assertTrue(c.next());
		assertEquals("1", c.get(0));
		assertTrue(c.isEmpty(1));
		assertTrue(c.isEmpty(c.column("nope")));
		assertEquals(7, c.getInt(2, 7));
		assertEquals(-1, c.column("nope"));
	}

	@Test
	public void timesPastMidnightBlankAndMalformed() throws IOException {
		GtfsCsv c = csv("t\n08:05:09\n8:05:09\n25:10:00\n\" 28:47:40 \"\n,\n24:61:00\nabc\n12:00\n");
		int[] expected = {8 * 3600 + 5 * 60 + 9, 8 * 3600 + 5 * 60 + 9, 25 * 3600 + 600, 28 * 3600 + 47 * 60 + 40};
		for (int e : expected) {
			assertTrue(c.next());
			assertEquals(e, c.getTime(0));
		}
		assertTrue(c.next());
		assertEquals(GtfsCsv.TIME_BLANK, c.getTime(0));
		assertTrue(c.next());
		assertEquals(GtfsCsv.TIME_MALFORMED, c.getTime(0));
		assertTrue(c.next());
		assertEquals(GtfsCsv.TIME_MALFORMED, c.getTime(0));
		assertTrue(c.next());
		assertEquals(GtfsCsv.TIME_MALFORMED, c.getTime(0));
	}

	@Test
	public void datesAndInts() throws IOException {
		GtfsCsv c = csv("d,n\n20261101,-5\n20260230,x\n");
		assertTrue(c.next());
		assertEquals(java.time.LocalDate.of(2026, 11, 1).toEpochDay(), c.getDate(0));
		assertEquals(-5, c.getInt(1, 0));
		assertTrue(c.next());
		assertEquals(GtfsCsv.NO_DATE, c.getDate(0)); // no 30 February
		assertEquals(9, c.getInt(1, 9));
	}

	@Test(expected = IOException.class)
	public void emptyFileIsAnError() throws IOException {
		csv("");
	}

	@Test(expected = IOException.class)
	public void runawayQuoteIsAnErrorNotAnOutOfMemory() throws IOException {
		StringBuilder sb = new StringBuilder("a\n\"");
		for (int i = 0; i < (2 << 20); i++) {
			sb.append('x');
		}
		GtfsCsv c = csv(sb.toString());
		c.next();
	}
}
