package net.osmand.plus.views;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.text.TextUtils;

import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import gnu.trove.set.hash.TIntHashSet;
import net.osmand.plus.R;

public class MapTextLayer extends OsmandMapLayer {

	private static final int TEXT_WRAP = 15;
	private static final int TEXT_LINES = 3;
	private static final int TEXT_SIZE = 13;

	private Map<OsmandMapLayer, Collection<?>> textObjects = new LinkedHashMap<>();
	private Paint paintTextIcon;
	private OsmandMapTileView view;

	public interface MapTextProvider<T> {

		LatLon getTextLocation(T o);

		int getTextShift(T o, RotatedTileBox rb);

		String getText(T o);

		boolean isTextVisible();

		boolean isFakeBoldText();
	}

	public void putData(OsmandMapLayer ml, Collection<?> objects) {
		if (objects == null || objects.isEmpty()) {
			textObjects.remove(ml);
		} else {
			if (ml instanceof MapTextProvider) {
				textObjects.put(ml, objects);
			} else {
				throw new IllegalArgumentException();
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		TIntHashSet set = new TIntHashSet();
		for (OsmandMapLayer l : textObjects.keySet()) {
			MapTextProvider provider = (MapTextProvider) l;
			if (!view.isLayerVisible(l) || !provider.isTextVisible()) {
				continue;
			}

			updateTextSize();
			paintTextIcon.setFakeBoldText(provider.isFakeBoldText());
			for (Object o : textObjects.get(l)) {
				LatLon loc = provider.getTextLocation(o);
				String name = provider.getText(o);
				if (loc == null || TextUtils.isEmpty(name)) {
					continue;
				}

				int x = (int) tileBox.getPixXFromLatLon(loc.getLatitude(), loc.getLongitude());
				int y = (int) tileBox.getPixYFromLatLon(loc.getLatitude(), loc.getLongitude());
				int tx = tileBox.getPixXFromLonNoRot(loc.getLongitude());
				int ty = tileBox.getPixYFromLatNoRot(loc.getLatitude());

				int lines = 0;
				while (lines < TEXT_LINES) {
					if (set.contains(division(tx, ty, 0, lines)) || set.contains(division(tx, ty, -1, lines))
							|| set.contains(division(tx, ty, +1, lines))) {
						break;
					}
					lines++;
				}
				if (lines != 0) {
					int r = provider.getTextShift(o, tileBox);
					drawWrappedText(canvas, name, paintTextIcon.getTextSize(), x,
							y + r + 2 + paintTextIcon.getTextSize() / 2, lines);
					while (lines > 0) {
						set.add(division(tx, ty, 1, lines - 1));
						set.add(division(tx, ty, -1, lines - 1));
						set.add(division(tx, ty, 0, lines - 1));
						lines--;
					}
				}
			}
		}
	}

	private int division(int x, int y, int sx, int sy) {
		// make numbers positive
		return ((((x + 10000) >> 4) + sx) << 16) | (((y + 10000) >> 4) + sy);
	}

	private void drawWrappedText(Canvas cv, String text, float textSize, float x, float y, int lines) {
		boolean nightMode = view.getApplication().getDaynightHelper().isNightMode();
		if (text.length() > TEXT_WRAP) {
			int start = 0;
			int end = text.length();
			int lastSpace = -1;
			int line = 0;
			int pos = 0;
			int limit = 0;
			while (pos < end && (line < lines)) {
				lastSpace = -1;
				limit += TEXT_WRAP;
				while (pos < limit && pos < end) {
					if (Character.isWhitespace(text.charAt(pos))) {
						lastSpace = pos;
					}
					pos++;
				}
				if (lastSpace == -1 || (pos == end)) {
					drawShadowText(cv, text.substring(start, pos), x, y + line * (textSize + 2), nightMode);
					start = pos;
				} else {
					String subtext = text.substring(start, lastSpace);
					if (line + 1 == lines) {
						subtext += "..";
					}
					drawShadowText(cv, subtext, x, y + line * (textSize + 2), nightMode);

					start = lastSpace + 1;
					limit += (start - pos) - 1;
				}

				line++;
			}
		} else {
			drawShadowText(cv, text, x, y, nightMode);
		}
	}

	private void drawShadowText(Canvas cv, String text, float centerX, float centerY, boolean nightMode) {
		Resources r = view.getApplication().getResources();
		paintTextIcon.setStyle(Style.STROKE);
		paintTextIcon.setColor(nightMode
			? r.getColor(R.color.widgettext_shadow_night)
			: r.getColor(R.color.widgettext_shadow_day));
		paintTextIcon.setStrokeWidth(2);
		cv.drawText(text, centerX, centerY, paintTextIcon);
		// reset
		paintTextIcon.setStrokeWidth(2);
		paintTextIcon.setStyle(Style.FILL);
		paintTextIcon.setColor(nightMode
			? r.getColor(R.color.widgettext_night)
			: r.getColor(R.color.widgettext_day));
		cv.drawText(text, centerX, centerY, paintTextIcon);
	}

	@Override
	public void initLayer(OsmandMapTileView v) {
		this.view = v;
		paintTextIcon = new Paint();
		updateTextSize();
		paintTextIcon.setTextAlign(Align.CENTER);
		paintTextIcon.setAntiAlias(true);
		Map<OsmandMapLayer, Collection<?>> textObjectsLoc = new TreeMap<>(new Comparator<OsmandMapLayer>() {
			@Override
			public int compare(OsmandMapLayer lhs, OsmandMapLayer rhs) {
				if (view != null) {
					float z1 = view.getZorder(lhs);
					float z2 = view.getZorder(rhs);
					return Float.compare(z1, z2);
				}
				return 0;
			}
		});
		textObjectsLoc.putAll(this.textObjects);
		this.textObjects = textObjectsLoc;
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
	}

	@Override
	public void destroyLayer() {
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	private void updateTextSize() {
		float scale = view.getApplication().getSettings().TEXT_SCALE.get();
		float textSize = scale * TEXT_SIZE * view.getDensity();
		if (paintTextIcon.getTextSize() != textSize) {
			paintTextIcon.setTextSize(textSize);
		}
	}
}
