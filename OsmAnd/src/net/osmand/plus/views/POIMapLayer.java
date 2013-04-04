package net.osmand.plus.views;

import gnu.trove.set.hash.TIntHashSet;

import java.util.ArrayList;
import java.util.List;

import net.osmand.PlatformUtil;
import net.osmand.access.AccessibleToast;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.data.LatLon;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.PoiFilter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.resources.ResourceManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.RectF;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;

public class POIMapLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider {
	private static final int startZoom = 10;
	public static final int TEXT_WRAP = 15;
	public static final int TEXT_LINES = 3;
	public static final org.apache.commons.logging.Log log = PlatformUtil.getLog(POIMapLayer.class);
	
	
	private Paint pointAltUI;
	private Paint paintIcon;
	private Paint paintTextIcon;
	private Paint point;
	private OsmandMapTileView view;
	private List<Amenity> objects = new ArrayList<Amenity>();
	private final static int MAXIMUM_SHOW_AMENITIES = 5;
	
	private ResourceManager resourceManager;
	private PoiFilter filter;
	private DisplayMetrics dm;
	
	public POIMapLayer(MapActivity activity) {
	}

	
	public PoiFilter getFilter() {
		return filter;
	}
	
	public void setFilter(PoiFilter filter) {
		this.filter = filter;
	}
	
	public void getAmenityFromPoint(PointF point, List<? super Amenity> am){
		if (objects != null) {
			int ex = (int) point.x;
			int ey = (int) point.y;
			int compare = getRadiusPoi(view.getZoom());
			int radius = getRadiusPoi(view.getZoom()) * 3 / 2;
			try {
				for (int i = 0; i < objects.size(); i++) {
					Amenity n = objects.get(i);
					int x = view.getRotatedMapXForPoint(n.getLocation().getLatitude(), n.getLocation().getLongitude());
					int y = view.getRotatedMapYForPoint(n.getLocation().getLatitude(), n.getLocation().getLongitude());
					if (Math.abs(x - ex) <= compare && Math.abs(y - ey) <= compare) {
						compare = radius;
						am.add(n);
					}
				}
			} catch (IndexOutOfBoundsException e) {
				// that's really rare case, but is much efficient than introduce synchronized block
			}
		}
	}
	

	@Override
	public boolean onSingleTap(PointF point) {
		List<Amenity> am = new ArrayList<Amenity>();
		getAmenityFromPoint(point, am);
		if(!am.isEmpty()){
			StringBuilder res = new StringBuilder();
			for (int i = 0; i < MAXIMUM_SHOW_AMENITIES && i < am.size(); i++) {
				Amenity n = am.get(i);
				if (i > 0) {
					res.append("\n\n");
				}
				buildPoiInformation(res, n);
			}
			AccessibleToast.makeText(view.getContext(), res.toString(), Toast.LENGTH_SHORT).show();
			return true;
		}
		return false;
	}


	private StringBuilder buildPoiInformation(StringBuilder res, Amenity n) {
		String format = OsmAndFormatter.getPoiSimpleFormat(n, view.getApplication(), view.getSettings().USE_ENGLISH_NAMES.get());
		res.append(" " + format);
		if (n.getOpeningHours() != null) {
			res.append("\n").append(view.getContext().getString(R.string.opening_hours)).append(" : ").append(n.getOpeningHours()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (n.getPhone() != null) {
			res.append("\n").append(view.getContext().getString(R.string.phone)).append(" : ").append(n.getPhone()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (n.getSite() != null && n.getType() != AmenityType.OSMWIKI) {
			res.append("\n").append(view.getContext().getString(R.string.website)).append(" : ").append(n.getSite()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return res;
	}
	
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		dm = new DisplayMetrics();
		WindowManager wmgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
		wmgr.getDefaultDisplay().getMetrics(dm);

		pointAltUI = new Paint();
		pointAltUI.setColor(view.getApplication().getResources().getColor(R.color.poi_background));
		pointAltUI.setStyle(Style.FILL);
		
		paintIcon = new Paint();
		
		paintTextIcon = new Paint();
		paintTextIcon.setTextSize(12 * dm.density);
		paintTextIcon.setTextAlign(Align.CENTER);
		paintTextIcon.setAntiAlias(true);
		
		point = new Paint();
		point.setColor(Color.GRAY);
		point.setAntiAlias(true);
		point.setStyle(Style.STROKE);
		resourceManager = view.getApplication().getResourceManager();
	}
	
	public int getRadiusPoi(int zoom){
		int r = 0;
		if(zoom < startZoom){
			r = 0;
		} else if(zoom <= 15){
			r = 10;
		} else if(zoom == 16){
			r = 14;
		} else if(zoom == 17){
			r = 16;
		} else {
			r = 18;
		}
		return (int) (r * dm.density);
	}
	
	
	@Override
	public void onDraw(Canvas canvas, RectF latLonBounds, RectF tilesRect, DrawSettings nightMode) {
		
		if (view.getZoom() >= startZoom) {
			objects.clear();
			resourceManager.searchAmenitiesAsync(latLonBounds.top, latLonBounds.left, latLonBounds.bottom, latLonBounds.right, view.getZoom(), filter, objects);
			int r = getRadiusPoi(view.getZoom());
			for (Amenity o : objects) {
				int x = view.getRotatedMapXForPoint(o.getLocation().getLatitude(), o.getLocation().getLongitude());
				int y = view.getRotatedMapYForPoint(o.getLocation().getLatitude(), o.getLocation().getLongitude());
				canvas.drawCircle(x, y, r, pointAltUI);
				canvas.drawCircle(x, y, r, point);
				String id = null;
				if(RenderingIcons.containsIcon(o.getSubType())){
					id = o.getSubType();
				} else if (RenderingIcons.containsIcon(o.getType().getDefaultTag() + "_" + o.getSubType())) {
					id = o.getType().getDefaultTag() + "_" + o.getSubType();
				}
				if(id != null){
					Bitmap bmp = RenderingIcons.getIcon(view.getContext(), id);
					if(bmp != null){
						canvas.drawBitmap(bmp, x - bmp.getWidth() / 2, y - bmp.getHeight() / 2, paintIcon);
					}
				}
			}
			
			if (view.getSettings().SHOW_POI_LABEL.get()) {
				TIntHashSet set = new TIntHashSet();
				for (Amenity o : objects) {
					int x = view.getRotatedMapXForPoint(o.getLocation().getLatitude(), o.getLocation().getLongitude());
					int y = view.getRotatedMapYForPoint(o.getLocation().getLatitude(), o.getLocation().getLongitude());
					int tx = view.getMapXForPoint(o.getLocation().getLongitude());
					int ty = view.getMapYForPoint(o.getLocation().getLatitude());
					String name = o.getName(view.getSettings().USE_ENGLISH_NAMES.get());
					if (name != null && name.length() > 0) {
						int lines = 0;
						while (lines < TEXT_LINES) {
							if (set.contains(division(tx, ty, 0, lines)) ||
									set.contains(division(tx, ty, -1, lines)) || set.contains(division(tx, ty, +1, lines))) {
								break;
							}
							lines++;
						}
						if (lines == 0) {
							// drawWrappedText(canvas, "...", paintTextIcon.getTextSize(), x, y + r + 2 + paintTextIcon.getTextSize() / 2, 1);
						} else {
							drawWrappedText(canvas, name, paintTextIcon.getTextSize(), x, y + r + 2 + paintTextIcon.getTextSize() / 2,
									lines);
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
	public void destroyLayer() {
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}
	@Override
	public void populateObjectContextMenu(Object o, ContextMenuAdapter adapter) {
		if(o instanceof Amenity) {
			final Amenity a = (Amenity) o;
			OnContextMenuClick listener = new ContextMenuAdapter.OnContextMenuClick() {
				@Override
				public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
					if (itemId == R.string.poi_context_menu_call) {
						try {
							Intent intent = new Intent(Intent.ACTION_VIEW);
							intent.setData(Uri.parse("tel:"+a.getPhone())); //$NON-NLS-1$
							view.getContext().startActivity(intent);
						} catch (RuntimeException e) {
							log.error("Failed to invoke call", e); //$NON-NLS-1$
							AccessibleToast.makeText(view.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
						}
					} else if (itemId == R.string.poi_context_menu_website) {
						try {
							Intent intent = new Intent(Intent.ACTION_VIEW);
							intent.setData(Uri.parse(a.getSite())); 
							view.getContext().startActivity(intent);
						} catch (RuntimeException e) {
							log.error("Failed to invoke call", e); //$NON-NLS-1$
							AccessibleToast.makeText(view.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
						}
					} else if (itemId == R.string.poi_context_menu_showdescription) {
						showDescriptionDialog(a);
					}
				}
			};
			if(a.getDescription() != null){
				adapter.registerItem(R.string.poi_context_menu_showdescription, R.drawable.list_activities_show_poi_description, listener, -1);
			}
			if(a.getPhone() != null){
				adapter.registerItem(R.string.poi_context_menu_call, R.drawable.list_activities_show_poi_phone, listener, -1);
			}
			if(a.getSite() != null){
				adapter.registerItem(R.string.poi_context_menu_website, R.drawable.list_activities_poi_show_website, listener, -1);
			}
		}
	}

	private void showDescriptionDialog(Amenity a) {
		Builder bs = new AlertDialog.Builder(view.getContext());
		bs.setTitle(OsmAndFormatter.getPoiSimpleFormat(a, view.getApplication(), view.getSettings().USE_ENGLISH_NAMES.get()));
		bs.setMessage(a.getDescription());
		bs.show();
	}
	
	@Override
	public String getObjectDescription(Object o) {
		if(o instanceof Amenity){
			return buildPoiInformation(new StringBuilder(), (Amenity) o).toString();
		}
		return null;
	}
	
	@Override
	public String getObjectName(Object o) {
		if(o instanceof Amenity){
			return ((Amenity)o).getName(); //$NON-NLS-1$
		}
		return null;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, List<Object> objects) {
		getAmenityFromPoint(point, objects);
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if(o instanceof Amenity){
			return ((Amenity)o).getLocation();
		}
		return null;
	}

}
