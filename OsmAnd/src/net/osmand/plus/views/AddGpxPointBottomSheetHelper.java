package net.osmand.plus.views;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MapContextMenu;

public class AddGpxPointBottomSheetHelper {
	private final View view;
	private final TextView title;
	private final TextView description;
	private final ImageView icon;
	private final Context context;
	private final MapContextMenu contextMenu;
	private final ContextMenuLayer contextMenuLayer;
	private final IconsCache iconsCache;
	private String titleText;
	private boolean applyingPositionMode;
	private NewPoint newPoint;
	private PointDescription pointDescription;

	public AddGpxPointBottomSheetHelper(final MapActivity activity, ContextMenuLayer ctxMenuLayer) {
		this.contextMenuLayer = ctxMenuLayer;
		iconsCache = activity.getMyApplication().getIconsCache();
		context = activity;
		contextMenu = activity.getContextMenu();
		view = activity.findViewById(R.id.add_gpx_point_bottom_sheet);
		title = (TextView) view.findViewById(R.id.add_gpx_point_bottom_sheet_title);
		description = (TextView) view.findViewById(R.id.description);
		icon = (ImageView) view.findViewById(R.id.icon);

		view.findViewById(R.id.create_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				contextMenuLayer.createGpxPoint();
				GPXFile gpx = newPoint.getGpx();
				LatLon latLon = contextMenu.getLatLon();
				activity.getContextMenu().getWptPtPointEditor().add(gpx, latLon, titleText, pointDescription);
			}
		});
		view.findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				hide();
				contextMenuLayer.cancelAddGpxPoint();
			}
		});
	}

	public void onDraw(RotatedTileBox rt) {
		PointF point = contextMenuLayer.getMovableCenterPoint(rt);
		double lat = rt.getLatFromPixel(point.x, point.y);
		double lon = rt.getLonFromPixel(point.x, point.y);
		description.setText(PointDescription.getLocationName(context, lat, lon, true));
	}

	public void setTitle(String title) {
		if (title.equals("")) {
			if (pointDescription.isWpt()) {
				title = context.getString(R.string.waypoint_one);
			} else if (pointDescription.isRoutePoint()) {
				title = context.getString(R.string.route_point_one);
			}
		}
		titleText = title;
		this.title.setText(titleText);
	}

	public boolean isVisible() {
		return view.getVisibility() == View.VISIBLE;
	}

	public void show(NewPoint newPoint) {
		this.newPoint = newPoint;
		pointDescription = newPoint.getPointDescription();
		if (pointDescription.isWpt()) {
			setTitle(context.getString(R.string.waypoint_one));
			icon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_marker_dark));
		} else if (pointDescription.isRoutePoint()) {
			setTitle(context.getString(R.string.route_point_one));
			icon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_markers_dark));
		}
		exitApplyPositionMode();
		view.setVisibility(View.VISIBLE);
	}

	public void hide() {
		exitApplyPositionMode();
		view.setVisibility(View.GONE);
	}

	public void enterApplyPositionMode() {
		if (!applyingPositionMode) {
			applyingPositionMode = true;
			view.findViewById(R.id.create_button).setEnabled(false);
		}
	}

	public void exitApplyPositionMode() {
		if (applyingPositionMode) {
			applyingPositionMode = false;
			view.findViewById(R.id.create_button).setEnabled(true);
		}
	}

	public static class NewPoint {
		private PointDescription pointDescription;
		private GPXFile gpx;

		public NewPoint(GPXFile gpx, PointDescription pointDescription) {
			this.gpx = gpx;
			this.pointDescription = pointDescription;
		}

		public GPXFile getGpx() {
			return gpx;
		}

		public PointDescription getPointDescription() {
			return pointDescription;
		}
	}
}
