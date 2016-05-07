package net.osmand.plus.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import net.osmand.data.RotatedTileBox;
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
	
	public void onDraw(RotatedTileBox rt) {
		double lat = rt.getLatFromPixel(rt.getPixWidth() / 2, rt.getPixHeight() / 2);
		double lon = rt.getLonFromPixel(rt.getPixWidth() / 2, rt.getPixHeight() / 2);
		mDescription.setText(mContext.getString(R.string.lat_lon_pattern, lat, lon));
	}
	
	public boolean isVisible() {
		return mView.getVisibility() == View.VISIBLE;
	}

	public void show(Drawable drawable) {
		mView.setVisibility(View.VISIBLE);
		((ImageView) mView.findViewById(R.id.icon)).setImageDrawable(drawable);
	}

	public void hide() {
		mView.setVisibility(View.GONE);
	}
}
