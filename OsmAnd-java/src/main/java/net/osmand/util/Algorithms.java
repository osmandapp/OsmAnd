package net.osmand.util;

import net.osmand.IProgress;
import net.osmand.PlatformUtil;
import net.osmand.router.RouteColorize;
import net.osmand.data.LatLon;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


/**
 * Basic algorithms that are not in jdk
 */
public class Algorithms {
	private static final int BUFFER_SIZE = 1024;
	private static final Log log = PlatformUtil.getLog(Algorithms.class);

	public static boolean isEmpty(Collection<?> c) {
		return c == null || c.size() == 0;
	}
	
	private static char[] CHARS_TO_NORMALIZE_KEY = new char['’'];
	private static char[] CHARS_TO_NORMALIZE_VALUE = new char['\''];

	public static final int ZIP_FILE_SIGNATURE = 0x504b0304;
	public static final int XML_FILE_SIGNATURE = 0x3c3f786d;
	public static final int OBF_FILE_SIGNATURE = 0x08029001;
	public static final int SQLITE_FILE_SIGNATURE = 0x53514C69;
	public static final int BZIP_FILE_SIGNATURE = 0x425a;
	public static final int GZIP_FILE_SIGNATURE = 0x1f8b;

	public static String normalizeSearchText(String s) {
		boolean norm = false;
		for (int i = 0; i < s.length() && !norm; i++) {
			char ch = s.charAt(i);
			for (int j = 0; j < CHARS_TO_NORMALIZE_KEY.length; j++) {
				if (ch == CHARS_TO_NORMALIZE_KEY[j]) {
					norm = true;
					break;
				}
			}
		}
		if (!norm) {
			return s;
		}
		for (int k = 0; k < CHARS_TO_NORMALIZE_KEY.length; k++) {
			s = s.replace(CHARS_TO_NORMALIZE_KEY[k], CHARS_TO_NORMALIZE_VALUE[k]);
		}
		return s;
	}

	public static boolean isEmpty(Map<?, ?> map) {
		return map == null || map.size() == 0;
	}

	public static String emptyIfNull(String s) {
		return s == null ? "" : s;
	}

	public static boolean isEmpty(CharSequence s) {
		return s == null || s.length() == 0;
	}

	public static boolean isBlank(String s) {
		return s == null || s.trim().length() == 0;
	}

	public static int hash(Object... values) {
		return Arrays.hashCode(values);
	}

	public static boolean stringsEqual(String s1, String s2) {
		if (s1 == null && s2 == null) {
			return true;
		} else if (s1 == null) {
			return false;
		} else if (s2 == null) {
			return false;
		}
		return s2.equals(s1);
	}

	public static long parseLongSilently(String input, long def) {
		if (input != null && input.length() > 0) {
			try {
				return Long.parseLong(input);
			} catch (NumberFormatException e) {
				return def;
			}
		}
		return def;
	}

	public static int parseIntSilently(String input, int def) {
		if (input != null && input.length() > 0) {
			try {
				return Integer.parseInt(input);
			} catch (NumberFormatException e) {
				return def;
			}
		}
		return def;
	}

	public static double parseDoubleSilently(String input, double def) {
		if (input != null && input.length() > 0) {
			try {
				return Double.parseDouble(input);
			} catch (NumberFormatException e) {
				return def;
			}
		}
		return def;
	}

	public static boolean isFirstPolygonInsideSecond(List<LatLon> firstPolygon,
	                                                 List<LatLon> secondPolygon) {
		for (LatLon point : firstPolygon) {
			if (!isPointInsidePolygon(point, secondPolygon)) {
				// if at least one point is not inside the boundary, return false
				return false;
			}
		}
		return true;
	}

	public static boolean isPointInsidePolygon(LatLon point,
	                                           List<LatLon> polygon) {
		double pointX = point.getLongitude();
		double pointY = point.getLatitude();
		boolean result = false;
		for (int i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
			double x1 = polygon.get(i).getLongitude();
			double y1 = polygon.get(i).getLatitude();
			double x2 = polygon.get(j).getLongitude();
			double y2 = polygon.get(j).getLatitude();
			if ((y1 > pointY) != (y2 > pointY)
					&& (pointX < (x2 - x1) * (pointY - y1) / (y2-y1) + x1)) {
				result = !result;
			}
		}
		return result;
	}

	public static String getFileNameWithoutExtension(File f) {
		return getFileNameWithoutExtension(f.getName());
	}

	public static String getFileNameWithoutExtension(String name) {
		if (name != null) {
			int index = name.lastIndexOf('.');
			if (index != -1) {
				return name.substring(0, index);
			}
		}
		return name;
	}

	public static String getFileExtension(File f) {
		String name = f.getName();
		int i = name.lastIndexOf(".");
		return name.substring(i + 1);
	}

	public static String getFileWithoutDirs(String name) {
		int i = name.lastIndexOf(File.separator);
		if (i != -1) {
			return name.substring(i + 1);
		}
		return name;
	}

	public static List<File> collectDirs(File parentDir, List<File> dirs) {
		return collectDirs(parentDir, dirs, null);
	}

	public static List<File> collectDirs(File parentDir, List<File> dirs, File exclDir) {
		File[] listFiles = parentDir.listFiles();
		if (listFiles != null) {
			Arrays.sort(listFiles);
			for (File f : listFiles) {
				if (f.isDirectory()) {
					if (!f.equals(exclDir)) {
						dirs.add(f);
					}
					Algorithms.collectDirs(f, dirs);
				}
			}
		}
		return dirs;
	}

	public static File[] getSortedFilesVersions(File dir) {
		File[] listFiles = dir.listFiles();
		if (listFiles != null) {
			Arrays.sort(listFiles, getFileVersionComparator());
		}
		return listFiles;
	}

	public static Comparator<File> getFileVersionComparator() {
		return new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				return -simplifyFileName(o1.getName()).compareTo(simplifyFileName(o2.getName()));
			}

		};
	}

    private static String simplifyFileName(String fn) {
        String lc = fn.toLowerCase();
        if (lc.contains(".")) {
            lc = lc.substring(0, lc.indexOf("."));
        }
        if (lc.endsWith("_2")) {
            lc = lc.substring(0, lc.length() - "_2".length());
        }
        boolean hasTimestampEnd = false;
        for (int i = 0; i < lc.length(); i++) {
            if (lc.charAt(i) >= '0' && lc.charAt(i) <= '9') {
                hasTimestampEnd = true;
                break;
            }
        }
        if (!hasTimestampEnd) {
            lc += "_00_00_00";
        }
        return lc;
    }

    public static Comparator<String> getStringVersionComparator() {
        return new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return -simplifyFileName(o1).compareTo(simplifyFileName(o2));
            }
        };
    }

	private static final char CHAR_TOSPLIT = 0x01;

	public static Map<String, String> decodeMap(String s) {
		if (isEmpty(s)) {
			return Collections.emptyMap();
		}
		Map<String, String> names = new HashMap<String, String>();
		String[] split = s.split(CHAR_TOSPLIT + "");
		// last split is an empty string
		for (int i = 1; i < split.length; i += 2) {
			names.put(split[i - 1], split[i]);
		}
		return names;
	}

	public static String encodeMap(Map<String, String> names) {
		if (names != null) {
			Iterator<Entry<String, String>> it = names.entrySet().iterator();
			StringBuilder bld = new StringBuilder();
			while (it.hasNext()) {
				Entry<String, String> e = it.next();
				bld.append(e.getKey()).append(CHAR_TOSPLIT)
						.append(e.getValue().replace(CHAR_TOSPLIT, (char) (CHAR_TOSPLIT + 1)));
				bld.append(CHAR_TOSPLIT);
			}
			return bld.toString();
		}
		return "";
	}

	public static Set<String> decodeStringSet(String s) {
		if (isEmpty(s)) {
			return Collections.emptySet();
		}
		return new HashSet<>(Arrays.asList(s.split(CHAR_TOSPLIT + "")));
	}

	public static String encodeStringSet(Set<String> set) {
		if (set != null) {
			StringBuilder sb = new StringBuilder();
			for (String s : set) {
				sb.append(s).append(CHAR_TOSPLIT);
			}
			return sb.toString();
		}
		return "";
	}

	public static int findFirstNumberEndIndex(String value) {
		int i = 0;
		boolean valid = false;
		if (value.length() > 0 && value.charAt(0) == '-') {
			i++;
		}
		while (i < value.length() && (isDigit(value.charAt(i)) || value.charAt(i) == '.')) {
			i++;
			valid = true;
		}
		if (valid) {
			return i;
		} else {
			return -1;
		}
	}

	public static boolean isDigit(char charAt) {
		return charAt >= '0' && charAt <= '9';
	}

	/**
	 * Determine whether a file is a ZIP File.
	 */
	public static boolean isZipFile(File file) throws IOException {
		if (file.isDirectory()) {
			return false;
		}
		if (!file.canRead()) {
			throw new IOException("Cannot read file " + file.getAbsolutePath());
		}
		if (file.length() < 4) {
			return false;
		}
		FileInputStream in = new FileInputStream(file);
		int test = readInt(in);
		in.close();
		return test == ZIP_FILE_SIGNATURE;
	}

	public static boolean checkFileSignature(InputStream inputStream, int fileSignature) throws IOException {
		if (inputStream == null) return false;
		int firstBytes;
		if (isSmallFileSignature(fileSignature)) {
			firstBytes = readSmallInt(inputStream);
		} else {
			firstBytes = readInt(inputStream);
		}
		if (inputStream.markSupported()) {
			inputStream.reset();
		}
		return firstBytes == fileSignature;
	}

	public static boolean isSmallFileSignature(int fileSignature) {
		return fileSignature == BZIP_FILE_SIGNATURE || fileSignature == GZIP_FILE_SIGNATURE;
	}

	/**
	 * Checks, whether the child directory is a subdirectory of the parent
	 * directory.
	 *
	 * @param parent the parent directory.
	 * @param child  the suspected child directory.
	 * @return true if the child is a subdirectory of the parent directory.
	 */
	public static boolean isSubDirectory(File parent, File child) {
		try {
			parent = parent.getCanonicalFile();
			child = child.getCanonicalFile();

			File dir = child;
			while (dir != null) {
				if (parent.equals(dir)) {
					return true;
				}
				dir = dir.getParentFile();
			}
		} catch (IOException e) {
			return false;
		}
		return false;
	}

	public static int readInt(InputStream in) throws IOException {
		int ch1 = in.read();
		int ch2 = in.read();
		int ch3 = in.read();
		int ch4 = in.read();
		if ((ch1 | ch2 | ch3 | ch4) < 0)
			throw new EOFException();
		return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + ch4);
	}

	public static int readSmallInt(InputStream in) throws IOException {
		int ch1 = in.read();
		int ch2 = in.read();
		if ((ch1 | ch2) < 0)
			throw new EOFException();
		return ((ch1 << 8) + ch2);
	}

	public static String capitalizeFirstLetterAndLowercase(String s) {
		if (s != null && s.length() > 1) {
			// not very efficient algorithm
			return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
		} else {
			return s;
		}
	}

	public static String capitalizeFirstLetter(String s) {
		if (s != null && s.length() > 0) {
			return Character.toUpperCase(s.charAt(0)) + (s.length() > 1 ? s.substring(1) : "");
		} else {
			return s;
		}
	}

	public static boolean objectEquals(Object a, Object b) {
		if (a == null) {
			return b == null;
		} else {
			return a.equals(b);
		}
	}


	/**
	 * Parse the color string, and return the corresponding color-int.
	 * If the string cannot be parsed, throws an IllegalArgumentException
	 * exception. Supported formats are:
	 * #RRGGBB
	 * #AARRGGBB
	 */
	public static int parseColor(String colorString) throws IllegalArgumentException {
		if (colorString.charAt(0) == '#') {
			// Use a long to avoid rollovers on #ffXXXXXX
			if (colorString.length() == 4) {
				colorString = "#" +
						colorString.charAt(1) + colorString.charAt(1) +
						colorString.charAt(2) + colorString.charAt(2) +
						colorString.charAt(3) + colorString.charAt(3);
			}
			long color = Long.parseLong(colorString.substring(1), 16);
			if (colorString.length() == 7) {
				// Set the alpha value
				color |= 0x00000000ff000000;
			} else if (colorString.length() != 9) {
				throw new IllegalArgumentException("Unknown color " + colorString); //$NON-NLS-1$
			}
			return (int) color;
		}
		throw new IllegalArgumentException("Unknown color " + colorString); //$NON-NLS-1$
	}


	public static int extractFirstIntegerNumber(String s) {
		int i = 0;
		for (int k = 0; k < s.length(); k++) {
			if (isDigit(s.charAt(k))) {
				i = i * 10 + (s.charAt(k) - '0');
			} else {
				break;
			}
		}
		return i;
	}

	public static int extractIntegerNumber(String s) {
		int i = 0;
		int k;
		for (k = 0; k < s.length(); k++) {
			if (isDigit(s.charAt(k))) {
				break;
			}
		}
		for (; k < s.length(); k++) {
			if (isDigit(s.charAt(k))) {
				i = i * 10 + (s.charAt(k) - '0');
			} else {
				break;
			}
		}
		return i;
	}

	public static String extractIntegerPrefix(String s) {
		int k = 0;
		for (; k < s.length(); k++) {
			if (Character.isDigit(s.charAt(k))) {
				return s.substring(0, k);
			}
		}
		return "";
	}

	public static String extractOnlyIntegerSuffix(String s) {
		int k = 0;
		for (; k < s.length(); k++) {
			if (Character.isDigit(s.charAt(k))) {
				return s.substring(k);
			}
		}
		return "";
	}

	public static String extractIntegerSuffix(String s) {
		int k = 0;
		for (; k < s.length(); k++) {
			if (!Character.isDigit(s.charAt(k))) {
				return s.substring(k);
			}
		}
		return "";
	}

	public static void createParentDirsForFile(File file) {
		if (file != null && !file.exists()) {
			File parent = file.getParentFile();
			if (parent != null && !parent.exists()) {
				parent.mkdirs();
			}
		}
	}

	@SuppressWarnings("TryFinallyCanBeTryWithResources")
	public static void fileCopy(File src, File dst) throws IOException {
		FileOutputStream fout = new FileOutputStream(dst);
		try {
			FileInputStream fin = new FileInputStream(src);
			try {
				Algorithms.streamCopy(fin, fout);
			} finally {
				fin.close();
			}
		} finally {
			fout.close();
		}
	}

	public static void streamCopy(InputStream in, OutputStream out) throws IOException {
		byte[] b = new byte[BUFFER_SIZE];
		int read;
		while ((read = in.read(b)) != -1) {
			out.write(b, 0, read);
		}
	}


	public static void streamCopy(InputStream in, OutputStream out, IProgress pg, int bytesDivisor) throws IOException {
		byte[] b = new byte[BUFFER_SIZE];
		int read;
		int cp = 0;
		while ((read = in.read(b)) != -1) {
			out.write(b, 0, read);
			cp += read;
			if (pg != null && cp > bytesDivisor) {
				pg.progress(cp / bytesDivisor);
				cp = cp % bytesDivisor;
			}
		}
	}

	public static void oneByteStreamCopy(InputStream in, OutputStream out) throws IOException {
		int read;
		while ((read = in.read()) != -1) {
			out.write(read);
		}
	}

	public static void closeStream(Closeable stream) {
		try {
			if (stream != null) {
				stream.close();
			}
		} catch (IOException e) {
			log.warn("Closing stream warn", e); //$NON-NLS-1$
		}
	}

	public static ByteArrayInputStream createByteArrayIS(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		streamCopy(in, out);
		in.close();
		return new ByteArrayInputStream(out.toByteArray());
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public static void updateAllExistingImgTilesToOsmandFormat(File f) {
		if (f.isDirectory()) {
			for (File c : f.listFiles()) {
				updateAllExistingImgTilesToOsmandFormat(c);
			}
		} else if (f.getName().endsWith(".png") || f.getName().endsWith(".jpg")) { //$NON-NLS-1$ //$NON-NLS-2$
			f.renameTo(new File(f.getAbsolutePath() + ".tile")); //$NON-NLS-1$
		} else if (f.getName().endsWith(".andnav2")) { //$NON-NLS-1$
			f.renameTo(new File(f.getAbsolutePath().substring(0, f.getAbsolutePath().length() - ".andnav2".length()) + ".tile")); //$NON-NLS-1$ //$NON-NLS-2$
		}

	}

	public static StringBuilder readFromInputStream(InputStream i) throws IOException {
		return readFromInputStream(i, true);
	}
	
	public static StringBuilder readFromInputStream(InputStream i, boolean autoclose) throws IOException {
		StringBuilder responseBody = new StringBuilder();
		responseBody.setLength(0);
		if (i != null) {
			BufferedReader in = new BufferedReader(new InputStreamReader(i, "UTF-8"), 256); //$NON-NLS-1$
			String s;
			boolean f = true;
			while ((s = in.readLine()) != null) {
				if (!f) {
					responseBody.append("\n"); //$NON-NLS-1$
				} else {
					f = false;
				}
				responseBody.append(s);
			}
			if (autoclose) {
				i.close();
			}
		}
		return responseBody;
	}

	public static String gzipToString(byte[] gzip) {
		try {
			GZIPInputStream gzipIs = new GZIPInputStream(new ByteArrayInputStream(gzip));
			return readFromInputStream(gzipIs).toString();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public static byte[] stringToGzip(String str) {
		try {
			ByteArrayOutputStream bous = new ByteArrayOutputStream();
			GZIPOutputStream gzout = new GZIPOutputStream(bous);
			gzout.write(str.getBytes());
			gzout.close();
			return bous.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public static boolean removeAllFiles(File f) {
		if (f == null) {
			return false;
		}
		if (f.isDirectory()) {
			File[] fs = f.listFiles();
			if (fs != null) {
				for (File c : fs) {
					removeAllFiles(c);
				}
			}
			return f.delete();
		} else {
			return f.delete();
		}
	}


	public static long parseLongFromBytes(byte[] bytes, int offset) {
		long o = 0xff & bytes[offset + 7];
		o = o << 8 | (0xff & bytes[offset + 6]);
		o = o << 8 | (0xff & bytes[offset + 5]);
		o = o << 8 | (0xff & bytes[offset + 4]);
		o = o << 8 | (0xff & bytes[offset + 3]);
		o = o << 8 | (0xff & bytes[offset + 2]);
		o = o << 8 | (0xff & bytes[offset + 1]);
		o = o << 8 | (0xff & bytes[offset]);
		return o;
	}


	public static void putLongToBytes(byte[] bytes, int offset, long l) {
		bytes[offset] = (byte) (l & 0xff);
		l >>= 8;
		bytes[offset + 1] = (byte) (l & 0xff);
		l >>= 8;
		bytes[offset + 2] = (byte) (l & 0xff);
		l >>= 8;
		bytes[offset + 3] = (byte) (l & 0xff);
		l >>= 8;
		bytes[offset + 4] = (byte) (l & 0xff);
		l >>= 8;
		bytes[offset + 5] = (byte) (l & 0xff);
		l >>= 8;
		bytes[offset + 6] = (byte) (l & 0xff);
		l >>= 8;
		bytes[offset + 7] = (byte) (l & 0xff);
	}


	public static int parseIntFromBytes(byte[] bytes, int offset) {
		int o = (0xff & bytes[offset + 3]) << 24;
		o |= (0xff & bytes[offset + 2]) << 16;
		o |= (0xff & bytes[offset + 1]) << 8;
		o |= (0xff & bytes[offset]);
		return o;
	}

	public static void putIntToBytes(byte[] bytes, int offset, int l) {
		bytes[offset] = (byte) (l & 0xff);
		l >>= 8;
		bytes[offset + 1] = (byte) (l & 0xff);
		l >>= 8;
		bytes[offset + 2] = (byte) (l & 0xff);
		l >>= 8;
		bytes[offset + 3] = (byte) (l & 0xff);
	}


	public static void writeLongInt(OutputStream stream, long l) throws IOException {
		stream.write((int) (l & 0xff));
		l >>= 8;
		stream.write((int) (l & 0xff));
		l >>= 8;
		stream.write((int) (l & 0xff));
		l >>= 8;
		stream.write((int) (l & 0xff));
		l >>= 8;
		stream.write((int) (l & 0xff));
		l >>= 8;
		stream.write((int) (l & 0xff));
		l >>= 8;
		stream.write((int) (l & 0xff));
		l >>= 8;
		stream.write((int) (l & 0xff));
	}

	public static void writeInt(OutputStream stream, int l) throws IOException {
		stream.write(l & 0xff);
		l >>= 8;
		stream.write(l & 0xff);
		l >>= 8;
		stream.write(l & 0xff);
		l >>= 8;
		stream.write(l & 0xff);
	}


	public static void writeSmallInt(OutputStream stream, int l) throws IOException {
		stream.write(l & 0xff);
		l >>= 8;
		stream.write(l & 0xff);
	}

	public static int parseSmallIntFromBytes(byte[] bytes, int offset) {
		int s = (0xff & bytes[offset + 1]) << 8;
		s |= (0xff & bytes[offset]);
		return s;
	}

	public static void putSmallIntBytes(byte[] bytes, int offset, int s) {
		bytes[offset] = (byte) (s & 0xff);
		s >>= 8;
		bytes[offset + 1] = (byte) (s & 0xff);
	}

	public static boolean containsDigit(String name) {
		for (int i = 0; i < name.length(); i++) {
			if (Character.isDigit(name.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	public static boolean isInt(double d) {
		return (d == Math.floor(d)) && !Double.isInfinite(d);
	}

	public static boolean isInt(String value) {
		int length = value.length();
		for (int i = 0; i < length; i++) {
			char ch = value.charAt(i);
			if (!Character.isDigit(ch)) {
				if (length == 1 || i > 0 || ch != '-') {
					return false;
				}
			}
		}
		return true;
	}

	public static boolean isFloat(String value) {
		int pointsCount = 0;
		int length = value.length();
		for (int i = 0; i < length; i++) {
			char ch = value.charAt(i);
			if (!Character.isDigit(ch)) {
				if (length < 2) {
					return false;
				}
				if (!(ch == '-' || ch == '.')) {
					return false;
				} else if (ch == '-' && i != 0) {
					return false;
				} else if ((ch == '.' && pointsCount >= 1) || (ch == '.' && i == length - 1)) {
					return false;
				} else if (ch == '.') {
					pointsCount++;
				}
			}
		}
		return pointsCount == 1;
	}

	public static String formatDuration(int seconds, boolean fullForm) {
		String sec;
		if (seconds % 60 < 10) {
			sec = "0" + (seconds % 60);
		} else {
			sec = (seconds % 60) + "";
		}
		int minutes = seconds / 60;
		if ((!fullForm) && (minutes < 60)) {
			return minutes + ":" + sec;
		} else {
			String min;
			if (minutes % 60 < 10) {
				min = "0" + (minutes % 60);
			} else {
				min = (minutes % 60) + "";
			}
			int hours = minutes / 60;
			return hours + ":" + min + ":" + sec;
		}
	}

	public static String formatMinutesDuration(int minutes) {
		int min = minutes % 60;
		int hours = minutes / 60;
		return String.format(Locale.UK, "%02d:%02d", hours, min);
	}

	public static <T extends Enum<T>> T parseEnumValue(T[] cl, String val, T defaultValue) {
		for (T aCl : cl) {
			if (aCl.name().equalsIgnoreCase(val)) {
				return aCl;
			}
		}
		return defaultValue;
	}

	public static String colorToString(int color) {
		if ((0xFF000000 & color) == 0xFF000000) {
			return "#" + format(6, Integer.toHexString(color & 0x00FFFFFF)); //$NON-NLS-1$
		} else {
			return "#" + format(8, Integer.toHexString(color)); //$NON-NLS-1$
		}
	}

	private static String format(int i, String hexString) {
		while (hexString.length() < i) {
			hexString = "0" + hexString;
		}
		return hexString;
	}

	public static int getRainbowColor(double percent) {

		// Given an input percentage (0.0-1.0) this will produce a colour from a "wide rainbow"
		// from purple (low) to red(high).  This is useful for producing value-based colourations (e.g., altitude)

		double a = (1. - percent) * 5.;
		int X = (int) Math.floor(a);
		int Y = (int) (Math.floor(255 * (a - X)));
		switch (X) {
			case 0: return 0xFFFF0000 + (Y << 8);
			case 1: return 0xFF00FF00 + ((255 - Y) << 16);
			case 2: return 0xFF00FF00 + Y;
			case 3: return 0xFF0000FF + ((255 - Y) << 8);
			case 4: return 0xFF0000FF + (Y << 16);
		}
		return 0xFFFF00FF;
	}

	public static int compare(int x, int y) {
		return (x < y) ? -1 : ((x == y) ? 0 : 1);
	}
	
	public static int compare(long x, long y) {
		return (x < y) ? -1 : ((x == y) ? 0 : 1);
	}
	
	public static int compare(final String str1, final String str2) {
		return compare(str1, str2, false);
	}
	
	public static int compare(final String str1, final String str2, final boolean nullIsLess) {
        if (str1 == str2) {
            return 0;
        }
        if (str1 == null) {
            return nullIsLess ? -1 : 1;
        }
        if (str2 == null) {
            return nullIsLess ? 1 : - 1;
        }
        return str1.compareTo(str2);
    }

	public static String getFileAsString(File file) {
		try {
			FileInputStream fin = new FileInputStream(file);
			BufferedReader reader = new BufferedReader(new InputStreamReader(fin, "UTF-8"));
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				if (sb.length() > 0) {
					sb.append("\n");
				}
				sb.append(line);
			}
			reader.close();
			fin.close();
			return sb.toString();
		} catch (Exception e) {
			return null;
		}
	}

	public static Map<String, String> parseStringsXml(File file) throws IOException, XmlPullParserException {
		InputStream is = new FileInputStream(file);
		XmlPullParser parser = PlatformUtil.newXMLPullParser();
		Map<String, String> map = new HashMap<>();
		parser.setInput(is, "UTF-8");
		int tok;
		String key = null;
		StringBuilder text = null;
		while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
			if (tok == XmlPullParser.START_TAG) {
				text = new StringBuilder();
				String name = parser.getName();
				if ("string".equals(name)) {
					key = parser.getAttributeValue("", "name");
				}
			} else if (tok == XmlPullParser.TEXT) {
				if (text != null) {
					text.append(parser.getText());
				}
			} else if (tok == XmlPullParser.END_TAG) {
				if (key != null) {
					map.put(key, text.toString());
				}
				key = null;
				text = null;
			}
		}
		is.close();
		return map;
	}

	public static <T> void reverseArray(T[] array) {
		for (int i = 0; i < array.length / 2; i++) {
			T temp = array[i];
			array[i] = array[array.length - i - 1];
			array[array.length - i - 1] = temp;
		}
	}

	public static boolean containsInArrayL(long[] array, long value) {
		return Arrays.binarySearch(array, value) >= 0;
	}

	public static long[] addToArrayL(long[] array, long value, boolean skipIfExists) {
		long[] result;
		if (array == null) {
			result = new long[]{ value };
		} else if (skipIfExists && Arrays.binarySearch(array, value) >= 0) {
			result = array;
		} else {
			result = new long[array.length + 1];
			System.arraycopy(array, 0, result, 0, array.length);
			result[result.length - 1] = value;
			Arrays.sort(result);
		}
		return result;
	}

	public static long[] removeFromArrayL(long[] array, long value) {
		long[] result;
		if (array != null) {
			int index = Arrays.binarySearch(array, value);
			if (index >= 0) {
				result = new long[array.length - 1];
				System.arraycopy(array, 0, result, 0, index);
				if (index < result.length) {
					System.arraycopy(array, index + 1, result, index, array.length - (index + 1));
				}
				return result;
			} else {
				return array;
			}
		} else {
			return array;
		}
	}

	public static String arrayToString(int[] a) {
		if (a == null || a.length == 0) {
			return null;
		}
		StringBuilder b = new StringBuilder();
		for (int value : a) {
			if (b.length() > 0) {
				b.append(",");
			}
			b.append(value);
		}
		return b.toString();
	}

	public static int[] stringToArray(String array) throws NumberFormatException {
		if (array == null || array.length() == 0) {
			return null;
		}
		String[] items = array.split(",");
		int[] res = new int[items.length];
		for (int i = 0; i < items.length; i++) {
			res[i] = Integer.parseInt(items[i]);
		}
		return res;
	}

	public static boolean isValidMessageFormat(CharSequence sequence) {
		if (!isEmpty(sequence)) {
			int counter = 0;
			for (int i = 0; i < sequence.length(); i++) {
				char ch = sequence.charAt(i);
				if (ch == '{') {
					counter++;
				} else if (ch == '}') {
					counter--;
				}
			}
			return counter == 0;
		}
		return false;
	}

	public static int[] stringToGradientPalette(String str) {
		if (Algorithms.isBlank(str)) {
			return RouteColorize.colors;
		}
		String[] arr = str.split(" ");
		if (arr.length != 3) {
			return RouteColorize.colors;
		}
		int[] colors = new int[3];
		try {
			for (int i = 0; i < 3; i++) {
				colors[i] = Algorithms.parseColor(arr[i]);
			}
		} catch (IllegalArgumentException e) {
			return RouteColorize.colors;
		}
		return colors;
	}

	public static String gradientPaletteToString(int[] colors) {
		int[] src = (colors != null && colors.length == 3) ? colors : RouteColorize.colors;
		return Algorithms.colorToString(src[0]) + " " +
				Algorithms.colorToString(src[1]) + " " +
				Algorithms.colorToString(src[2]);
	}
}