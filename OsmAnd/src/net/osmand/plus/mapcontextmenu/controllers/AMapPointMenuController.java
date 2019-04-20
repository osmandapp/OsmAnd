package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Pair;

import net.osmand.aidl.contextmenu.AContextMenuButton;
import net.osmand.aidl.contextmenu.ContextMenuButtonsParams;
import net.osmand.aidl.maplayer.point.AMapPoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.widgets.tools.CropCircleTransformation;
import net.osmand.util.Algorithms;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AMapPointMenuController extends MenuController {

	private static final float NO_SPEED = -1;
	private static final int NO_ICON = 0;

	private AMapPoint point;

	private Drawable pointDrawable;

	public AMapPointMenuController(@NonNull MapActivity mapActivity, @NonNull PointDescription pointDescription, @NonNull final AMapPoint point) {
		super(new MenuBuilder(mapActivity), pointDescription, mapActivity);
		this.point = point;
		pointDrawable = getPointDrawable();
		final OsmandApplication app = mapActivity.getMyApplication();
		Map<String, ContextMenuButtonsParams> buttonsParamsMap = app.getAidlApi().getContextMenuButtonsParams();
		if (!buttonsParamsMap.isEmpty()) {
			additionalButtonsControllers = new ArrayList<>();
			for (ContextMenuButtonsParams buttonsParams : buttonsParamsMap.values()) {
				List<String> pointsIds = buttonsParams.getPointsIds();
				if (((pointsIds == null || pointsIds.isEmpty()) || pointsIds.contains(point.getId())) || buttonsParams.getLayerId().equals(point.getLayerId())) {
					long callbackId = buttonsParams.getCallbackId();
					TitleButtonController leftButtonController = createAdditionButtonController(buttonsParams.getLeftButton(), callbackId);
					TitleButtonController rightButtonController = createAdditionButtonController(buttonsParams.getRightButton(), callbackId);
					additionalButtonsControllers.add(Pair.create(leftButtonController, rightButtonController));
				}
			}
		}
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
		int id = getPointTypeIconId();
		if (id != NO_ICON) {
			return getIcon(id);
		}
		return null;
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
	public int getAdditionalInfoColorId() {
		return R.color.icon_color;
	}

	@Override
	public CharSequence getAdditionalInfoStr() {
		MapActivity activity = getMapActivity();
		if (activity != null) {
			float speed = getPointSpeed();
			if (speed != NO_SPEED) {
				String formatted = OsmAndFormatter.getFormattedSpeed(speed, activity.getMyApplication());
				return activity.getString(R.string.map_widget_speed) + ": " + formatted;
			}
		}
		return super.getAdditionalInfoStr();
	}

	@Override
	public int getAdditionalInfoIconRes() {
		if (getPointSpeed() != NO_SPEED) {
			return R.drawable.ic_action_speed_16;
		}
		return super.getAdditionalInfoIconRes();
	}

	@Override
	public boolean needStreetName() {
		return false;
	}

	private TitleButtonController createAdditionButtonController(final AContextMenuButton contextMenuButton, final long callbackId) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null || contextMenuButton == null) {
			return null;
		}
		TitleButtonController titleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					int buttonId = contextMenuButton.getButtonId();
					String pointId = point.getId();
					String layerId = point.getLayerId();
					mapActivity.getMyApplication().getAidlApi().contextMenuCallbackButtonClicked(callbackId, buttonId, pointId, layerId);
				}
			}
		};
		titleButtonController.caption = contextMenuButton.getLeftTextCaption();
		titleButtonController.rightTextCaption = contextMenuButton.getRightTextCaption();
		titleButtonController.leftIconId = getIconIdByName(contextMenuButton.getLeftIconName());
		titleButtonController.rightIconId = getIconIdByName(contextMenuButton.getRightIconName());
		titleButtonController.enabled = contextMenuButton.isEnabled();
		titleButtonController.needColorizeIcon = contextMenuButton.isNeedColorizeIcon();

		return titleButtonController;
	}

	private int getPointTypeIconId() {
		MapActivity activity = getMapActivity();
		if (activity != null) {
			String iconName = point.getParams().get(AMapPoint.POINT_TYPE_ICON_NAME_PARAM);
			if (!TextUtils.isEmpty(iconName)) {
				return getIconIdByName(iconName);
			}
		}
		if (!TextUtils.isEmpty(point.getShortName())) {
			return R.drawable.ic_small_group;
		}
		return NO_ICON;
	}

	private int getIconIdByName(String iconName) {
		MapActivity activity = getMapActivity();
		if (activity != null && !TextUtils.isEmpty(iconName)) {
			OsmandApplication app = activity.getMyApplication();
			return app.getResources().getIdentifier(iconName, "drawable", app.getPackageName());
		}
		return 0;
	}

	private float getPointSpeed() {
		String speed = point.getParams().get(AMapPoint.POINT_SPEED_PARAM);
		if (!TextUtils.isEmpty(speed)) {
			try {
				return Float.parseFloat(speed);
			} catch (NumberFormatException e) {
				return NO_SPEED;
			}
		}
		return NO_SPEED;
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