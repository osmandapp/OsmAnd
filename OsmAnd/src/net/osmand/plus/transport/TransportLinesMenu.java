package net.osmand.plus.transport;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import net.osmand.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dialogs.ConfigureMapMenu;
import net.osmand.render.RenderingRuleProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransportLinesMenu {

	public static final String RENDERING_CATEGORY_TRANSPORT = "transport";

	public static final String PT_TRANSPORT_STOPS = "transportStops";
	public static final String PT_PUBLIC_TRANSPORT_MODE = "publicTransportMode";
	public static final String PT_TRAM_TRAIN_ROUTES = "tramTrainRoutes";
	public static final String PT_SUBWAY_MODE = "subwayMode";

	public static void toggleTransportLines(@NonNull MapActivity mapActivity, boolean enable,
	                                        @Nullable CallbackWithObject<Boolean> callback) {
		OsmandApplication app = mapActivity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		if (enable) {
			List<String> enabledTransportIds = settings.DISPLAYED_TRANSPORT_SETTINGS.getStringsList();
			if (enabledTransportIds != null) {
				for (CommonPreference<Boolean> pref : getTransportPrefs(app, null)) {
					boolean selected = enabledTransportIds.contains(pref.getId());
					pref.set(selected);
				}
				refreshMap(mapActivity);
			} else {
				showTransportsDialog(mapActivity, callback);
			}
		} else {
			for (CommonPreference<Boolean> pref : getTransportPrefs(app, null)) {
				pref.set(false);
			}
			refreshMap(mapActivity);
			if (callback != null) {
				callback.processResult(false);
			}
		}
	}

	public static void showTransportsDialog(@NonNull final MapActivity mapActivity,
	                                        @Nullable final CallbackWithObject<Boolean> callback) {
		final OsmandApplication app = mapActivity.getMyApplication();
		final OsmandSettings settings = app.getSettings();
		final ApplicationMode appMode = settings.getApplicationMode();

		final List<String> enabledTransportIds = app.getSettings().DISPLAYED_TRANSPORT_SETTINGS.getStringsList();
		final List<RenderingRuleProperty> transportRules = getTransportRules(app);
		final List<CommonPreference<Boolean>> transportPrefs = getTransportPrefs(app, transportRules);

		final boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		final Context themedCtx = UiUtilities.getThemedContext(mapActivity, nightMode);
		final int profileColor = appMode.getProfileColor(nightMode);

		final AlertDialog.Builder b = new AlertDialog.Builder(themedCtx);
		b.setTitle(themedCtx.getString(R.string.rendering_category_transport));

		final boolean[] checkedItems = new boolean[transportPrefs.size()];
		for (int i = 0; i < transportPrefs.size(); i++) {
			boolean selected = enabledTransportIds != null
					&& enabledTransportIds.contains(transportPrefs.get(i).getId());
			checkedItems[i] = selected;
		}

		final int[] iconIds = new int[transportPrefs.size()];
		final String[] vals = new String[transportRules.size()];

		final Map<String, Integer> transportIcons = new HashMap<>();
		transportIcons.put(PT_TRANSPORT_STOPS, R.drawable.ic_action_transport_stop);
		transportIcons.put(PT_PUBLIC_TRANSPORT_MODE, R.drawable.ic_action_transport_bus);
		transportIcons.put(PT_TRAM_TRAIN_ROUTES, R.drawable.ic_action_transport_tram);
		transportIcons.put(PT_SUBWAY_MODE, R.drawable.ic_action_transport_subway);

		for (int i = 0; i < transportRules.size(); i++) {
			RenderingRuleProperty p = transportRules.get(i);
			String attrName = p.getAttrName();
			String propertyName = AndroidUtils.getRenderingStringPropertyName(themedCtx, attrName, p.getName());
			vals[i] = propertyName;
			Integer iconId = transportIcons.get(attrName);
			if (iconId != null) {
				iconIds[i] = iconId;
			} else {
				iconIds[i] = R.drawable.ic_action_transport_bus;
			}
		}

		final ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(themedCtx,
				R.layout.popup_list_item_icon24_and_menu, R.id.title, vals) {
			@NonNull
			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				View v = super.getView(position, convertView, parent);
				final ImageView icon = (ImageView) v.findViewById(R.id.icon);
				if (checkedItems[position]) {
					icon.setImageDrawable(app.getUIUtilities().getPaintedIcon(iconIds[position], profileColor));
				} else {
					icon.setImageDrawable(app.getUIUtilities().getThemedIcon(iconIds[position]));
				}
				v.findViewById(R.id.divider).setVisibility(View.GONE);
				v.findViewById(R.id.description).setVisibility(View.GONE);
				v.findViewById(R.id.secondary_icon).setVisibility(View.GONE);
				final SwitchCompat check = (SwitchCompat) v.findViewById(R.id.toggle_item);
				check.setOnCheckedChangeListener(null);
				check.setChecked(checkedItems[position]);
				check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						checkedItems[position] = isChecked;
						if (checkedItems[position]) {
							icon.setImageDrawable(app.getUIUtilities().getPaintedIcon(iconIds[position], profileColor));
						} else {
							icon.setImageDrawable(app.getUIUtilities().getThemedIcon(iconIds[position]));
						}
					}
				});
				UiUtilities.setupCompoundButton(nightMode, profileColor, check);
				return v;
			}
		};

		final ListView listView = new ListView(themedCtx);
		listView.setDivider(null);
		listView.setClickable(true);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new ListView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				checkedItems[position] = !checkedItems[position];
				adapter.notifyDataSetChanged();
			}
		});
		b.setView(listView);

		b.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				if (callback != null) {
					callback.processResult(isShowLines(app));
				}
			}
		});

		b.setNegativeButton(R.string.shared_string_cancel, null);
		b.setPositiveButton(R.string.shared_string_apply, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				List<String> transportIdsToSave = new ArrayList<>();
				for (int i = 0; i < transportPrefs.size(); i++) {
					CommonPreference<Boolean> pref = transportPrefs.get(i);
					boolean selected = checkedItems[i];
					if (selected) {
						transportIdsToSave.add(pref.getId());
					}
					pref.set(selected);
				}
				settings.DISPLAYED_TRANSPORT_SETTINGS.setStringsListForProfile(appMode,
						transportIdsToSave.size() > 0 ? transportIdsToSave : null);
				if (callback != null) {
					callback.processResult(transportIdsToSave.size() > 0);
				}
				refreshMap(mapActivity);
			}
		});
		b.show();
	}

	public static List<RenderingRuleProperty> getTransportRules(OsmandApplication app) {
		List<RenderingRuleProperty> transportRules = new ArrayList<>();
		for (RenderingRuleProperty property : ConfigureMapMenu.getCustomRules(app)) {
			if (RENDERING_CATEGORY_TRANSPORT.equals(property.getCategory()) && property.isBoolean()) {
				transportRules.add(property);
			}
		}
		return transportRules;
	}

	public static List<CommonPreference<Boolean>> getTransportPrefs(@NonNull OsmandApplication app,
	                                                                List<RenderingRuleProperty> transportRules) {
		if (transportRules == null) {
			transportRules = getTransportRules(app);
		}
		List<CommonPreference<Boolean>> transportPrefs = new ArrayList<>();
		for (RenderingRuleProperty property : transportRules) {
			final CommonPreference<Boolean> pref = app.getSettings()
					.getCustomRenderBooleanProperty(property.getAttrName());
			transportPrefs.add(pref);
		}
		return transportPrefs;
	}

	private static void refreshMap(MapActivity mapActivity) {
		mapActivity.refreshMapComplete();
		mapActivity.getMapLayers().updateLayers(mapActivity.getMapView());
	}

	public static boolean isShowLines(@NonNull OsmandApplication app) {
		ApplicationMode appMode = app.getSettings().getApplicationMode();
		for (CommonPreference<Boolean> pref : getTransportPrefs(app, null)) {
			if (pref.getModeValue(appMode)) {
				return true;
			}
		}
		return false;
	}
}
