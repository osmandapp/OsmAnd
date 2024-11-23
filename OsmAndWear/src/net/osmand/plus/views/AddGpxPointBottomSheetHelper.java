package net.osmand.plus.views;

import android.graphics.PointF;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.editors.WptPtEditor;
import net.osmand.plus.mapcontextmenu.editors.WptPtEditor.OnDismissListener;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.views.layers.ContextMenuLayer;
import net.osmand.util.Algorithms;

import java.io.File;

public class AddGpxPointBottomSheetHelper implements OnDismissListener {

	private final OsmandApplication app;

	private final MapActivity mapActivity;
	private final ContextMenuLayer menuLayer;

	private final View view;
	private final TextView title;
	private final TextView description;
	private final ImageView icon;

	private String titleText;
	private NewGpxPoint newGpxPoint;
	private PointDescription pointDescription;
	private boolean applyingPositionMode;

	public AddGpxPointBottomSheetHelper(@NonNull MapActivity mapActivity, @NonNull ContextMenuLayer layer) {
		this.mapActivity = mapActivity;
		menuLayer = layer;
		app = mapActivity.getMyApplication();

		view = mapActivity.findViewById(R.id.add_gpx_point_bottom_sheet);
		title = view.findViewById(R.id.add_gpx_point_bottom_sheet_title);
		description = view.findViewById(R.id.description);
		icon = view.findViewById(R.id.icon);

		view.findViewById(R.id.create_button).setOnClickListener(v -> {
			menuLayer.createGpxPoint();
			if (pointDescription.isWpt()) {
				RotatedTileBox tileBox = mapActivity.getMapView().getRotatedTileBox();
				GpxFile gpx = newGpxPoint.getGpx();
				LatLon latLon = menuLayer.getMovableCenterLatLon(tileBox);
				WptPtEditor editor = mapActivity.getContextMenu().getWptPtPointEditor();
				if (editor != null) {
					editor.setOnDismissListener(this);
					editor.setNewGpxPointProcessing();
					editor.add(gpx, latLon, titleText, null);
				}
			}
		});
		view.findViewById(R.id.cancel_button).setOnClickListener(v -> {
			hide();
			menuLayer.cancelAddGpxPoint();
			onClose();
		});
	}

	public void onDraw(@NonNull RotatedTileBox tileBox) {
		PointF point = menuLayer.getMovableCenterPoint(tileBox);
		double lat = tileBox.getLatFromPixel(point.x, point.y);
		double lon = tileBox.getLonFromPixel(point.x, point.y);
		description.setText(PointDescription.getLocationName(mapActivity, lat, lon, true));
	}

	public void setTitle(@NonNull String title) {
		if (Algorithms.isEmpty(title) && pointDescription.isWpt()) {
			title = mapActivity.getString(R.string.waypoint_one);
		}
		titleText = title;
		this.title.setText(titleText);
	}

	public boolean isVisible() {
		return view.getVisibility() == View.VISIBLE;
	}

	public void show(@NonNull NewGpxPoint newPoint) {
		this.newGpxPoint = newPoint;
		pointDescription = newPoint.getPointDescription();
		if (pointDescription.isWpt()) {
			setTitle(mapActivity.getString(R.string.waypoint_one));
			icon.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_marker_dark));
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
		TrackMenuFragment fragment = mapActivity.getFragmentsHelper().getTrackMenuFragment();
		if (fragment != null) {
			fragment.updateContent();
			fragment.show();
		} else {
			TrackMenuFragment.openTrack(mapActivity, new File(newGpxPoint.getGpx().getPath()), null);
		}
	}

	public static class NewGpxPoint {
		private final PointDescription pointDescription;
		private final GpxFile gpx;
		private final QuadRect rect;

		public NewGpxPoint(GpxFile gpx, PointDescription pointDescription, QuadRect rect) {
			this.gpx = gpx;
			this.pointDescription = pointDescription;
			this.rect = rect;
		}

		public GpxFile getGpx() {
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
