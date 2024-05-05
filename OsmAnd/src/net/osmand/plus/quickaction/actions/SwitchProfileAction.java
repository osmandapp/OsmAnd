package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.SWITCH_PROFILE_ACTION_ID;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.profiles.SelectMultipleProfilesBottomSheet;
import net.osmand.plus.quickaction.CreateEditActionDialog;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.quickaction.SwitchableAction;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.controls.maphudbuttons.QuickActionButton;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SwitchProfileAction extends SwitchableAction<String> {

	private static final String KEY_PROFILES = "profiles";

	public static final QuickActionType TYPE = new QuickActionType(SWITCH_PROFILE_ACTION_ID,
			"profile.change", SwitchProfileAction.class)
			.nameRes(R.string.change_application_profile)
			.iconRes(R.drawable.ic_action_manage_profiles)
			.category(QuickActionType.NAVIGATION);

	public SwitchProfileAction() {
		super(TYPE);
	}

	public SwitchProfileAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	protected String getTitle(List<String> filters) {
		List<String> profileNames = new ArrayList<>();
		for (String key : filters) {
			ApplicationMode appMode = getModeForKey(key);
			if (appMode != null) {
				profileNames.add(appMode.toHumanString());
			}
		}
		return TextUtils.join(", ", profileNames);
	}

	@Override
	protected void saveListToParams(List<String> list) {
		getParams().put(getListKey(), new Gson().toJson(list));
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
			ApplicationMode appMode = getModeForKey(it.next());
			if (appMode == null) {
				it.remove();
			}
		}

		return list;
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		List<String> profiles = loadListFromParams();
		if (profiles.size() == 0) {
			Toast.makeText(mapActivity, mapActivity.getString(R.string.profiles_for_action_not_found),
					Toast.LENGTH_SHORT).show();
			return;
		}

		boolean showDialog = Boolean.parseBoolean(getParams().get(KEY_DIALOG));
		if (showDialog) {
			showChooseDialog(mapActivity);
			return;
		}
		String nextProfile = getNextSelectedItem(mapActivity.getMyApplication());
		executeWithParams(mapActivity, nextProfile);
	}

	@Override
	public void executeWithParams(@NonNull MapActivity mapActivity, String params) {
		ApplicationMode appMode = getModeForKey(params);
		if (appMode != null) {
			OsmandApplication app = mapActivity.getMyApplication();
			app.getSettings().setApplicationMode(appMode);

			MapLayers mapLayers = mapActivity.getMapLayers();
			QuickActionButton selectedButton = mapLayers.getMapQuickActionLayer().getSelectedButton();
			if (selectedButton != null) {
				app.getMapButtonsHelper().setQuickActionFabState(selectedButton.getButtonState(), true);
			}

			String message = String.format(mapActivity.getString(
					R.string.application_profile_changed), appMode.toHumanString());
			Toast.makeText(mapActivity, message, Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public String getTranslatedItemName(Context context, String item) {
		return getModeForKey(item).toHumanString();
	}

	@Override
	protected String getItemName(Context context, String item) {
		ApplicationMode appMode = getModeForKey(item);
		if (appMode != null) {
			return appMode.toHumanString();
		}
		return item;
	}

	@Override
	public String getDisabledItem(OsmandApplication app) {
		return null;
	}

	@Override
	public String getSelectedItem(OsmandApplication app) {
		ApplicationMode appMode = app.getSettings().getApplicationMode();
		return appMode.getStringKey();
	}

	@Override
	public String getNextSelectedItem(OsmandApplication app) {
		List<String> profiles = loadListFromParams();
		if (profiles.size() > 0) {
			String currentProfile = getSelectedItem(app);

			int index = -1;
			for (int idx = 0; idx < profiles.size(); idx++) {
				if (currentProfile.equals(profiles.get(idx))) {
					index = idx;
					break;
				}
			}

			String nextProfile = profiles.get(0);
			if (index >= 0 && index + 1 < profiles.size()) {
				nextProfile = profiles.get(index + 1);
			}
			return nextProfile;
		}
		return null;
	}

	@Override
	protected int getAddBtnText() {
		return R.string.shared_string_add_profile;
	}

	@Override
	protected int getDiscrHint() {
		return R.string.quick_action_switch_profile_descr;
	}

	@Override
	protected int getDiscrTitle() {
		return R.string.application_profiles;
	}

	@Override
	protected String getListKey() {
		return KEY_PROFILES;
	}

	@Override
	protected int getItemIconRes(Context context, String item) {
		ApplicationMode appMode = getModeForKey(item);
		if (appMode != null) {
			return appMode.getIconRes();
		}
		return super.getItemIconRes(context, item);
	}

	@Override
	@ColorInt
	protected int getItemIconColor(OsmandApplication app, String item) {
		ApplicationMode appMode = getModeForKey(item);
		if (appMode != null) {
			boolean nightMode = !app.getSettings().isLightContent();
			return appMode.getProfileColor(nightMode);
		}
		return super.getItemIconColor(app, item);
	}

	@Override
	protected View.OnClickListener getOnAddBtnClickListener(MapActivity activity, Adapter adapter) {
		return new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CreateEditActionDialog targetFragment = (CreateEditActionDialog) activity
						.getSupportFragmentManager().findFragmentByTag(CreateEditActionDialog.TAG);
				List<String> selectedProfilesKeys = new ArrayList<>(adapter.getItemsList());
				SelectMultipleProfilesBottomSheet.showInstance(activity, targetFragment,
						selectedProfilesKeys, selectedProfilesKeys, false);
			}
		};
	}

	private ApplicationMode getModeForKey(String key) {
		return ApplicationMode.valueOfStringKey(key, null);
	}

	@Override
	public boolean fillParams(@NonNull View root, @NonNull MapActivity mapActivity) {
		getParams().put(KEY_DIALOG, Boolean.toString(((SwitchCompat) root.findViewById(R.id.saveButton)).isChecked()));
		return super.fillParams(root, mapActivity);
	}

	@Override
	public String getItemIdFromObject(String object) {
		return object;
	}

	@Override
	protected void onItemsSelected(Context ctx, List<String> selectedItems) {
		Adapter adapter = getAdapter();
		if (adapter == null) {
			return;
		}
		for (String key : selectedItems) {
			ApplicationMode appMode = getModeForKey(key);
			if (appMode != null) {
				adapter.addItem(key, ctx);
			}
		}
	}

	@Override
	public String getActionText(@NonNull OsmandApplication app) {
		return getName(app);
	}
}
