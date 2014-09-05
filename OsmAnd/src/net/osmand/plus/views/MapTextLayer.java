package net.osmand.plus.views;

import gnu.trove.set.hash.TIntHashSet;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;

public class MapTextLayer extends OsmandMapLayer {
	
	private Map<OsmandMapLayer, 
			List<?>> textObjects = new LinkedHashMap<OsmandMapLayer, List<?>>();
	public static final int TEXT_WRAP = 15;
	public static final int TEXT_LINES = 3;
	private Paint paintTextIcon;
	private OsmandMapTileView view;
	private boolean alwaysVisible;
	
	
	
	public interface MapTextProvider<T> {
		
		LatLon getTextLocation(T o);
		
		int getTextShift(T o, RotatedTileBox rb);
		
		String getText(T o);
	}
	
	public void putData(OsmandMapLayer ml, List<?> objects) {
		if(objects == null || objects.isEmpty()) {
			textObjects.remove(ml); 
		} else {
			if(ml instanceof MapTextProvider) {
				textObjects.put(ml, objects);
			} else {
				throw new IllegalArgumentException();
			}
		}
	}
	
	public boolean isAlwaysVisible() {
		return alwaysVisible;
	}
	
	public void setAlwaysVisible(boolean alwaysVisible) {
		this.alwaysVisible = alwaysVisible;
	}
	
	public boolean isVisible() {
		return view.getSettings().SHOW_POI_LABEL.get() || isAlwaysVisible();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (!isVisible()) {
			return;
		}
		TIntHashSet set = new TIntHashSet();
		for (OsmandMapLayer l : textObjects.keySet()) {
			if (view.isLayerVisible(l)) {
				for (Object o : textObjects.get(l)) {
					LatLon location = ((MapTextProvider) l).getTextLocation(o);
					int x = (int) tileBox.getPixXFromLatLon(location.getLatitude(), location
							.getLongitude());
					int y = (int) tileBox.getPixYFromLatLon(location.getLatitude(), location
							.getLongitude());
					int tx = tileBox.getPixXFromLonNoRot(location.getLongitude());
					int ty = tileBox.getPixYFromLatNoRot(location.getLatitude());
					String name = ((MapTextProvider) l).getText(o);
					if (name != null && name.length() > 0) {
						int lines = 0;
						while (lines < TEXT_LINES) {
							if (set.contains(division(tx, ty, 0, lines)) || set.contains(division(tx, ty, -1, lines))
									|| set.contains(division(tx, ty, +1, lines))) {
								break;
							}
							lines++;
						}
						if (lines == 0) {
							// drawWrappedText(canvas, "...", paintTextIcon.getTextSize(), x, y + r + 2 +
							// paintTextIcon.getTextSize() / 2, 1);
						} else {
							int r = ((MapTextProvider) l).getTextShift(o, tileBox);
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
		}
	}

	private int division(int x, int y, int sx, int sy) {
		// make numbers positive
		return ((((x + 10000) >> 4) + sx) << 16) | (((y + 10000) >> 4) + sy);
	}
	
	private void drawWrappedText(Canvas cv, String text, float textSize, float x, float y, int lines) {
		if(text.length() > TEXT_WRAP){
			int start = 0;
			int end = text.length();
			int lastSpace = -1;
			int line = 0;
			int pos = 0;
			int limit = 0;
			while(pos < end && (line < lines)){
				lastSpace = -1;
				limit += TEXT_WRAP;
				while(pos < limit && pos < end){
					if(!Character.isLetterOrDigit(text.charAt(pos))){
						lastSpace = pos;
					}
					pos++;
				}
				if(lastSpace == -1 || (pos == end)){
					drawShadowText(cv, text.substring(start, pos), x, y + line * (textSize + 2));
					start = pos;
				} else {
					String subtext = text.substring(start, lastSpace);
					if (line + 1 == lines) {
						subtext += "..";
					}
					drawShadowText(cv, subtext, x, y + line * (textSize + 2));
					
					start = lastSpace + 1;
					limit += (start - pos) - 1;
				}
				
				line++;
				
				
			}
		} else {
			drawShadowText(cv, text, x, y);
		}
	}
	
	private void drawShadowText(Canvas cv, String text, float centerX, float centerY) {
		int c = paintTextIcon.getColor();
		paintTextIcon.setStyle(Style.STROKE);
		paintTextIcon.setColor(Color.WHITE);
		paintTextIcon.setStrokeWidth(2);
		cv.drawText(text, centerX, centerY, paintTextIcon);
		// reset
		paintTextIcon.setStrokeWidth(2);
		paintTextIcon.setStyle(Style.FILL);
		paintTextIcon.setColor(c);
		cv.drawText(text, centerX, centerY, paintTextIcon);
	}

	@Override
	public void initLayer(OsmandMapTileView v) {
		this.view = v;
		paintTextIcon = new Paint();
		paintTextIcon.setTextSize(13 * v.getDensity());
		paintTextIcon.setTextAlign(Align.CENTER);
		paintTextIcon.setAntiAlias(true);
		Map<OsmandMapLayer, List<?>> textObjectsLoc = new TreeMap<OsmandMapLayer, List<?>>(new Comparator<OsmandMapLayer>() {

			@Override
			public int compare(OsmandMapLayer lhs, OsmandMapLayer rhs) {
				 if(view != null) {
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

}
