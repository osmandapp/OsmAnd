package net.osmand.plus.views.layers;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

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

	public MapTextLayer(@NonNull Context ctx) {
		super(ctx);
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
		QuadTree<QuadRect> intersections = initBoundIntersections(tileBox);
		for (Map.Entry<OsmandMapLayer, Collection<?>> entry : textObjects.entrySet()) {
			OsmandMapLayer l = entry.getKey();
			MapTextProvider provider = (MapTextProvider) l;
			if (!view.isLayerVisible(l) || !provider.isTextVisible()) {
				continue;
			}

			updateTextSize();
			paintTextIcon.setFakeBoldText(provider.isFakeBoldText());
			float textSize = paintTextIcon.getTextSize();
			for (Object o : entry.getValue()) {
				LatLon loc = provider.getTextLocation(o);
				String name = provider.getText(o);
				if (loc == null || TextUtils.isEmpty(name)) {
					continue;
				}
				int r = provider.getTextShift(o, tileBox);
				float x = tileBox.getPixXFromLatLon(loc.getLatitude(), loc.getLongitude());
				float y = tileBox.getPixYFromLatLon(loc.getLatitude(), loc.getLongitude());

				drawWrappedText(canvas, name, intersections, x, y + r + 2 + textSize / 2, settings.isNightMode());
			}
		}
	}

	private void drawWrappedText(@NonNull Canvas canvas, @NonNull String text,
								 @NonNull QuadTree<QuadRect> intersections, float x, float y,
								 boolean nightMode) {
		float textSize = paintTextIcon.getTextSize();
		if (text.length() > TEXT_WRAP) {
			int start = 0;
			int end = text.length();
			int lastSpace = -1;
			int line = 0;
			int pos = 0;
			int limit = 0;
			while (pos < end && (line < TEXT_LINES)) {
				lastSpace = -1;
				limit += TEXT_WRAP;
				while (pos < limit && pos < end) {
					if (Character.isWhitespace(text.charAt(pos))) {
						lastSpace = pos;
					}
					pos++;
				}
				if (lastSpace == -1 || (pos == end)) {
					String subtext = text.substring(start, pos);
					if (!drawShadowTextLine(canvas, subtext, intersections, x, y, line, nightMode)) {
						break;
					}
					start = pos;
				} else {
					String subtext = text.substring(start, lastSpace);
					if (line + 1 == TEXT_LINES) {
						subtext += "..";
					}
					if (!drawShadowTextLine(canvas, subtext, intersections, x, y, line, nightMode)) {
						break;
					}
					start = lastSpace + 1;
					limit += (start - pos) - 1;
				}
				line++;
			}
		} else if (!intersects(intersections, x, y, paintTextIcon.measureText(text), textSize)) {
			drawShadowText(canvas, text, x, y, nightMode);
		}
	}

	private boolean drawShadowTextLine(@NonNull Canvas canvas, @NonNull String text,
									   @NonNull QuadTree<QuadRect> intersections, float x, float y,
									   int line, boolean nightMode) {
		float textSize = paintTextIcon.getTextSize();
		float centerY = y + line * (textSize + 2);
		if (!intersects(intersections, x, centerY, paintTextIcon.measureText(text), textSize)) {
			drawShadowText(canvas, text, x, centerY, nightMode);
			return true;
		}
		return false;
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
	public void initLayer(@NonNull OsmandMapTileView v) {
		this.view = v;
		paintTextIcon = new Paint();
		updateTextSize();
		paintTextIcon.setTextAlign(Align.CENTER);
		paintTextIcon.setAntiAlias(true);
		Map<OsmandMapLayer, Collection<?>> textObjectsLoc = new TreeMap<>((lhs, rhs) -> {
			if (view != null) {
				float z1 = view.getZorder(lhs);
				float z2 = view.getZorder(rhs);
				return Float.compare(z1, z2);
			}
			return 0;
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
