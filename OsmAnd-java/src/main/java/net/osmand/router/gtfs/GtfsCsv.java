package net.osmand.router.gtfs;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Streaming reader for the CSV flavour found in GTFS feeds: RFC 4180 quoting (doubled quotes,
 * embedded commas and newlines), an optional UTF-8 BOM, CRLF or LF line ends, blank lines, and
 * stray spaces around values (some feeds ship " 40.57" coordinates). Fields are kept as bytes
 * of the current row so that the large stop_times file can be parsed without per-field allocation.
 * The first row is the header; columns are looked up by name with {@link #column(String)}.
 */
public class GtfsCsv implements Closeable {

	public static final int TIME_BLANK = -1;
	public static final int TIME_MALFORMED = -2;
	public static final int NO_DATE = Integer.MIN_VALUE;

	private static final int MAX_ROW_BYTES = 1 << 20; // a runaway quote must not eat the heap

	private final InputStream in;
	private final byte[] buf = new byte[1 << 16];
	private int pos, lim;
	private byte[] row = new byte[1 << 12];
	private int rowLen;
	private int[] fieldStart = new int[32];
	private int[] fieldEnd = new int[32];
	private int fieldCount;
	private final Map<String, Integer> columns = new HashMap<>();

	/** Opens the stream and reads the header row. */
	public GtfsCsv(InputStream in) throws IOException {
		this.in = in;
		if (peek() == 0xEF) { // UTF-8 BOM
			read();
			if (peek() == 0xBB && read() == 0xBB && read() == 0xBF) {
				// skipped
			} else {
				throw new IOException("Not a CSV file: bad byte order mark");
			}
		}
		if (!next()) {
			throw new IOException("Empty CSV file");
		}
		for (int i = 0; i < fieldCount; i++) {
			columns.put(get(i), i);
		}
	}

	@Override
	public void close() throws IOException {
		in.close();
	}

	/** Index of a header column, or -1 if the file does not have it. */
	public int column(String name) {
		Integer i = columns.get(name);
		return i == null ? -1 : i;
	}

	private int fill() throws IOException {
		if (pos == lim) {
			lim = in.read(buf, 0, buf.length);
			pos = 0;
			if (lim <= 0) {
				lim = 0;
				return -1;
			}
		}
		return buf[pos] & 0xff;
	}

	private int peek() throws IOException {
		return fill();
	}

	private int read() throws IOException {
		int c = fill();
		if (c >= 0) {
			pos++;
		}
		return c;
	}

	private void put(int c) throws IOException {
		if (rowLen == row.length) {
			if (rowLen >= MAX_ROW_BYTES) {
				throw new IOException("CSV row longer than " + MAX_ROW_BYTES + " bytes (unterminated quote?)");
			}
			row = Arrays.copyOf(row, rowLen * 2);
		}
		row[rowLen++] = (byte) c;
	}

	private void endField(int start) {
		int s = start, e = rowLen;
		while (s < e && (row[s] == ' ' || row[s] == '\t')) {
			s++;
		}
		while (e > s && (row[e - 1] == ' ' || row[e - 1] == '\t')) {
			e--;
		}
		if (fieldCount == fieldStart.length) {
			fieldStart = Arrays.copyOf(fieldStart, fieldCount * 2);
			fieldEnd = Arrays.copyOf(fieldEnd, fieldCount * 2);
		}
		fieldStart[fieldCount] = s;
		fieldEnd[fieldCount] = e;
		fieldCount++;
	}

	/** Advances to the next non-blank row. Returns false at the end of the file. */
	public boolean next() throws IOException {
		while (true) {
			fieldCount = 0;
			rowLen = 0;
			int start = 0;
			boolean inQuotes = false;
			boolean atFieldStart = true; // only spaces so far in this field
			while (true) {
				int c = read();
				if (c < 0) {
					if (rowLen == 0 && fieldCount == 0) {
						return false;
					}
					endField(start);
					break;
				}
				if (inQuotes) {
					if (c != '"') {
						put(c);
					} else if (peek() == '"') {
						read();
						put('"');
					} else {
						inQuotes = false;
					}
				} else if (c == '"' && atFieldStart) {
					inQuotes = true;
					atFieldStart = false;
				} else if (c == ',') {
					endField(start);
					start = rowLen;
					atFieldStart = true;
				} else if (c == '\n') {
					endField(start);
					break;
				} else if (c != '\r') {
					put(c);
					if (c != ' ' && c != '\t') {
						atFieldStart = false;
					}
				}
			}
			if (fieldCount == 1 && fieldStart[0] == fieldEnd[0]) {
				continue; // blank line
			}
			return true;
		}
	}

	public boolean isEmpty(int col) {
		return col < 0 || col >= fieldCount || fieldStart[col] == fieldEnd[col];
	}

	/** Field text, or "" if the column or the field is missing. */
	public String get(int col) {
		if (col < 0 || col >= fieldCount) {
			return "";
		}
		return new String(row, fieldStart[col], fieldEnd[col] - fieldStart[col], StandardCharsets.UTF_8);
	}

	public int getInt(int col, int def) {
		if (isEmpty(col)) {
			return def;
		}
		int v = 0;
		boolean neg = false;
		for (int i = fieldStart[col]; i < fieldEnd[col]; i++) {
			int ch = row[i];
			if (ch == '-' && i == fieldStart[col]) {
				neg = true;
			} else if (ch >= '0' && ch <= '9') {
				v = v * 10 + ch - '0';
			} else {
				return def;
			}
		}
		return neg ? -v : v;
	}

	public double getDouble(int col, double def) {
		if (isEmpty(col)) {
			return def;
		}
		try {
			return Double.parseDouble(get(col));
		} catch (NumberFormatException e) {
			return def;
		}
	}

	/**
	 * "H:MM:SS" or "HH:MM:SS" as seconds since the start of the service day. Hours may exceed 23
	 * (service past midnight, e.g. 25:10:00). Returns {@link #TIME_BLANK} or {@link #TIME_MALFORMED}.
	 */
	public int getTime(int col) {
		if (isEmpty(col)) {
			return TIME_BLANK;
		}
		int[] parts = new int[3];
		int k = 0, digits = 0;
		for (int i = fieldStart[col]; i < fieldEnd[col]; i++) {
			int ch = row[i];
			if (ch == ':') {
				if (digits == 0 || ++k > 2) {
					return TIME_MALFORMED;
				}
				digits = 0;
			} else if (ch >= '0' && ch <= '9' && digits < 4) {
				parts[k] = parts[k] * 10 + ch - '0';
				digits++;
			} else {
				return TIME_MALFORMED;
			}
		}
		if (k != 2 || digits == 0 || parts[1] > 59 || parts[2] > 59) {
			return TIME_MALFORMED;
		}
		return parts[0] * 3600 + parts[1] * 60 + parts[2];
	}

	/** "YYYYMMDD" as epoch day, or {@link #NO_DATE}. */
	public int getDate(int col) {
		int v = getInt(col, 0);
		try {
			return v <= 0 ? NO_DATE : (int) LocalDate.of(v / 10000, v / 100 % 100, v % 100).toEpochDay();
		} catch (DateTimeException e) {
			return NO_DATE;
		}
	}

	public byte[] getBytes(int col) {
		return col < 0 || col >= fieldCount ? new byte[0] : Arrays.copyOfRange(row, fieldStart[col], fieldEnd[col]);
	}

	public boolean fieldEquals(int col, byte[] other, int len) {
		if (col < 0 || col >= fieldCount || fieldEnd[col] - fieldStart[col] != len) {
			return false;
		}
		for (int i = 0; i < len; i++) {
			if (row[fieldStart[col] + i] != other[i]) {
				return false;
			}
		}
		return true;
	}
}
