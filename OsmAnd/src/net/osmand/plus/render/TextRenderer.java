package net.osmand.plus.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.plus.render.OsmandRenderer.RenderingContext;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.sf.junidecode.Junidecode;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.FloatMath;

public class TextRenderer {

	private TextPaint paintText;
	private final Context context;
	private Paint paintIcon;

	static class TextDrawInfo {

		public TextDrawInfo(String text) {
			this.text = text;
		}

		String text = null;
		Path drawOnPath = null;
		RectF bounds = null;
		float vOffset = 0;
		float centerX = 0;
		float pathRotate = 0;
		float centerY = 0;
		float textSize = 0;
		float minDistance = 0;
		int textColor = Color.BLACK;
		int textShadow = 0;
		int textWrap = 0;
		boolean bold = false;
		String shieldRes = null;
		int textOrder = 100;

		public void fillProperties(RenderingRuleSearchRequest render, float centerX, float centerY) {
			this.centerX = centerX;
			this.centerY = centerY + render.getIntPropertyValue(render.ALL.R_TEXT_DY, 0);
			// used only for draw on path where centerY doesn't play role
			this.vOffset = render.getIntPropertyValue(render.ALL.R_TEXT_DY, 0);
			textColor = render.getIntPropertyValue(render.ALL.R_TEXT_COLOR);
			if (textColor == 0) {
				textColor = Color.BLACK;
			}
			textSize = render.getIntPropertyValue(render.ALL.R_TEXT_SIZE);
			textShadow = render.getIntPropertyValue(render.ALL.R_TEXT_HALO_RADIUS, 0);
			textWrap = render.getIntPropertyValue(render.ALL.R_TEXT_WRAP_WIDTH, 0);
			bold = render.getIntPropertyValue(render.ALL.R_TEXT_BOLD, 0) > 0;
			minDistance = render.getIntPropertyValue(render.ALL.R_TEXT_MIN_DISTANCE, 0);
			if (render.isSpecified(render.ALL.R_TEXT_SHIELD)) {
				shieldRes = render.getStringPropertyValue(render.ALL.R_TEXT_SHIELD);
			}
			textOrder = render.getIntPropertyValue(render.ALL.R_TEXT_ORDER, 100);
		}
	}

	public TextRenderer(Context context) {
		this.context = context;
		paintText = new TextPaint();
		paintText.setStyle(Style.FILL);
		paintText.setStrokeWidth(1);
		paintText.setColor(Color.BLACK);
		paintText.setTextAlign(Align.CENTER);
		paintText.setTypeface(Typeface.create("Droid Serif", Typeface.NORMAL)); //$NON-NLS-1$
		paintText.setAntiAlias(true);

		paintIcon = new Paint();
		paintIcon.setStyle(Style.STROKE);
	}

	public TextPaint getPaintText() {
		return paintText;
	}

	private float sqr(float a) {
		return a * a;
	}

	boolean intersects(RectF tRect, float tRot, RectF sRect, float sRot) {
		if (Math.abs(tRot) < Math.PI / 15 && Math.abs(sRot) < Math.PI / 15) {
			return RectF.intersects(tRect, sRect);
		}
		float dist = FloatMath.sqrt(sqr(tRect.centerX() - sRect.centerX()) + sqr(tRect.centerY() - sRect.centerY()));
		if (dist < 3) {
			return true;
		}

		// difference close to 90/270 degrees
		if (Math.abs(Math.cos(tRot - sRot)) < 0.3) {
			// rotate one rectangle to 90 degrees
			tRot += Math.PI / 2;
			float l = tRect.centerX() - tRect.height() / 2;
			float t = tRect.centerY() - tRect.width() / 2;
			tRect = new RectF(l, t, l + tRect.height(), t + tRect.width());
		}

		// determine difference close to 180/0 degrees
		if (Math.abs(FloatMath.sin(tRot - sRot)) < 0.3) {
			// rotate t box
			// (calculate offset for t center suppose we rotate around s center)
			float diff = (float) (-Math.atan2(tRect.centerX() - sRect.centerX(), tRect.centerY() - sRect.centerY()) + Math.PI / 2);
			diff -= sRot;
			float left = sRect.centerX() + dist * FloatMath.cos(diff) - tRect.width() / 2;
			float top = sRect.centerY() - dist * FloatMath.sin(diff) - tRect.height() / 2;
			RectF nRect = new RectF(left, top, left + tRect.width(), top + tRect.height());
			return RectF.intersects(nRect, sRect);
		}

		// TODO other cases not covered
		return RectF.intersects(tRect, sRect);
	}

	void drawTestBox(Canvas cv, RectF r, float rot, String text) {
		cv.save();
		cv.translate(r.centerX(), r.centerY());
		cv.rotate((float) (rot * 180 / Math.PI));
		RectF rs = new RectF(-r.width() / 2, -r.height() / 2, r.width() / 2, r.height() / 2);
		cv.drawRect(rs, paintIcon);
		if (text != null) {
			paintText.setTextSize(paintText.getTextSize() - 4);
			cv.drawText(text, rs.centerX(), rs.centerY(), paintText);
			paintText.setTextSize(paintText.getTextSize() + 4);
		}
		cv.restore();
	}

	List<TextDrawInfo> tempSearch = new ArrayList<TextDrawInfo>();

	private boolean findTextIntersection(Canvas cv, RenderingContext rc, QuadTree<TextDrawInfo> boundIntersections, TextDrawInfo text) {
		// for test purposes
//		drawTestBox(cv, text.bounds, text.pathRotate, text.text);
		boundIntersections.queryInBox(text.bounds, tempSearch);
		for (int i = 0; i < tempSearch.size(); i++) {
			TextDrawInfo t = tempSearch.get(i);
			if (intersects(text.bounds, text.pathRotate, t.bounds, t.pathRotate)) {
				return true;
			}
		}
		if (text.minDistance > 0) {
			RectF boundsSearch = new RectF(text.bounds);
			boundsSearch.inset(-rc.getDensityValue(Math.max(5.0f, text.minDistance)), -rc.getDensityValue(15));
			boundIntersections.queryInBox(boundsSearch, tempSearch);
			// drawTestBox(cv, &boundsSearch, text.pathRotate, paintIcon, text.text, NULL/*paintText*/);
			for (int i = 0; i < tempSearch.size(); i++) {
				TextDrawInfo t = tempSearch.get(i);
				if (t.minDistance > 0 && t.text.equals(text.text) &&
						intersects(boundsSearch, text.pathRotate, t.bounds, t.pathRotate)) {
					return true;
				}
			}
		}
		boundIntersections.insert(text, text.bounds);
		return false;
	}

	private void drawTextOnCanvas(Canvas cv, String text, float centerX, float centerY, Paint paint, float textShadow) {
		if (textShadow > 0) {
			int c = paintText.getColor();
			paintText.setStyle(Style.STROKE);
			paintText.setColor(Color.WHITE);
			paintText.setStrokeWidth(2 + textShadow);
			cv.drawText(text, centerX, centerY, paint);
			// reset
			paintText.setStrokeWidth(2);
			paintText.setStyle(Style.FILL);
			paintText.setColor(c);
		}
		cv.drawText(text, centerX, centerY, paint);
	}

	public void drawTextOverCanvas(RenderingContext rc, Canvas cv, boolean useEnglishNames) {
		int size = rc.textToDraw.size();

		// 1. Sort text using text order
		Collections.sort(rc.textToDraw, new Comparator<TextDrawInfo>() {
			@Override
			public int compare(TextDrawInfo object1, TextDrawInfo object2) {
				return object1.textOrder - object2.textOrder;
			}
		});
		RectF r = new RectF(0, 0, rc.width, rc.height);
		r.inset(-100, -100);
		QuadTree<TextDrawInfo> nonIntersectedBounds = new QuadTree<TextDrawInfo>(r, 4, 0.6f);

		for (int i = 0; i < size; i++) {
			TextDrawInfo text = rc.textToDraw.get(i);
			if (text.text != null && text.text.length() > 0) {
				int d = text.text.indexOf(MapRenderingTypes.DELIM_CHAR);
				// not used now functionality
				// possibly it will be used specifying english names after that character
				if (d > 0) {
					text.text = text.text.substring(0, d);
				}
				if (useEnglishNames) {
					text.text = Junidecode.unidecode(text.text);
				}

				// sest text size before finding intersection (it is used there)
				float textSize = rc.getDensityValue(text.textSize);
				paintText.setTextSize(textSize);
				paintText.setFakeBoldText(text.bold);
				paintText.setColor(text.textColor);
				// align center y
				text.centerY += (-paintText.ascent());

				// calculate if there is intersection
				boolean intersects = findTextIntersection(cv, rc, nonIntersectedBounds, text);
				if (!intersects) {
					if (text.drawOnPath != null) {
						if (text.textShadow > 0) {
							paintText.setColor(Color.WHITE);
							paintText.setStyle(Style.STROKE);
							paintText.setStrokeWidth(2 + text.textShadow);
							cv.drawTextOnPath(text.text, text.drawOnPath, 0, text.vOffset, paintText);
							// reset
							paintText.setStyle(Style.FILL);
							paintText.setStrokeWidth(2);
							paintText.setColor(text.textColor);
						}
						cv.drawTextOnPath(text.text, text.drawOnPath, 0, text.vOffset, paintText);
					} else {
						if (text.shieldRes != null) {
							Bitmap ico = RenderingIcons.getIcon(context, text.shieldRes);
							if (ico != null) {
								cv.drawBitmap(ico, text.centerX - ico.getWidth() / 2 - 0.5f,
										text.centerY - ico.getHeight() / 2 - rc.getDensityValue(4.5f), paintIcon);
							}
						}

						drawWrappedText(cv, text, textSize);
					}
				}
			}
		}
	}

	private void drawWrappedText(Canvas cv, TextDrawInfo text, float textSize) {
		if (text.textWrap == 0) {
			// set maximum for all text
			text.textWrap = 40;
		}

		if (text.text.length() > text.textWrap) {
			int start = 0;
			int end = text.text.length();
			int lastSpace = -1;
			int line = 0;
			int pos = 0;
			int limit = 0;
			while (pos < end) {
				lastSpace = -1;
				limit += text.textWrap;
				while (pos < limit && pos < end) {
					if (!Character.isLetterOrDigit(text.text.charAt(pos))) {
						lastSpace = pos;
					}
					pos++;
				}
				if (lastSpace == -1) {
					drawTextOnCanvas(cv, text.text.substring(start, pos), text.centerX, text.centerY + line * (textSize + 2), paintText,
							text.textShadow);
					start = pos;
				} else {
					drawTextOnCanvas(cv, text.text.substring(start, lastSpace), text.centerX, text.centerY + line * (textSize + 2),
							paintText, text.textShadow);
					start = lastSpace + 1;
					limit += (start - pos) - 1;
				}
				line++;

			}
		} else {
			drawTextOnCanvas(cv, text.text, text.centerX, text.centerY, paintText, text.textShadow);
		}
	}
	
	private void createTextDrawInfo(RenderingRuleSearchRequest render, RenderingContext rc, TagValuePair pair, float xMid, float yMid,
			Path path, PointF[] points, String name, boolean ref) {
		render.setInitialTagValueZoom(pair.tag, pair.value, rc.zoom);
		render.setIntFilter(render.ALL.R_TEXT_LENGTH, name.length());
		render.setBooleanFilter(render.ALL.R_REF, ref);
		if(render.search(RenderingRulesStorage.TEXT_RULES)){
			if(render.getIntPropertyValue(render.ALL.R_TEXT_SIZE) > 0){
				TextDrawInfo text = new TextDrawInfo(name);
				text.fillProperties(render, xMid, yMid);
				paintText.setTextSize(text.textSize);
				Rect bs = new Rect();
				paintText.getTextBounds(name, 0, name.length(), bs);
				text.bounds = new RectF(bs);
				text.bounds.inset(-rc.getDensityValue(3), -rc.getDensityValue(10));
				boolean display = true;
				if(path != null) {
					text.drawOnPath = path;
					display = calculatePathToRotate(rc, text, points, 
							render.getIntPropertyValue(render.ALL.R_TEXT_ON_PATH, 0) != 0);
				}
				if(text.drawOnPath == null) {
					text.bounds.offset(text.centerX, text.centerY);
					// shift to match alignment
					text.bounds.offset(-text.bounds.width()/2, 0);
				} else {
					text.bounds.offset(text.centerX - text.bounds.width()/2, text.centerY - text.bounds.height()/2);
				}
				if(display) {
					rc.textToDraw.add(text);
				}
			}
		}
	}
	
	public void renderText(String name, RenderingRuleSearchRequest render, RenderingContext rc, TagValuePair pair,
			float xMid, float yMid, Path path, PointF[] points) {
		String ref = null;
		if(name.charAt(0) == MapRenderingTypes.REF_CHAR){
			ref = name.substring(1);
			name = ""; //$NON-NLS-1$
			for(int k = 0; k < ref.length(); k++){
				if(ref.charAt(k) == MapRenderingTypes.REF_CHAR){
					if(k < ref.length() - 1){
						name = ref.substring(k + 1);
					}
					ref = ref.substring(0, k);
					break;
				}
			}
		}
		if(ref != null && ref.trim().length() > 0){
			createTextDrawInfo(render, rc, pair, xMid, yMid, path, points, ref, true);
		}

		if(name != null && name.trim().length() > 0){
			createTextDrawInfo(render, rc, pair, xMid, yMid, path, points, name, false);
		}
	}

	
	boolean calculatePathToRotate(RenderingContext rc, TextDrawInfo p, PointF[] points, boolean drawOnPath) {
		int len = points.length;
		if (!drawOnPath) {
			p.drawOnPath = null;
			// simply calculate rotation of path used for shields
			float px = 0;
			float py = 0;
			for (int i = 1; i < len; i++) {
				px += points[i].x - points[i - 1].x;
				py += points[i].y - points[i - 1].y;
			}
			if (px != 0 || py != 0) {
				p.pathRotate = (float) (-Math.atan2(px, py) + Math.PI / 2);
			}
			return true;
		}

		boolean inverse = false;
		float roadLength = 0;
		boolean prevInside = false;
		float visibleRoadLength = 0;
		float textw = p.bounds.width();
		int last = 0;
		int startVisible = 0;
		float[] distances = new float[points.length - 1];

		float normalTextLen = 1.5f * textw;
		for (int i = 0; i < len; i++, last++) {
			boolean inside = points[i].x >= 0 && points[i].x <= rc.width &&
					points[i].x >= 0 && points[i].y <= rc.height;
			if (i > 0) {
				float d = FloatMath.sqrt(sqr(points[i].x - points[i - 1].x) + 
						sqr(points[i].y - points[i - 1].y));
				distances[i-1]= d;
				roadLength += d;
				if(inside) {
					visibleRoadLength += d;
					if(!prevInside) {
						startVisible = i - 1;
					}
				} else if(prevInside) {
					if(visibleRoadLength >= normalTextLen) {
						break;
					}
					visibleRoadLength = 0;
				}

			}
			prevInside = inside;
		}
		if (textw >= roadLength) {
			return false;
		}
		int startInd = 0;
		int endInd = len;

		if(textw < visibleRoadLength &&  last - startVisible > 1) {
			startInd = startVisible;
			endInd = last;
			// display long road name in center
			if (visibleRoadLength > 3 * textw) {
				boolean ch ;
				do {
					ch = false;
					if(endInd - startInd > 2 && visibleRoadLength - distances[startInd] > normalTextLen){
						visibleRoadLength -= distances[startInd];
						startInd++;
						ch = true;
					}
					if(endInd - startInd > 2 && visibleRoadLength - distances[endInd - 2] > normalTextLen){
						visibleRoadLength -= distances[endInd - 2];
						endInd--;
						ch = true;
					}
				} while(ch);
			}
		}
		// shrink path to display more text
		if (startInd > 0 || endInd < len) {
			// find subpath
			Path path = new Path(); 
			for (int i = startInd; i < endInd; i++) {
				if (i == startInd) {
					path.moveTo(points[i].x, points[i].y);
				} else {
					path.lineTo(points[i].x, points[i].y);
				}
			}
			p.drawOnPath = path;
		}
		// calculate vector of the road (px, py) to proper rotate it
		float px = 0;
		float py = 0;
		for (int i = startInd + 1; i < endInd; i++) {
			px += points[i].x - points[i - 1].x;
			py += points[i].y - points[i - 1].y;
		}
		float scale = 0.5f;
		float plen = (float) Math.sqrt(px * px + py * py);
		// vector ox,oy orthogonal to px,py to measure height
		float ox = -py;
		float oy = px;
		if(plen > 0) {
			float rot = (float) (-Math.atan2(px, py) + Math.PI / 2);
			if (rot < 0) rot += Math.PI * 2;
			if (rot > Math.PI / 2f && rot < 3 * Math.PI / 2f) {
				rot += Math.PI;
				inverse = true;
				ox = -ox;
				oy = -oy;
			}
			p.pathRotate = rot;
			ox *= (p.bounds.height() / plen) / 2;
			oy *= (p.bounds.height() / plen) / 2;
		}

		p.centerX = points[startInd].x + scale * px + ox;
		p.centerY = points[startInd].y + scale * py + oy;
		p.vOffset += p.textSize / 2 - 1;
//		p.hOffset = 0;

		if (inverse) {
			Path path = new Path();
			for (int i = endInd - 1; i >= startInd; i--) {
				if (i == endInd - 1) {
					path.moveTo(points[i].x, points[i].y);
				} else {
					path.lineTo(points[i].x, points[i].y);
				}
			}
			p.drawOnPath = path;
		}
		return true;
	}


}
