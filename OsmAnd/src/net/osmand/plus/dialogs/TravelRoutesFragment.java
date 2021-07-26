package net.osmand.plus.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.CommonPreference;
import net.osmand.plus.settings.backend.OsmandSettings;

import java.util.Set;

import static net.osmand.plus.dialogs.ConfigureMapMenu.TRAVEL_ROUTES;
import static net.osmand.render.RenderingRuleStorageProperties.UI_CATEGORY_ROUTES;

public class TravelRoutesFragment extends BaseOsmAndFragment {

	public static final String TAG = TravelRoutesFragment.class.getSimpleName();

	private OsmandApplication app;
	private OsmandSettings settings;

	private Set<String> routesTypes;

	private boolean nightMode;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		settings = app.getSettings();
		nightMode = app.getDaynightHelper().isNightModeForMapControls();
		routesTypes = app.getResourceManager().searchSubCategoriesByCategoryName(UI_CATEGORY_ROUTES);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		MapActivity mapActivity = (MapActivity) requireMyActivity();
		LayoutInflater themedInflater = UiUtilities.getInflater(mapActivity, nightMode);
		View view = themedInflater.inflate(R.layout.travel_routes_fragment, container, false);

		setupPreferenceHeader(view);
		setupTypesCard(view);
		setupBottomEmptySpace(view);

		return view;
	}

	private void setupPreferenceHeader(@NonNull View view) {
		View container = view.findViewById(R.id.preference_container);
		CommonPreference<Boolean> pref = settings.getCustomRenderBooleanProperty(TRAVEL_ROUTES);
		setupPreferenceItem(container, pref, getString(R.string.travel_routes));
	}

	private void setupTypesCard(@NonNull View view) {
		ViewGroup container = view.findViewById(R.id.items_container);
		LayoutInflater inflater = UiUtilities.getInflater(view.getContext(), nightMode);
		for (String type : routesTypes) {
			CommonPreference<Boolean> pref = settings.getCustomRenderBooleanProperty(TRAVEL_ROUTES + type);
			View itemView = inflater.inflate(R.layout.list_item_icon_and_menu, null, false);
			setupPreferenceItem(itemView, pref, type);
			container.addView(itemView);
		}
	}

	private void setupPreferenceItem(View itemView, CommonPreference<Boolean> pref, String name) {
		TextView title = itemView.findViewById(R.id.title);
		ImageView icon = itemView.findViewById(R.id.icon);
		TextView description = itemView.findViewById(R.id.description);

		ApplicationMode mode = settings.getApplicationMode();
		int selectedColor = mode.getProfileColor(nightMode);
		int disabledColor = AndroidUtils.getColorFromAttr(itemView.getContext(), R.attr.default_icon_color);

		title.setText(name);
		icon.setImageDrawable(getPaintedContentIcon(mode.getIconRes(), pref.get() ? selectedColor : disabledColor));
		description.setText(pref.get() ? R.string.shared_string_enabled : R.string.shared_string_disabled);

		CompoundButton button = itemView.findViewById(R.id.toggle_item);
		button.setClickable(false);
		button.setFocusable(false);
		button.setChecked(pref.get());
		UiUtilities.setupCompoundButton(nightMode, selectedColor, button);

		itemView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				pref.set(!pref.get());
				setupPreferenceItem(itemView, pref, name);

				MapActivity mapActivity = (MapActivity) getMyActivity();
				if (mapActivity != null) {
					mapActivity.refreshMapComplete();
					mapActivity.getMapLayers().updateLayers(mapActivity.getMapView());
				}
			}
		});
		AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.divider), false);
	}

	private void setupBottomEmptySpace(@NonNull View view) {
		View bottomView = view.findViewById(R.id.bottom_empty_space);
		int height = AndroidUtils.getScreenHeight(requireActivity()) - getResources().getDimensionPixelSize(R.dimen.dashboard_map_top_padding);
		ViewGroup.LayoutParams params = bottomView.getLayoutParams();
		params.height = height;
		bottomView.setLayoutParams(params);
	}
}
