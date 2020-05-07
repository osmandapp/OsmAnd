package net.osmand.plus.quickaction.actions;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;
import androidx.core.util.Pair;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.CallbackWithObject;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.profiles.SelectMultipleProfileBottomSheet;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.quickaction.SwitchableAction;
import net.osmand.plus.views.MapQuickActionLayer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static net.osmand.plus.dialogs.SelectMapViewQuickActionsBottomSheet.PROFILE_DIALOG_TYPE;

public class SwitchProfileAction extends SwitchableAction<Pair<String, String>> {

	private final static String KEY_PROFILES = "profiles";

	public static final QuickActionType TYPE = new QuickActionType(32,
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
	protected String getTitle(List<Pair<String, String>> filters) {
		List<String> profileNames = new ArrayList<>();
		for (Pair<String, String> p : filters) {
			profileNames.add(p.second);
		}
		return TextUtils.join(", ", profileNames);
	}

	@Override
	protected void saveListToParams(List<Pair<String, String>> list) {
		getParams().put(getListKey(), new Gson().toJson(list));
	}

	@Override
	public List<Pair<String, String>> loadListFromParams() {
		String json = getParams().get(getListKey());

		if (json == null || json.isEmpty()) return new ArrayList<>();

		Type listType = new TypeToken<ArrayList<Pair<String, String>>>() {
		}.getType();

		List<Pair<String, String>> list = new Gson().fromJson(json, listType);

		Iterator<Pair<String, String>> it = list.iterator();
		while (it.hasNext()) {
			ApplicationMode appMode = getModeForKey(it.next().first);
			if (appMode == null) {
				it.remove();
			}
		}

		return list;
	}

	@Override
	public void execute(MapActivity activity) {
		OsmandSettings settings = activity.getMyApplication().getSettings();
		List<Pair<String, String>> profiles = loadListFromParams();

		if (profiles.size() == 0) {
			Toast.makeText(activity, activity.getString(R.string.profiles_for_action_not_found),
					Toast.LENGTH_SHORT).show();
			return;
		}

		boolean showDialog = Boolean.valueOf(getParams().get(KEY_DIALOG));
		if (showDialog) {
			showChooseDialog(activity.getSupportFragmentManager(), PROFILE_DIALOG_TYPE);
			return;
		}

		int index = -1;
		final String currentSource = settings.getApplicationMode().getStringKey();

		for (int idx = 0; idx < profiles.size(); idx++) {
			if (currentSource.equals(profiles.get(idx).first)) {
				index = idx;
				break;
			}
		}

		String nextSource = profiles.get(0).first;

		if (index >= 0 && index + 1 < profiles.size()) {
			nextSource = profiles.get(index + 1).first;
		}
		executeWithParams(activity, nextSource);

		super.execute(activity);
	}

	@Override
	public void executeWithParams(MapActivity activity, String params) {
		OsmandApplication app = activity.getMyApplication();
		OsmandSettings settings = app.getSettings();

		ApplicationMode appMode = getModeForKey(params);
		if (appMode != null) {
			settings.APPLICATION_MODE.set(appMode);

			app.getQuickActionRegistry().setQuickActionFabState(true);

			MapQuickActionLayer mil = activity.getMapLayers().getMapQuickActionLayer();
			if (mil != null) {
				mil.refreshLayer();
			}

			String message = String.format(activity.getString(
					R.string.application_profile_changed), appMode.toHumanString());
			Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public String getTranslatedItemName(Context context, String item) {
		return getModeForKey(item).toHumanString();
	}

	@Override
	protected String getItemName(Context context, Pair<String, String> item) {
		return item.second;
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
	protected int getItemIconRes(Context context, Pair<String, String> item) {
		ApplicationMode appMode = getModeForKey(item.first);
		if (appMode != null) {
			return appMode.getIconRes();
		}
		return super.getItemIconRes(context, item);
	}

	@Override
	protected int getItemIconColorRes(OsmandApplication app, Pair<String, String> item) {
		ApplicationMode appMode = getModeForKey(item.first);
		if (appMode != null) {
			boolean nightMode = !app.getSettings().isLightContent();
			return appMode.getIconColorInfo().getColor(nightMode);
		}
		return super.getItemIconColorRes(app, item);
	}

	@Override
	protected View.OnClickListener getOnAddBtnClickListener(final MapActivity activity, final Adapter adapter) {
		return new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				List<String> selectedProfilesKeys = new ArrayList<>();
				for (Pair<String, String> item : adapter.getItemsList()) {
					selectedProfilesKeys.add(item.first);
				}
				SelectMultipleProfileBottomSheet.showInstance(activity, selectedProfilesKeys,
						selectedProfilesKeys, false, new CallbackWithObject<List<String>>() {
							@Override
							public boolean processResult(List<String> result) {
								if (result == null || result.size() == 0) {
									return false;
								}
								for (String item : result) {
									ApplicationMode appMode = getModeForKey(item);
									if (appMode != null) {
										Pair<String, String> profile = new Pair<>(
												appMode.getStringKey(), appMode.toHumanString());
										adapter.addItem(profile, activity);
									}
								}
								return true;
							}
						});
			}
		};
	}

	private ApplicationMode getModeForKey(String key) {
		return ApplicationMode.valueOfStringKey(key, null);
	}

	@Override
	public boolean fillParams(View root, MapActivity activity) {
		getParams().put(KEY_DIALOG, Boolean.toString(((SwitchCompat) root.findViewById(R.id.saveButton)).isChecked()));
		return super.fillParams(root, activity);
	}

}
