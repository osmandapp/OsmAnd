package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import net.osmand.aidl.maplayer.point.AMapPoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.widgets.tools.CropCircleTransformation;
import net.osmand.util.Algorithms;

import java.io.IOException;
import java.io.InputStream;

public class AMapPointMenuController extends MenuController {

	private AMapPoint point;

	private Drawable pointDrawable;

	public AMapPointMenuController(@NonNull MapActivity mapActivity, @NonNull PointDescription pointDescription, @NonNull AMapPoint point) {
		super(new MenuBuilder(mapActivity), pointDescription, mapActivity);
		this.point = point;
		pointDrawable = getPointDrawable();
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof AMapPoint) {
			this.point = (AMapPoint) object;
		}
	}

	@Override
	protected Object getObject() {
		return point;
	}

	@Override
	public boolean displayDistanceDirection() {
		return true;
	}

	@Override
	public void addPlainMenuItems(String typeStr, PointDescription pointDescription, final LatLon latLon) {
		for (String detail : point.getDetails()) {
			builder.addPlainMenuItem(R.drawable.ic_action_info_dark, detail, true, false, null);
		}
		super.addPlainMenuItems(typeStr, pointDescription, latLon);
	}

	@Override
	public Drawable getRightIcon() {
		if (pointDrawable != null) {
			return pointDrawable;
		}
		return getIcon(R.drawable.ic_action_get_my_location);
	}

	@Override
	public boolean isBigRightIcon() {
		return pointDrawable != null;
	}

	@Override
	public Drawable getSecondLineTypeIcon() {
		if (!Algorithms.isEmpty(point.getShortName())) {
			return getIcon(R.drawable.ic_small_group);
		} else {
			return null;
		}
	}

	@NonNull
	@Override
	public String getTypeStr() {
		if (!Algorithms.isEmpty(point.getTypeName())) {
			return point.getTypeName();
		} else {
			return "";
		}
	}

	@NonNull
	@Override
	public String getCommonTypeStr() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return mapActivity.getString(R.string.shared_string_location);
		} else {
			return "";
		}
	}

	@Override
	public boolean needStreetName() {
		return false;
	}

	@Nullable
	private Drawable getPointDrawable() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return null;
		}

		String imageUriStr = point.getParams().get(AMapPoint.POINT_IMAGE_URI_PARAM);
		if (TextUtils.isEmpty(imageUriStr)) {
			return null;
		}

		Uri fileUri = Uri.parse(imageUriStr);
		try {
			InputStream ims = mapActivity.getContentResolver().openInputStream(fileUri);
			if (ims != null) {
				Bitmap bitmap = BitmapFactory.decodeStream(ims);
				if (bitmap != null) {
					bitmap = new CropCircleTransformation().transform(bitmap);
					return new BitmapDrawable(mapActivity.getResources(), bitmap);
				}
				ims.close();
			}
		} catch (IOException e) {
			// ignore
		}

		return null;
	}
}