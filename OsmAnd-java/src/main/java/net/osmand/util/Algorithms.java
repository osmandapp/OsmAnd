package net.osmand.util;

import static net.osmand.util.CollectionUtils.startsWithAny;

import net.osmand.CallbackWithObject;
import net.osmand.IProgress;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.router.RouteColorize;

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
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


/**
 * Basic algorithms that are not in jdk
 */
public class Algorithms {
	private static final int BUFFER_SIZE = 1024;
	private static final Log log = PlatformUtil.getLog(Algorithms.class);
	
	private static final char[] CHARS_TO_NORMALIZE_KEY = {'’'};
	private static final char[] CHARS_TO_NORMALIZE_VALUE = {'\''};

	private static final String HTML_PATTERN = "<(\"[^\"]*\"|'[^']*'|[^'\">])*>";

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

	/**
	 * Split string by words and convert to lowercase, use as delimiter all chars except letters and digits
	 * @param str input string
	 * @return result words list
	 */

	public static List<String> splitByWordsLowercase(String str) {
		List<String> splitStr = new ArrayList<>();
		int prev = -1;
		for (int i = 0; i <= str.length(); i++) {
			if (i == str.length() ||
					(!Character.isLetter(str.charAt(i)) && !Character.isDigit(str.charAt(i)))) {
				if (prev != -1) {
					String subStr = str.substring(prev, i);
					splitStr.add(subStr.toLowerCase());
					prev = -1;
				}
			} else {
				if (prev == -1) {
					prev = i;
				}
			}
		}
		return splitStr;
	}

	public static boolean isEmpty(Collection<?> c) {
		return c == null || c.size() == 0;
	}

	public static boolean isEmpty(Map<?, ?> map) {
		return map == null || map.size() == 0;
	}

	public static <T> boolean isEmpty(T[] array) {
		return array == null || array.length == 0;
	}

	public static String emptyIfNull(String s) {
		return s == null ? "" : s;
	}

	public static String trimIfNotNull(String s) {
		return s == null ? null : s.trim();
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
		if (!isEmpty(input)) {
			try {
				return Long.parseLong(input);
			} catch (NumberFormatException e) {
				return def;
			}
		}
		return def;
	}

	public static int parseIntSilently(String input, int def) {
		if (!isEmpty(input)) {
			try {
				return Integer.parseInt(input);
			} catch (NumberFormatException e) {
				return def;
			}
		}
		return def;
	}

	public static double parseDoubleSilently(String input, double def) {
		if (!isEmpty(input)) {
			try {
				return Double.parseDouble(input);
			} catch (NumberFormatException e) {
				return def;
			}
		}
		return def;
	}

	public static float parseFloatSilently(String input, float def) {
		if (!isEmpty(input)) {
			try {
				return Float.parseFloat(input);
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

	/**
	 * @see <a href="http://alienryderflex.com/polygon/">Determining Whether A Point Is Inside A Complex Polygon</a>
	 * @param point
	 * @param polygon
	 * @return true if the point is in the area of the polygon
	 */
	public static boolean isPointInsidePolygon(LatLon point,
	                                           List<LatLon> polygon) {
		double px = point.getLongitude();
		double py = point.getLatitude();
		boolean oddNodes = false;
		for (int i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
			double x1 = polygon.get(i).getLongitude();
			double y1 = polygon.get(i).getLatitude();
			double x2 = polygon.get(j).getLongitude();
			double y2 = polygon.get(j).getLatitude();
			if ((y1 < py && y2 >= py
					|| y2 < py && y1 >= py)
					&& (x1 <= px || x2 <= px)) {
				if (x1 + (py - y1) / (y2 - y1) * (x2 - x1) < px) {
					oddNodes = !oddNodes;
				}
			}
		}
		return oddNodes;
	}

	public static String getFileNameWithoutExtensionAndRoadSuffix(String fileName) {
		String name = getFileNameWithoutExtension(fileName);
		return name.endsWith(".road") ? name.substring(0, name.lastIndexOf(".road")) : name;
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
		return getFileNameExtension(f.getName());
	}

	public static String getFileNameExtension(String name) {
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

	public static String convertToPermittedFileName(String name) {
		name = name.replace ("\"", "~");
		name = name.replace ("*", "~");
		name = name.replace ("/", "~");
		name = name.replace (":", "~");
		name = name.replace ("<", "~");
		name = name.replace (">", "~");
		name = name.replace ("?", "~");
		name = name.replace ("\\", "~");
		name = name.replace ("|", "~");
		return name;
	}

	public static List<File> collectDirs(File parentDir, List<File> dirs) {
		return collectDirs(parentDir, dirs, null);
	}

	public static List<File> collectDirs(File parentDir, List<File> dirs, File exclDir) {
		File[] files = parentDir.listFiles();
		if (files != null) {
			Arrays.sort(files);
			for (File file : files) {
				if (file.isDirectory()) {
					if (!file.equals(exclDir)) {
						dirs.add(file);
					}
					collectDirs(file, dirs, exclDir);
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

	private static final char CHAR_TO_SPLIT = 0x01;

	public static Map<String, String> decodeMap(String s) {
		if (isEmpty(s)) {
			return Collections.emptyMap();
		}
		Map<String, String> names = new HashMap<String, String>();
		String[] split = s.split(CHAR_TO_SPLIT + "");
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
				bld.append(e.getKey()).append(CHAR_TO_SPLIT)
						.append(e.getValue().replace(CHAR_TO_SPLIT, (char) (CHAR_TO_SPLIT + 1)));
				bld.append(CHAR_TO_SPLIT);
			}
			return bld.toString();
		}
		return "";
	}

	public static Set<String> decodeStringSet(String s) {
		return decodeStringSet(s, String.valueOf(CHAR_TO_SPLIT));
	}

	public static Set<String> decodeStringSet(String s, String split) {
		if (isEmpty(s)) {
			return Collections.emptySet();
		}
		return new HashSet<>(Arrays.asList(s.split(split)));
	}

	public static <T> String encodeCollection(Collection<T> collection) {
		return encodeCollection(collection, String.valueOf(CHAR_TO_SPLIT));
	}

	public static <T> String encodeCollection(Collection<T> collection, String split) {
		if (collection != null) {
			StringBuilder sb = new StringBuilder();
			for (T item : collection) {
				sb.append(item).append(split);
			}
			return sb.toString();
		}
		return "";
	}

	public static int findFirstNumberEndIndexLegacy(String value) {
		// keep this method unmodified ! (to check old clients crashes on server side)
		int i = 0;
		boolean valid = false;
		if (value.length() > 0 && value.charAt(0) == '-') {
			i++;
		}
		while (i < value.length() &&
				(isDigit(value.charAt(i)) || value.charAt(i) == '.')) {
			i++;
			valid = true;
		}
		if (valid) {
			return i;
		} else {
			return -1;
		}
	}

	public static int findFirstNumberEndIndex(String value) {
		int i = 0;
		if (value.length() > 0 && value.charAt(0) == '-') {
			i++;
		}
		int state = 0; // 0 - no number, 1 - 1st digits, 2 - dot, 3 - last digits
		while (i < value.length() && (isDigit(value.charAt(i)) || (value.charAt(i) == '.'))) {
			if (value.charAt(i) == '.') {
				if (state == 2) {
					return i - 1;
				}
				if (state != 1) {
					return -1;
				}
				state = 2;
			} else {
				if (state == 2) {
					// last digits 
					state = 3;
				} else if (state == 0) {
					// first digits started
					state = 1;
				}

			}
			i++;
		}
		if (state == 2) {
			// invalid number like 40. correct to -> '40'
			return i - 1;
		}
		if (state == 0) {
			return -1;
		}
		return i;
	}

	public static boolean isDigit(char charAt) {
		return charAt >= '0' && charAt <= '9';
	}

	public static boolean isHtmlText(String text) {
		Pattern pattern = Pattern.compile(HTML_PATTERN);
		Matcher matcher = pattern.matcher(text);
		return matcher.find();
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

	public static String formatLatlon(LatLon latLon) {
		if (latLon != null) {
			String lat = String.format(Locale.US, "%.6f", latLon.getLatitude());
			String lan = String.format(Locale.US, "%.6f", latLon.getLongitude());
			return lat + "," + lan;
		}
		return null;
	}

	public static LatLon parseLatLon(String latLon) {
		if (latLon != null) {
			String[] coords = latLon.split(",");
			if (coords.length == 2) {
				try {
					double lat = Double.parseDouble(coords[0]);
					double lon = Double.parseDouble(coords[1]);
					return new LatLon(lat, lon);
				} catch (NumberFormatException e) {
					return null;
				}
			}
		}
		return null;
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
				streamCopy(fin, fout);
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
				if (pg.isInterrupted()) {
					throw new InterruptedIOException();
				}
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
	
	public static byte[] readBytesFromInputStream(InputStream i) throws IOException {
		ByteArrayOutputStream bous = new ByteArrayOutputStream();
		streamCopy(i, bous);
		i.close();
		return bous.toByteArray();
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

	public static boolean removeAllFiles(File file) {
		if (file == null) {
			return false;
		}
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			if (!isEmpty(files)) {
				for (File f : files) {
					removeAllFiles(f);
				}
			}
			return file.delete();
		} else {
			return file.delete();
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

	public static <T> T getPercentile(List<T> sortedValues, int percentile) throws IllegalArgumentException {
		if (percentile < 0 || percentile > 100) {
			throw new IllegalArgumentException("invalid percentile " + percentile + ", should be 0-100");
		}
		int index = (sortedValues.size() - 1) * percentile / 100;
		return sortedValues.get(index);
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
	
	public static int compare(String str1, String str2) {
		return compare(str1, str2, false);
	}
	
	public static int compare(String str1, String str2, boolean nullIsLess) {
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

	public static int[] stringToGradientPalette(String str, String gradientScaleType) {
		boolean isSlope = "gradient_slope_color".equals(gradientScaleType);
		if (isBlank(str)) {
			return isSlope ? RouteColorize.SLOPE_COLORS : RouteColorize.COLORS;
		}
		String[] arr = str.split(" ");
		if (arr.length < 2) {
			return isSlope ? RouteColorize.SLOPE_COLORS : RouteColorize.COLORS;
		}
		int[] colors = new int[arr.length];
		try {
			for (int i = 0; i < arr.length; i++) {
				colors[i] = parseColor(arr[i]);
			}
		} catch (IllegalArgumentException e) {
			return isSlope ? RouteColorize.SLOPE_COLORS : RouteColorize.COLORS;
		}
		return colors;
	}

	public static String gradientPaletteToString(int[] palette, String gradientScaleType) {
		boolean isSlope = "gradient_slope_color".equals(gradientScaleType);
		int[] src;
		if (palette != null && palette.length >= 2) {
			src = palette;
		} else {
			src = isSlope ? RouteColorize.SLOPE_COLORS : RouteColorize.COLORS;
		}
		StringBuilder stringPalette = new StringBuilder();
		for (int i = 0; i < src.length; i++) {
			stringPalette.append(colorToString(src[i]));
			if (i + 1 != src.length) {
				stringPalette.append(" ");
			}
		}
		return stringPalette.toString();
	}

	public static boolean isUrl(String value) {
		String[] urlPrefixes = new String[] {"http://", "https://", "HTTP://", "HTTPS://"};
		return startsWithAny(value, urlPrefixes);
	}

	public static <T> List<WeakReference<T>> updateWeakReferencesList(List<WeakReference<T>> list, T item, boolean isNew) {
		List<WeakReference<T>> copy = new ArrayList<>(list);
		Iterator<WeakReference<T>> it = copy.iterator();
		while (it.hasNext()) {
			WeakReference<T> ref = it.next();
			T object = ref.get();
			if (object == null || object == item) {
				it.remove();
			}
		}
		if (isNew) {
			copy.add(new WeakReference<>(item));
		}
		return copy;
	}

	public static void extendRectToContainPoint(QuadRect mapRect, double longitude, double latitude) {
		mapRect.left = mapRect.left == 0.0 ? longitude : Math.min(mapRect.left, longitude);
		mapRect.right = Math.max(mapRect.right, longitude);
		mapRect.bottom = mapRect.bottom == 0.0 ? latitude : Math.min(mapRect.bottom, latitude);
		mapRect.top = Math.max(mapRect.top, latitude);
	}

	public static void extendRectToContainRect(QuadRect mapRect, QuadRect gpxRect) {
		mapRect.left = mapRect.left == 0.0 ? gpxRect.left : Math.min(mapRect.left, gpxRect.left);
		mapRect.right = Math.max(mapRect.right, gpxRect.right);
		mapRect.top = Math.max(mapRect.top, gpxRect.top);
		mapRect.bottom = mapRect.bottom == 0.0 ? gpxRect.bottom : Math.min(mapRect.bottom, gpxRect.bottom);
	}
	
	public static long combine2Points(int x, int y) {
		return (((long) x) << 32) | ((long) y);
	}

	public static String makeUniqueName(String oldName, CallbackWithObject<String> checkNameCallback) {
		int suffix = 0;
		int i = oldName.length() - 1;
		do {
			try {
				if (oldName.charAt(i) == ' ' || oldName.charAt(i) == '-') {
					throw new NumberFormatException();
				}
				suffix = Integer.parseInt(oldName.substring(i));
			} catch (NumberFormatException e) {
				break;
			}
			i--;
		} while (i >= 0);
		String newName;
		String divider = suffix == 0 ? " " : "";
		do {
			suffix++;
			newName = oldName.substring(0, i + 1) + divider + suffix;
		}
		while (!checkNameCallback.processResult(newName));
		return newName;
	}
	

	public static int lowerTo10BaseRoundingBounds(int num, int[] roundRange) {
		int k = 1;
		while (k < roundRange.length && (roundRange[k] > num || roundRange[k - 1] > num) ) {
			k += 2;
		}
		if (k < roundRange.length) {
			return (num / roundRange[k - 1]) * roundRange[k - 1];
		}
		return num;
	}
	
	public static int[] generate10BaseRoundingBounds(int max, int multCoef) {
		int basenum = 1, mult = 1, num = basenum * mult, ind = 0;
		List<Integer> bounds = new ArrayList<>();
		while (num < max) {
			ind++;
			if (ind % 3 == 1) {
				mult = 2;
			} else if (ind % 3 == 2) {
				mult = 5;
			} else {
				basenum *= 10;
				mult = 1;
			}
			if (ind > 1) {
				int bound = num * multCoef;
				while (bound % (basenum * mult) != 0 && bound > basenum * mult ) {
					bound += num;
				}
				bounds.add(bound);
			}
			num = basenum * mult;
			bounds.add(num);
		}
		int[] ret = new int[bounds.size()];
		for(int j = 0; j < ret.length; j++) {
			ret[j] = bounds.get(bounds.size() - j - 1);
		}
		return ret;
	}
}
