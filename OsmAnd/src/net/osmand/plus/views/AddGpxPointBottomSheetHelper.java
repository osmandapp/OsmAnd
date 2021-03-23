package net.osmand.plus.views;

import android.graphics.PointF;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.editors.RtePtEditor;
import net.osmand.plus.mapcontextmenu.editors.WptPtEditor;
import net.osmand.plus.mapcontextmenu.editors.WptPtEditor.OnDismissListener;
import net.osmand.plus.track.TrackMenuFragment;
import net.osmand.plus.views.layers.ContextMenuLayer;

import java.io.File;

public class AddGpxPointBottomSheetHelper implements OnDismissListener {
	private final View view;
	private final TextView title;
	private final TextView description;
	private final ImageView icon;
	private final MapActivity mapActivity;
	private final MapContextMenu contextMenu;
	private final ContextMenuLayer contextMenuLayer;
	private final UiUtilities iconsCache;
	private String titleText;
	private boolean applyingPositionMode;
	private NewGpxPoint newGpxPoint;
	private PointDescription pointDescription;

	public AddGpxPointBottomSheetHelper(final MapActivity activity, ContextMenuLayer ctxMenuLayer) {
		this.contextMenuLayer = ctxMenuLayer;
		iconsCache = activity.getMyApplication().getUIUtilities();
		mapActivity = activity;
		contextMenu = activity.getContextMenu();
		view = activity.findViewById(R.id.add_gpx_point_bottom_sheet);
		title = (TextView) view.findViewById(R.id.add_gpx_point_bottom_sheet_title);
		description = (TextView) view.findViewById(R.id.description);
		icon = (ImageView) view.findViewById(R.id.icon);

		view.findViewById(R.id.create_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				contextMenuLayer.createGpxPoint();
				GPXFile gpx = newGpxPoint.getGpx();
				LatLon latLon = contextMenu.getLatLon();
				if (pointDescription.isWpt()) {
					WptPtEditor editor = activity.getContextMenu().getWptPtPointEditor();
					if (editor != null) {
						editor.setOnDismissListener(AddGpxPointBottomSheetHelper.this);
						editor.setNewGpxPointProcessing(true);
						editor.add(gpx, latLon, titleText);
					}
				} else if (pointDescription.isRte()) {
					RtePtEditor editor = activity.getContextMenu().getRtePtPointEditor();
					if (editor != null) {
						editor.setOnDismissListener(AddGpxPointBottomSheetHelper.this);
						editor.setNewGpxPointProcessing(true);
						editor.add(gpx, latLon, titleText);
					}
				}
			}
		});
		view.findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				hide();
				contextMenuLayer.cancelAddGpxPoint();
				onClose();
			}
		});
	}

	public void onDraw(RotatedTileBox rt) {
		PointF point = contextMenuLayer.getMovableCenterPoint(rt);
		double lat = rt.getLatFromPixel(point.x, point.y);
		double lon = rt.getLonFromPixel(point.x, point.y);
		description.setText(PointDescription.getLocationName(mapActivity, lat, lon, true));
	}

	public void setTitle(String title) {
		if (title.isEmpty()) {
			if (pointDescription.isWpt()) {
				title = mapActivity.getString(R.string.waypoint_one);
			} else if (pointDescription.isRte()) {
				title = mapActivity.getString(R.string.route_point_one);
			}
		}
		titleText = title;
		this.title.setText(titleText);
	}

	public boolean isVisible() {
		return view.getVisibility() == View.VISIBLE;
	}

	public void show(NewGpxPoint newPoint) {
		this.newGpxPoint = newPoint;
		pointDescription = newPoint.getPointDescription();
		if (pointDescription.isWpt()) {
			setTitle(mapActivity.getString(R.string.waypoint_one));
			icon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_marker_dark));
		} else if (pointDescription.isRte()) {
			setTitle(mapActivity.getString(R.string.route_point_one));
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

	@Override
	public void onDismiss() {
		MapContextMenu contextMenu = mapActivity.getContextMenu();
		if (contextMenu.isVisible() && contextMenu.isClosable()) {
			contextMenu.close();
		}
		onClose();
	}

	private void onClose() {
		TrackMenuFragment fragment = mapActivity.getTrackMenuFragment();
		if (fragment != null) {
			fragment.updateContent();
			fragment.show();
		} else {
			TrackMenuFragment.openTrack(mapActivity, new File(newGpxPoint.getGpx().path), null);
		}
	}

	public static class NewGpxPoint {
		private PointDescription pointDescription;
		private GPXFile gpx;
		private QuadRect rect;

		public NewGpxPoint(GPXFile gpx, PointDescription pointDescription, QuadRect rect) {
			this.gpx = gpx;
			this.pointDescription = pointDescription;
			this.rect = rect;
		}

		public GPXFile getGpx() {
			return gpx;
		}

		public PointDescription getPointDescription() {
			return pointDescription;
		}

		public QuadRect getRect() {
			return rect;
		}
	}
}
