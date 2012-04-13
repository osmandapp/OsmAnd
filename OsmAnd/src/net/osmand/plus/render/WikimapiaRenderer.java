package net.osmand.plus.render;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.Algoritms;
import net.osmand.LogUtil;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.data.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.MultyPolygon;
import net.osmand.plus.render.NativeOsmandLibrary.NativeSearchResult;
import net.osmand.plus.render.TextRenderer.TextDrawInfo;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
//import net.osmand.plus.render.OsmandRenderer;

import org.apache.commons.logging.Log;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.view.WindowManager;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import java.util.zip.GZIPInputStream;
import java.io.FileInputStream;


public class WikimapiaRenderer {
	private static final Log log = LogUtil.getLog(WikimapiaRenderer.class);
	public static final int TILE_SIZE = 256;
	/*
	private DisplayMetrics dm;

	public WikimapiaRenderer(Context context) {
		this.context = context;

		dm = new DisplayMetrics();
		WindowManager wmgr = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		wmgr.getDefaultDisplay().getMetrics(dm);
	}
	*/

	public static Bitmap render(final String tilePath, int x, int y, int zoom) {
		System.out.println("WikimapiaRenderer : " + tilePath);
		try {
			return doRender(tilePath, x, y, zoom);
		} catch(Exception e) {
			System.out.println("WikimapiaRenderer : Got exception: " + e);
			log.info("Got exception: " + e);
			return null;
		}
	}

	private static Bitmap doRender(final String tilePath, final int tileX, final int tileY, final int zoom) throws Exception {

		final Bitmap bmp = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.ARGB_4444);

		// fill area
		final Canvas cv = new Canvas(bmp);
		//cv.drawColor(0xf1eee8);
		// TODO: transparent or opaque, depending on settings
		cv.drawARGB(0x0, 0xf1, 0xee, 0xe8);

		// TODO: get the color from the settings
		final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setARGB(0xff, 0, 127, 255);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(2.0f);
		paint.setStrokeMiter(1.0f);
		paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setStrokeCap(Paint.Cap.ROUND);

		final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG /* | Paint.LINEAR_TEXT_FLAG */ ); // Anyone has any idea what LINEAR_TEXT_FLAG does?
		textPaint.setStyle(Paint.Style.FILL);
		textPaint.setTextSize(13.0f);
		textPaint.setTextAlign(Paint.Align.CENTER);

		DefaultHandler xmlHandler = new DefaultHandler() {

			// Only these variables are used for actual drawing
			boolean startPointRead = false;
			Path poly = new Path();
			long id = 0;
			boolean readLabel = false;
			String label = "";
			// URL is not used yet
			boolean readUrl = false;
			String url = "";
			int readBound = 0;
			double boundY1 = 0.0;
			double boundY2 = 0.0;
			double boundX1 = 0.0;
			double boundX2 = 0.0;

			float px[] = new float[2];

			public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
				readBound = -1;
				if (name.equals("point")) {
					convertLatLonToPixels(	Double.parseDouble(attributes.getValue("x")),
											Double.parseDouble(attributes.getValue("y")),
											tileX, tileY, zoom, px);
					if(!startPointRead) {
						startPointRead = true;
						poly.moveTo(px[0], px[1]);
					} else {
						poly.lineTo(px[0], px[1]);
					}
				} else
				if (name.equals("place")) {
					id = Long.parseLong(attributes.getValue("id"));
					startPointRead = false;
					label = "";
					url = "";
					readLabel = false;
					readUrl = false;
					readBound = 0;
					boundX1 = 0.0;
					boundX2 = 0.0;
					boundY1 = 0.0;
					boundY2 = 0.0;
				} else
				if (name.equals("name")) {
					readLabel = true;
				} else
				if (name.equals("url")) {
					readUrl = true;
				} else
				if (name.equals("north")) {
					readBound = 1;
				} else
				if (name.equals("south")) {
					readBound = 2;
				} else
				if (name.equals("east")) {
					readBound = 3;
				} else
				if (name.equals("west")) {
					readBound = 4;
				}
			}

			public void endElement(String uri, String localName, String name) throws SAXException {
				if (name.equals("place")) {
					poly.close();
					cv.drawPath(poly, paint);
					poly.reset();
					convertLatLonToPixels(	(boundX1 + boundX2) / 2.0,
											(boundY1 + boundY2) / 2.0,
											tileX, tileY, zoom, px);
					// Replace &amp; twice, because this XML string is XML-escaped twice.
					label = label.replaceAll("[&]amp[;]", "&").replaceAll("[&]amp[;]", "&").replaceAll("[&]quot[;]", "\"").
							replaceAll("[&]apos[;]", "\'").replaceAll("[&]lt[;]", "<").replaceAll("[&]gt[;]", ">");
					// Draw text twice, to make a shadow, so it will be visible on both dark and white backgrounds
					textPaint.setARGB(192, 0, 0, 0);
					cv.drawText(label, px[0]+1.0f, px[1]+1.0f, textPaint);
					textPaint.setARGB(255, 255, 255, 0);
					cv.drawText(label, px[0], px[1], textPaint);
					//log.info((boundX1 + boundX2) / 2.0 + ":" + (boundY1 + boundY2) / 2.0 + " -> " + px[0] + ":" + px[1] + " -> " + id + " " + label + "\n");
					label = "";
					url = "";
				}
				readLabel = false;
				readUrl = false;
				readBound = 0;
			}
			
			public void characters(char ch[], int start, int length) throws SAXException {
				if (readLabel) {
					label += new String(ch, start, length);
				} else
				if (readUrl) {
					url += new String(ch, start, length);
				} else
				if (readBound == 1) {
					boundY1 = Double.parseDouble(new String(ch, start, length));
				} else
				if (readBound == 2) {
					boundY2 = Double.parseDouble(new String(ch, start, length));
				} else
				if (readBound == 3) {
					boundX1 = Double.parseDouble(new String(ch, start, length));
				} else
				if (readBound == 4) {
					boundX2 = Double.parseDouble(new String(ch, start, length));
				}
			}
			
			/*
			public void draw(double x1, double y1, double x2, double y2) {
				float p1[] = new float[2];
				float p2[] = new float[2];
				convertLatLonToPixels(x1, y1, tileX, tileY, zoom, p1);
				convertLatLonToPixels(x2, y2, tileX, tileY, zoom, p2);
				//log.info(x1 + ":" + y1 + ":" + x2 + ":" + y2 + " -> " + p1[0] + ":" + p1[1] + ":" + p2[0] + ":" + p2[1] + " -> " + id + " " + label + "\n");
				cv.drawLine(p1[0], p1[1], p2[0], p2[1], paint);
			}
			*/
		};
		
		SAXParserFactory.newInstance().newSAXParser().parse(new GZIPInputStream(new FileInputStream(tilePath)), xmlHandler);

		return bmp;
	}

	// Conversion taken from http://www.maptiler.org/google-maps-coordinates-tile-bounds-projection/
	// 156543.03392804062 for tileSize 256 pixels
	private static final double initialResolution = 2.0 * Math.PI * 6378137.0 / (double)TILE_SIZE;
	private static final double originShift = 2.0 * Math.PI * 6378137.0 / 2.0;

	public static void convertLatLonToPixels(double lon, double lat, int tx, int ty, int zoom, float out[]) {
		double res = initialResolution / Math.pow(2, zoom);
		// Convert lon/lat to meters first
		double px = lon * originShift / 180.0;
		double py = Math.log( Math.tan((90.0 + lat) * Math.PI / 360.0 )) / (Math.PI / 180.0);
		py = py * originShift / 180.0;
		// Substract the coords of the tile upper-left corner
		px -= (double)tx * (double)TILE_SIZE * res - originShift;
		py += (double)ty * (double)TILE_SIZE * res - originShift;
		py = -py;
		// Then convert to pixels
		px /= res;
		py /= res;
		out[0] = (float)px;
		out[1] = (float)py;
	}
}
