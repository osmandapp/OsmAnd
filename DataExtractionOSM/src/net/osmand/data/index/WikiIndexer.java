package net.osmand.data.index;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.logging.Log;
import org.apache.tools.bzip2.CBZip2InputStream;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import net.osmand.Algoritms;
import net.osmand.LogUtil;
import net.osmand.Version;
import net.osmand.data.preparation.IndexCreator;
import net.osmand.impl.ConsoleProgressImplementation;

// http://toolserver.org/~dispenser/dumps/coord_commonswiki.sql.gz
// select * from coord_commonswiki limit 10;
public class WikiIndexer {
	private static final Log log = LogUtil.getLog(WikiIndexer.class);
	private final File srcPath;
	private final File workPath;
	private final File targetPath;
	private final String userName = "jenkins";
	private final String password = "jenkins";
	private final String url = "jdbc:mysql://localhost/wiki";

	public static class WikiIndexerException extends Exception {
		private static final long serialVersionUID = 1L;

		public WikiIndexerException(String name) {
			super(name);
		}

		public WikiIndexerException(String string, Exception e) {
			super(string, e);
		}

	}

	public WikiIndexer(File srcPath, File targetPath, File workPath) {
		this.srcPath = srcPath;
		this.targetPath = targetPath;
		this.workPath = workPath;
	}

	public static void main(String[] args) {
		try {
			File srcPath = extractDirectory(args, 0);
			File targetPath = extractDirectory(args, 1);
			File workPath = extractDirectory(args, 2);

			WikiIndexer wikiIndexer = new WikiIndexer(srcPath, targetPath, workPath);
			wikiIndexer.run();

		} catch (WikiIndexerException e) {
			log.error(e.getMessage());
		}
	}

	private static File extractDirectory(String[] args, int ind) throws WikiIndexerException {
		if (args.length <= ind) {
			throw new WikiIndexerException(
					"Usage: WikiIndexer src_directory target_directory work_directory [--description={full|normal|minimum}]" + " missing "
							+ (ind + 1));
		} else {
			File fs = new File(args[ind]);
			fs.mkdir();
			if (!fs.exists() || !fs.isDirectory()) {
				throw new WikiIndexerException("Specified directory doesn't exist : " + args[ind]);
			}
			return fs;
		}
	}

	public void run() throws WikiIndexerException {
		log.info("Obtain database connection");
		Connection conn;
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection(url, userName, password);
			log.info("Database connection established");
		} catch (InstantiationException e1) {
			throw new WikiIndexerException("Could not establish connection to " + url + " with " + userName, e1);
		} catch (IllegalAccessException e1) {
			throw new WikiIndexerException("Could not establish connection to " + url + " with " + userName, e1);
		} catch (ClassNotFoundException e1) {
			throw new WikiIndexerException("Could not establish connection to " + url + " with " + userName, e1);
		} catch (SQLException e1) {
			throw new WikiIndexerException("Could not establish connection to " + url + " with " + userName, e1);
		}
		File[] listFiles = srcPath.listFiles();
		for (File f : listFiles) {
			try {
				if (f.isFile() && (f.getName().endsWith(".xml") || f.getName().endsWith(".xml.bz2"))) {
					log.info("About to process " + f.getName());
					File outFile = process(f, conn);
					if (outFile != null) {

						IndexCreator ic = new IndexCreator(workPath);
						ic.setIndexPOI(true);
						ic.setIndexMap(false);
						ic.setIndexTransport(false);
						ic.setIndexAddress(false);
						ic.generateIndexes(outFile, new ConsoleProgressImplementation(3), null, null, null, log);
						// final step
						new File(workPath, ic.getMapFileName()).renameTo(new File(targetPath, ic.getMapFileName()));
					}
				}
			} catch (WikiIndexerException e) {
				log.error("Error processing " + f.getName(), e);
			} catch (RuntimeException e) {
				log.error("Error processing " + f.getName(), e);
			} catch (IOException e) {
				log.error("Error processing " + f.getName(), e);
			} catch (SAXException e) {
				log.error("Error processing " + f.getName(), e);
			} catch (SQLException e) {
				log.error("Error processing " + f.getName(), e);
			} catch (InterruptedException e) {
				log.error("Error processing " + f.getName(), e);
			}
		}
	}

	protected File process(File f, Connection dbConnection) throws WikiIndexerException {
		InputStream fi = null;
		BufferedWriter out = null;
		try {
			int in = f.getName().indexOf('.');
			int in2 = f.getName().indexOf('.', in + 1);
			if (in2 == -1) {
				log.error("File was not processed " + f.getName() + " please use name <Index-name>.<2Letters-Locale>.xml.bz2");
				return null;
			}
			String wikiLocale = f.getName().substring(in + 1, in2);
			log.info("Locale for file " + wikiLocale);
			
			PreparedStatement locSearch = dbConnection.prepareStatement("SELECT gc_lat, gc_lon, gc_type FROM coord_"+wikiLocale+"wiki WHERE gc_from=?");
			PreparedStatement enSearch = null;
			try {
				enSearch = dbConnection.prepareStatement("SELECT gc_lat, gc_lon, gc_type FROM coord_enwiki WHERE gc_name=?");
			} catch (SQLException e) {
				log.warn("EN coord not initialized " , e);
			}
			
			File osmOut = new File(workPath, f.getName().substring(0, in) + ".osm");
			fi = new BufferedInputStream(new FileInputStream(f));
			InputStream progressStream = fi;
			if (f.getName().endsWith(".bz2")) {
				if (fi.read() != 'B' || fi.read() != 'Z') {
					throw new RuntimeException("The source stream must start with the characters BZ if it is to be read as a BZip2 stream."); //$NON-NLS-1$
				} else {
					fi = new CBZip2InputStream(fi);
				}
			}
			ConsoleProgressImplementation progress = new ConsoleProgressImplementation();
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(osmOut), "UTF-8"));
			SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
			WikiOsmHandler wikiOsmHandler = new WikiOsmHandler(saxParser, out, progress, progressStream, locSearch, enSearch, wikiLocale);
			saxParser.parse(fi, wikiOsmHandler);

			locSearch.close();
			enSearch.close();
			if (wikiOsmHandler.getCount() < 1) {
				return null;
			}
			return osmOut;
		} catch (ParserConfigurationException e) {
			throw new WikiIndexerException("Parse exception", e);
		} catch (SAXException e) {
			throw new WikiIndexerException("Parse exception", e);
		} catch (IOException e) {
			throw new WikiIndexerException("Parse exception", e);
		} catch (XMLStreamException e) {
			throw new WikiIndexerException("Parse exception", e);
		} catch (SQLException e) {
			throw new WikiIndexerException("Database exception or locale configuration problem", e);
		} finally {
			Algoritms.closeStream(out);
			Algoritms.closeStream(fi);
		}
	}

	public class WikiOsmHandler extends DefaultHandler {
		private final static boolean RECOGNIZE_ENGLISH = true;
		long id = 1;
		private final SAXParser saxParser;
		private boolean page = false;
		private boolean revision = false;
		private StringBuilder ctext = null;

		private StringBuilder title = new StringBuilder();
		private StringBuilder text = new StringBuilder();
		private StringBuilder pageId = new StringBuilder();
		private float clat = 0;
		private float clon = 0;
		private long cid;
		private String subcategory = null;
		private boolean parseText = false;

		private final ConsoleProgressImplementation progress;
		private final InputStream progIS;
		private XMLStreamWriter streamWriter;
		private final PreparedStatement locSearch;
		private final PreparedStatement enSearch;
		private final String locale;
		

		WikiOsmHandler(SAXParser saxParser, BufferedWriter outOsm, ConsoleProgressImplementation progress, InputStream progIS, 
				PreparedStatement dbStat, PreparedStatement enSearch, String wikiLocale)
				throws IOException, XMLStreamException {
			this.saxParser = saxParser;
			this.progress = progress;
			this.progIS = progIS;
			this.locSearch = dbStat;
			this.enSearch = enSearch;
			this.locale = wikiLocale;
			XMLOutputFactory xof = XMLOutputFactory.newInstance();
			streamWriter = xof.createXMLStreamWriter(outOsm);
			streamWriter.writeStartDocument();
			streamWriter.writeCharacters("\n");
			streamWriter.writeStartElement("osm");
			streamWriter.writeAttribute("version", "0.6");
			streamWriter.writeAttribute("generator", Version.APP_MAP_CREATOR_VERSION);

			progress.startTask("Parse wiki xml", progIS.available());
		}

		public int getCount() {
			return (int) (id - 1);
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			String name = saxParser.isNamespaceAware() ? localName : qName;
			if (!page) {
				page = name.equals("page");
			} else {
				if (name.equals("title")) {
					title.setLength(0);
					ctext = title;
				} else if (name.equals("text")) {
					if(parseText || RECOGNIZE_ENGLISH) {
						text.setLength(0);
						ctext = text;
					}
				} else if (name.equals("revision")) {
					revision  = true;
				} else if (name.equals("id") && !revision) {
					pageId.setLength(0);
					ctext = pageId;
				}
			}
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			if (page) {
				if (ctext != null) {
					ctext.append(ch, start, length);
				}
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			String name = saxParser.isNamespaceAware() ? localName : qName;
			try {
				if (page) {
					if (name.equals("page")) {
						page = false;
						parseText = false;
						progress.remaining(progIS.available());
					} else if (name.equals("title")) {
						ctext = null;
					} else if (name.equals("revision")) {
						revision = false;
					} else if (name.equals("id") && !revision) {
						ctext = null;
						cid = Long.parseLong(pageId.toString());
						locSearch.setLong(1, cid);
						ResultSet rs = locSearch.executeQuery();
						parseText = false;
						if(rs.next()) {
							parseText = true;
							clat = rs.getFloat(1);
							clon = rs.getFloat(2);
							subcategory = rs.getString(3);
							if(subcategory == null){
								subcategory = "";
							}
							subcategory = subcategory.toLowerCase();
						}
					} else if (name.equals("text")) {
						if(!parseText && RECOGNIZE_ENGLISH) {
							tryToMatchEnglish();
						}
						if (parseText) {
							if(id % 500 == 0) {
								log.debug("Article accepted " + cid + " " + title.toString());
							}
							analyzeTextForGeoInfoNew();
						}
						ctext = null;
					}
				}
			} catch (IOException e) {
				throw new SAXException(e);
			} catch (XMLStreamException e) {
				throw new SAXException(e);
			} catch (SQLException e) {
				throw new SAXException(e);
			}
		}
		
		public void tryToMatchEnglish() throws SQLException {
			int i = text.lastIndexOf("[[en:");
			if(i != -1){
				int ei = text.indexOf("]]",i);
				if(ei != -1) {
					String englishName = text.substring("[[en:".length() + i, ei).trim();
					enSearch.setString(1, englishName);
					ResultSet rs = enSearch.getResultSet();
					while (rs.next()) {
						clat = rs.getFloat(1);
						clon = rs.getFloat(2);
						subcategory = rs.getString(3);
						if (subcategory == null) {
							subcategory = "";
						}
						subcategory = subcategory.toLowerCase();
					}
				}
			}
		}

		private String readProperty(String prop, int s, int e) {
			int res = -1;
			for (int i = s; i < e - prop.length(); i++) {
				if (prop.charAt(0) == text.charAt(i)) {
					boolean neq = false;
					for (int j = 0; j < prop.length(); j++) {
						if (prop.charAt(j) != text.charAt(i + j)) {
							neq = true;
							break;
						}
					}
					if (!neq) {
						res = i + prop.length();
						break;
					}
				}
			}
			if (res == -1) {
				return null;
			}
			int sr = -1;
			int se = e;
			for (int i = res; i < e; i++) {
				if (text.charAt(i) == '|') {
					se = i;
					break;
				}
				if (text.charAt(i) == '=') {
					sr = i + 1;
				}
			}
			if (sr != -1) {
				String result = text.substring(sr, se);
				int commSt = result.indexOf("<!--");
				if (commSt != -1) {
					int commEnd = result.indexOf("-->");
					if (commEnd == -1) {
						commEnd = result.length();
					} else {
						commEnd += "-->".length();
					}
					return (result.substring(0, commSt) + result.substring(commEnd)).trim();
				}
				return result.trim();
			}
			return null;
		}
		
		private float zeroParseFloat(String s) {
			return s == null || s.length() == 0 ? 0 : Float.parseFloat(s);
		}

		private int findOpenBrackets(int i) {
			int h = text.indexOf("{{", i);
			boolean check = true;
			while (check) {
				int startComment = text.indexOf("<!--", i);
				check = false;
				if (startComment != -1 && startComment < h) {
					i = text.indexOf("-->", startComment);
					h = text.indexOf("{{", i);
					check = true;
				}
			}
			return h;
		}

		private int findClosedBrackets(int i) {
			if (i == -1) {
				return -1;
			}
			int stack = 1;
			int h = text.indexOf("{{", i + 2);
			int e = text.indexOf("}}", i + 2);
			while (stack != 0 && e != -1) {
				if (h != -1 && h < e) {
					i = h;
					stack++;
				} else {
					i = e;
					stack--;
				}
				if (stack != 0) {
					h = text.indexOf("{{", i + 2);
					e = text.indexOf("}}", i + 2);
				}
			}
			if (stack == 0) {
				return e;
			}
			return -1;
		}

		@Deprecated
		private void analyzeTextForGeoInfo() throws XMLStreamException {
			// fast precheck
			if (title.toString().endsWith("/doc") || title.toString().startsWith("Шаблон:") || title.toString().startsWith("Template:")) {
				// Looks as template article no information in it
				return;
			}
			int ls = text.indexOf("lat_dir");
			if (ls != -1 && text.charAt(ls + 1 + "lat_dir".length()) != '|') {
				float lat = 0;
				float lon = 0;
				String subcategory = "";
				StringBuilder description = new StringBuilder();

				int h = findOpenBrackets(0);
				int e = findClosedBrackets(h);
				// 1. Find main header section {{ ... lat, lon }}
				while (h != -1 && e != -1) {
					String lat_dir = readProperty("lat_dir", h, e);
					// continue if nothing was found
					if (lat_dir != null) {
						break;
					}
					h = findOpenBrackets(e);
					e = findClosedBrackets(h);
				}
				if (h == -1 || e == -1) {
					return;
				}

				// 2. Parse lat lon
				try {
					String lat_dir = readProperty("lat_dir", h, e);
					String lon_dir = readProperty("lon_dir", h, e);
					String lat_dg = readProperty("lat_deg", h, e);
					String lon_dg = readProperty("lon_deg", h, e);
					if (lon_dg == null || lat_dg == null || lat_dg.length() == 0 || lon_dg.length() == 0) {
						return;
					}
					float lat_deg = Float.parseFloat(lat_dg);
					float lon_deg = Float.parseFloat(lon_dg);
					float lat_min = zeroParseFloat(readProperty("lat_min", h, e));
					float lon_min = zeroParseFloat(readProperty("lon_min", h, e));
					float lat_sec = zeroParseFloat(readProperty("lat_sec", h, e));
					float lon_sec = zeroParseFloat(readProperty("lon_sec", h, e));
					lat = (("S".equals(lat_dir)) ? -1 : 1) * (lat_deg + (lat_min + lat_sec / 60) / 60);
					lon = (("W".equals(lon_dir)) ? -1 : 1) * (lon_deg + (lon_min + lon_sec / 60) / 60);
				} catch (RuntimeException es) {
					log.error("Article " + title, es);
					return;
				}
				// 3. Parse main subcategory name
				for (int j = h + 2; j < e; j++) {
					if (Character.isWhitespace(text.charAt(j)) || text.charAt(j) == '|') {
						subcategory = text.substring(h + 2, j).trim();
						break;
					}
				}
				// Special case

				// 4. Parse main subcategory name
				processDescription(description, e + 3);

				if (description.length() > 0) {
					writeNode(lat, lon, subcategory, description);
				}
			}
		}
		
		private void analyzeTextForGeoInfoNew() throws XMLStreamException {
			StringBuilder description = new StringBuilder();
			try {
				
				int beg = 0;
				int h = findOpenBrackets(beg);
				
				// 1. Find main header section {{ ... lat, lon }}
				while (h != -1 && text.substring(beg, h).trim().length() == 0 ) {
					beg = findClosedBrackets(h);
					if(beg == -1){
						return;
					}
					beg += 2;
					h = findOpenBrackets(beg);
					
				}

				// 3. Parse main subcategory name
				//		String subcategory ;
				//		for (int j = h + 2; j < e; j++) {
				//			if (Character.isWhitespace(text.charAt(j)) || text.charAt(j) == '|') {
				//				subcategory = text.substring(h + 2, j).trim();
				//				break;
				//			}
				//		}
				// Special case

				// 4. Parse main subcategory name
				processDescription(description, beg);

			} catch (RuntimeException e) {
				description.setLength(0);
				log.error(e.getMessage(), e);
			}
			if (description.length() > 0) {
				writeNode(clat, clon, subcategory, description);
			}
		}

		private int checkAndParse(int i, String start, String end, StringBuilder d, boolean add) {
			if (text.charAt(i) != start.charAt(0)) {
				return -1;
			}
			for (int j = 1; j < start.length() && i + j < text.length(); j++) {
				if (text.charAt(i + j) != start.charAt(j)) {
					return -1;
				}
			}
			int st = i + start.length();
			int en = text.length();
			boolean colon = false;
			for (int j = i + start.length(); j < text.length(); j++) {
				if (text.charAt(j) == '|') {
					st = j + 1;
					if (colon) {
						// Special case to prevent adding
						// [[File:av.png|thumb|220|220]]
						add = false;
					}
				} else if (j + end.length() <= text.length()) {
					boolean eq = true;
					if (text.charAt(j) == ':') {
						colon = true;
					}
					for (int k = 0; k < end.length(); k++) {
						if (text.charAt(j + k) != end.charAt(k)) {
							eq = false;
							break;
						}
					}
					if (eq) {
						en = j;
						break;
					}

				}
			}
			if (add) {
				d.append(text, st, en);
			}
			return en + end.length();
		}

		private void processDescription(StringBuilder description, int start) {
			for (int j = start; j < text.length();) {
				if (text.charAt(j) == '=' && text.charAt(j + 1) == '=' && j - start > 2048) {
					break;
				} else if (text.charAt(j) == '\n' && j - start > 2048) {
					break;
				} else {
					int r = -1;
					if (r == -1) {
						r = checkAndParse(j, "<ref", "</ref>", description, false);
					}
					if (r == -1) {
						r = checkAndParse(j, "[[", "]]", description, true);
					}
					if (r == -1) {
						r = checkAndParse(j, "{{", "}}", description, true);
					}
					if (r == -1) {
						r = checkAndParse(j, "''", "''", description, true);
					}

					if (r == -1) {
						description.append(text.charAt(j));
						j++;
					} else {
						j = r;
					}
				}
			}
		}

		private void writeNode(double lat, double lon, String subcategory, StringBuilder description) throws XMLStreamException {
			streamWriter.writeCharacters("\n");
			streamWriter.writeStartElement("node");
			id++;
			streamWriter.writeAttribute("id", "-" + cid);
			streamWriter.writeAttribute("lat", lat + "");
			streamWriter.writeAttribute("lon", lon + "");

			streamWriter.writeCharacters("\n  ");
			streamWriter.writeStartElement("tag");
			streamWriter.writeAttribute("k", "name");
			streamWriter.writeAttribute("v", title.toString());
			streamWriter.writeEndElement();
			
			streamWriter.writeCharacters("\n  ");
			streamWriter.writeStartElement("tag");
			streamWriter.writeAttribute("k", "wikipedia");
			streamWriter.writeAttribute("v", locale + ":"+title.toString());
			streamWriter.writeEndElement();
			

			streamWriter.writeCharacters("\n  ");
			streamWriter.writeStartElement("tag");
			streamWriter.writeAttribute("k", "osmwiki");
			streamWriter.writeAttribute("v", subcategory);
			streamWriter.writeEndElement();

			streamWriter.writeCharacters("\n  ");
			streamWriter.writeStartElement("tag");
			streamWriter.writeAttribute("k", "description");
			streamWriter.writeAttribute("v", description.toString().trim());
			streamWriter.writeEndElement();

			streamWriter.writeEndElement();
			streamWriter.writeCharacters("\n");
		}

		@Override
		public void endDocument() throws SAXException {
			try {
				streamWriter.writeEndElement();
				streamWriter.writeCharacters("\n");
				streamWriter.writeEndDocument();
			} catch (XMLStreamException e) {
				throw new SAXException(e);
			}
		}

	}

}
