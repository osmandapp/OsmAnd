package net.osmand.plus.plugins.aprs;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves APRS symbol table + code to bitmaps from bundled hessu/aprs-symbols sprite sheets.
 * Grid: 16 columns x 8 rows, index = symbolCode - 32.
 */
public class AprsSymbolResolver {

	private static final int COLS = 16;
	private static final int ROWS = 8;
	private static final int SYMBOL_PX = 48;

	/**
	 * Top-down symbols that point in the direction of travel (APRS heading symbols).
	 * See aprs.org/symbols/symbolsX.txt "HEADING SYMBOLS".
	 */
	private static final String DIRECTED_MOVING_SYMBOLS = ">^gns<uYvFOR";

	private final OsmandApplication app;
	private Bitmap primarySheet;
	private Bitmap alternateSheet;
	private Bitmap overlaySheet;
	private final ConcurrentHashMap<Long, Bitmap> cache = new ConcurrentHashMap<>();

	public AprsSymbolResolver(@NonNull OsmandApplication app) {
		this.app = app;
	}

	public static boolean isDirectedMovingSymbol(char symbolTable, char symbolCode) {
		if (symbolCode == '<') {
			return true;
		}
		return DIRECTED_MOVING_SYMBOLS.indexOf(symbolCode) >= 0;
	}

	public void ensureLoaded() {
		if (primarySheet == null) {
			primarySheet = loadAsset("aprs-symbols/aprs-symbols-48-0.png");
			alternateSheet = loadAsset("aprs-symbols/aprs-symbols-48-1.png");
			overlaySheet = loadAsset("aprs-symbols/aprs-symbols-48-2.png");
		}
	}

	@Nullable
	private Bitmap loadAsset(@NonNull String name) {
		try (InputStream in = app.getAssets().open(name)) {
			return BitmapFactory.decodeStream(in);
		} catch (IOException e) {
			return null;
		}
	}

	@NonNull
	public Bitmap getSymbolBitmap(char symbolTable, char symbolCode) {
		ensureLoaded();
		long key = (((long) symbolTable) << 32) | symbolCode;
		Bitmap cached = cache.get(key);
		if (cached != null) {
			return cached;
		}
		Bitmap bmp = extractSymbol(symbolTable, symbolCode);
		if (bmp == null) {
			bmp = fallbackBitmap();
		}
		cache.put(key, bmp);
		return bmp;
	}

	@Nullable
	private Bitmap extractSymbol(char symbolTable, char symbolCode) {
		Bitmap sheet;
		int index = symbolCode - 32;
		if (index < 0 || index >= COLS * ROWS) {
			return null;
		}
		if (symbolTable == '/' || symbolTable == '!' || symbolTable == '@') {
			sheet = primarySheet;
		} else if (symbolTable == '\\') {
			sheet = alternateSheet;
		} else if (Character.isDigit(symbolTable) || Character.isUpperCase(symbolTable)) {
			sheet = alternateSheet;
			Bitmap base = slice(sheet, index);
			if (base == null) {
				return null;
			}
			return compositeOverlay(base, symbolTable);
		} else {
			sheet = primarySheet;
		}
		return slice(sheet, index);
	}

	@Nullable
	private Bitmap slice(@Nullable Bitmap sheet, int index) {
		if (sheet == null) {
			return null;
		}
		int col = index % COLS;
		int row = index / COLS;
		int cellW = sheet.getWidth() / COLS;
		int cellH = sheet.getHeight() / ROWS;
		int left = col * cellW;
		int top = row * cellH;
		return Bitmap.createBitmap(sheet, left, top, cellW, cellH);
	}

	@NonNull
	private Bitmap compositeOverlay(@NonNull Bitmap base, char overlayChar) {
		if (overlaySheet == null) {
			return base;
		}
		int overlayIndex = overlayChar - 32;
		if (overlayIndex < 0 || overlayIndex >= COLS * ROWS) {
			return base;
		}
		Bitmap overlay = slice(overlaySheet, overlayIndex);
		if (overlay == null) {
			return base;
		}
		// Overlays (D, E, H, ...) belong in the top half of the symbol cell (aprs.fi convention).
		// Do not extend canvas above the base or squash the full overlay sprite into a narrow band.
		int bandH = Math.max(base.getHeight() / 2, overlay.getHeight() / 2);
		Bitmap out = Bitmap.createBitmap(base.getWidth(), base.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(out);
		canvas.drawBitmap(base, 0, 0, null);
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
		canvas.drawBitmap(overlay,
				new Rect(0, 0, overlay.getWidth(), bandH),
				new Rect(0, 0, base.getWidth(), bandH),
				paint);
		return out;
	}

	@NonNull
	private Bitmap fallbackBitmap() {
		Bitmap fb = Bitmap.createBitmap(SYMBOL_PX, SYMBOL_PX, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(fb);
		Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
		p.setColor(0xFF888888);
		c.drawCircle(SYMBOL_PX / 2f, SYMBOL_PX / 2f, SYMBOL_PX / 3f, p);
		return fb;
	}

	public int getSymbolCoverageCount() {
		return COLS * ROWS * 2;
	}

	public void clearCache() {
		cache.clear();
	}
}
