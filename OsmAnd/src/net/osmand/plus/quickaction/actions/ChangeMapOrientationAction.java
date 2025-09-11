package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.CHANGE_MAP_ORIENTATION_ACTION;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.quickaction.CreateEditActionDialog;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.quickaction.SelectMultipleOrientationsBottomSheet;
import net.osmand.plus.quickaction.SwitchableAction;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.CompassMode;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.controls.maphudbuttons.QuickActionButton;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ChangeMapOrientationAction extends SwitchableAction<String> {
	private static final String KEY_MODES = "compass_modes";

	public static final QuickActionType TYPE = new QuickActionType(CHANGE_MAP_ORIENTATION_ACTION,
			"change.map.orientation", ChangeMapOrientationAction.class)
			.nameActionRes(R.string.shared_string_change)
			.nameRes(R.string.rotate_map_to)
			.iconRes(R.drawable.ic_action_compass_rotated)
			.category(QuickActionType.SETTINGS)
			.nonEditable();

	public ChangeMapOrientationAction() {
		super(TYPE);
	}

	public ChangeMapOrientationAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity, @Nullable Bundle params) {
		List<String> profiles = loadListFromParams();
		if (profiles.isEmpty()) {
			mapActivity.getMapViewTrackingUtilities().requestSwitchCompassToNextMode();
			return;
		}

		boolean showDialog = Boolean.parseBoolean(getParams().get(KEY_DIALOG));
		if (showDialog) {
			showChooseDialog(mapActivity);
			return;
		}
		String nextMode = getNextSelectedItem(mapActivity.getApp());
		executeWithParams(mapActivity, nextMode);
	}

	@Override
	public int getIconRes(Context context) {
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		OsmandSettings settings = app.getSettings();
		CompassMode compassMode = settings.getCompassMode();
		boolean nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.APP);
		return compassMode.getIconId(nightMode);
	}

	@Override
	public String getItemIdFromObject(String object) {
		return object;
	}

	@Override
	public List<String> loadListFromParams() {
		String json = getParams().get(getListKey());

		if (json == null || json.isEmpty()) return new ArrayList<>();

		Type listType = new TypeToken<ArrayList<String>>() {
		}.getType();

		List<String> list = new Gson().fromJson(json, listType);

		Iterator<String> it = list.iterator();
		while (it.hasNext()) {
			CompassMode compassMode = CompassMode.getModeForKey(it.next());
			if (compassMode == null) {
				it.remove();
			}
		}

		return list;
	}

	@Override
	public void executeWithParams(@NonNull MapActivity activity, String params) {
		CompassMode compassMode = CompassMode.getModeForKey(params);
		if (compassMode != null) {
			OsmandApplication app = activity.getApp();
			MapViewTrackingUtilities trackingUtilities = app.getMapViewTrackingUtilities();
			trackingUtilities.switchCompassModeTo(compassMode);

			MapLayers mapLayers = activity.getMapLayers();
			QuickActionButton selectedButton = mapLayers.getMapQuickActionLayer().getSelectedButton();
			if (selectedButton != null && selectedButton.getButtonState() != null) {
				app.getMapButtonsHelper().setQuickActionFabState(selectedButton.getButtonState(), true);
			}
		}
	}

	@Override
	public String getTranslatedItemName(Context context, String item) {
		CompassMode compassMode = CompassMode.valueOf(item);
		return compassMode.getTitle(context);
	}

	@Override
	public String getDisabledItem(OsmandApplication app) {
		return null;
	}

	@Override
	public String getSelectedItem(OsmandApplication app) {
		return app.getSettings().getCompassMode().getKey();
	}

	@Override
	public String getNextSelectedItem(OsmandApplication app) {
		List<String> modes = loadListFromParams();
		if (!modes.isEmpty()) {
			String currentProfile = getSelectedItem(app);

			int index = -1;
			for (int idx = 0; idx < modes.size(); idx++) {
				if (currentProfile.equals(modes.get(idx))) {
					index = idx;
					break;
				}
			}

			String nextMode = modes.get(0);
			if (index >= 0 && index + 1 < modes.size()) {
				nextMode = modes.get(index + 1);
			}
			return nextMode;
		}
		return null;
	}

	@Override
	protected String getTitle(List<String> filters, @NonNull Context ctx) {
		List<String> profileNames = new ArrayList<>();
		for (String key : filters) {
			CompassMode compassMode = CompassMode.valueOf(key);
			profileNames.add(compassMode.getTitle(ctx));
		}
		return TextUtils.join(", ", profileNames);
	}

	@Override
	protected void saveListToParams(List<String> list) {
		getParams().put(getListKey(), new Gson().toJson(list));
	}

	@Override
	protected String getItemName(Context context, String item) {
		CompassMode mode = CompassMode.valueOf(item);
		return mode.getTitle(context);
	}

	@Override
	protected int getAddBtnText() {
		return R.string.shared_string_add;
	}

	@Override
	protected int getDiscrHint() {
		return R.string.map_orientations_choose_description;
	}

	@Override
	protected int getDiscrTitle() {
		return R.string.map_orientations;
	}

	@Override
	protected String getListKey() {
		return KEY_MODES;
	}

	@Override
	protected OnClickListener getOnAddBtnClickListener(MapActivity activity, SwitchableAction<String>.Adapter adapter) {
		return v -> {
			CreateEditActionDialog targetFragment = (CreateEditActionDialog) activity
					.getSupportFragmentManager().findFragmentByTag(CreateEditActionDialog.TAG);
			List<String> selectedProfilesKeys = new ArrayList<>(adapter.getItemsList());
			SelectMultipleOrientationsBottomSheet.showInstance(activity, targetFragment,
					selectedProfilesKeys, selectedProfilesKeys, false);
		};
	}

	@Override
	protected void setIcon(@NonNull OsmandApplication app, String item, @NonNull ImageView imageView, @NonNull ProgressBar iconProgressBar) {
		imageView.setImageDrawable(app.getUIUtilities().getIcon(getItemIconRes(app, item)));
	}

	@Override
	public boolean fillParams(@NonNull View root, @NonNull MapActivity mapActivity) {
		getParams().put(KEY_DIALOG, Boolean.toString(((SwitchCompat) root.findViewById(R.id.saveButton)).isChecked()));
		return super.fillParams(root, mapActivity);
	}

	@Override
	protected int getItemIconRes(Context context, String item) {
		CompassMode compassMode = CompassMode.valueOf(item);
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		boolean nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.APP);
		return compassMode.getIconId(nightMode);
	}

	@Override
	protected void onItemsSelected(Context ctx, List<String> selectedItems) {
		Adapter adapter = getAdapter();
		if (adapter == null) {
			return;
		}
		for (String key : selectedItems) {
			adapter.addItem(key, ctx);
		}
	}
}
