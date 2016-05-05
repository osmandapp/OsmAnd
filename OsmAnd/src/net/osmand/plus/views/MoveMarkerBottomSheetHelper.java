package net.osmand.plus.views;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.IconsCache;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public class MoveMarkerBottomSheetHelper {
	private final View mView;
	private final TextView mDescription;
	private final Context mContext;
	private final ContextMenuLayer mContextMenuLayer;

	public MoveMarkerBottomSheetHelper(MapActivity activity, ContextMenuLayer contextMenuLayer) {
		mContextMenuLayer = contextMenuLayer;
		this.mView = activity.findViewById(R.id.move_marker_bottom_sheet);
		ImageView icon = (ImageView) mView.findViewById(R.id.icon);
		this.mDescription = (TextView) mView.findViewById(R.id.description);
		this.mContext = activity;

		IconsCache iconsCache = activity.getMyApplication().getIconsCache();
		icon.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_photo_dark, R.color.marker_green));
		mView.findViewById(R.id.apply_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				hide();
				mContextMenuLayer.applyNewMarkerPosition();
			}
		});
		mView.findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				hide();
				mContextMenuLayer.cancelMovingMarker();
			}
		});
	}

	public void show(double lat, double lon) {
		mView.setVisibility(View.VISIBLE);
		mDescription.setText(mContext.getString(R.string.lat_lon_pattern, lat, lon));
	}

	public void hide() {
		mView.setVisibility(View.GONE);
	}
}
