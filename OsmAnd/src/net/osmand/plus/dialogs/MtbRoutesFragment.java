package net.osmand.plus.dialogs;

import static net.osmand.plus.configmap.ConfigureMapMenu.SHOW_MTB_ROUTES;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.enums.MtbClassification;
import net.osmand.plus.transport.TransportLinesFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

import java.util.HashMap;

public class MtbRoutesFragment extends BaseOsmAndFragment {
	public static final String TAG = MtbRoutesFragment.class.getSimpleName();

	private OsmandApplication app;
	private OsmandSettings settings;
	private boolean nightMode;

	private CommonPreference<Boolean> mtbRoutePreference;
	private final HashMap<MtbClassification, AppCompatRadioButton> radioButtonHashMap = new HashMap<>();

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		settings = app.getSettings();
		nightMode = app.getDaynightHelper().isNightModeForMapControls();
		mtbRoutePreference = settings.getCustomRenderBooleanProperty(SHOW_MTB_ROUTES);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		MapActivity mapActivity = (MapActivity) requireMyActivity();
		LayoutInflater themedInflater = UiUtilities.getInflater(mapActivity, nightMode);
		View view = themedInflater.inflate(R.layout.fragment_mtb_routes, container, false);

		setupHeader(view);
		updateScreenMode(view, mtbRoutePreference.get());
		setupClassifications(view);
		setupClassificationPreference(mtbRoutePreference.get());

		return view;
	}

	@NonNull
	public static MtbClassification getSelectedClassification(OsmandSettings settings) {
		MtbClassification selectedClassification = null;
		for (MtbClassification mtbClassification : MtbClassification.values()) {
			if (settings.getCustomRenderBooleanProperty(mtbClassification.attrName).get()) {
				selectedClassification = mtbClassification;
			}
		}
		return selectedClassification != null ? selectedClassification : MtbClassification.SCALE;
	}

	private void setupClassificationPreference(boolean mtbRoutesEnabled) {
		updateClassificationPreferences(mtbRoutesEnabled ? getSelectedClassification(settings) : null);
	}

	private void setupHeader(@NonNull View view) {
		TransportLinesFragment.setupButton(
				view.findViewById(R.id.main_toggle),
				R.drawable.ic_action_mountain_bike,
				getString(R.string.rendering_attr_showMtbRoutes_name),
				mtbRoutePreference.get(),
				false,
				v -> {
					boolean enabled = !mtbRoutePreference.get();
					setupClassificationPreference(enabled);
					mtbRoutePreference.set(enabled);
					updateScreenMode(view, enabled);
					refreshMap();
				});
	}

	private void setupClassifications(@NonNull View view) {
		ViewGroup container = view.findViewById(R.id.classification_properties);
		LayoutInflater inflater = UiUtilities.getInflater(getContext(), nightMode);
		radioButtonHashMap.clear();

		for (MtbClassification classification : MtbClassification.values()) {
			boolean hasDivider = classification.ordinal() != MtbClassification.values().length - 1;
			View propertyView = createRadioButton(classification, inflater, container, hasDivider);
			container.addView(propertyView);
		}
	}

	private View createRadioButton(@NonNull MtbClassification classification, @NonNull LayoutInflater inflater, @Nullable ViewGroup container, boolean hasDivider) {
		View view = inflater.inflate(R.layout.item_with_radiobutton_and_descr, container, false);

		TextView title = view.findViewById(R.id.title);
		TextView description = view.findViewById(R.id.description);
		View divider = view.findViewById(R.id.divider);
		AppCompatRadioButton radioButton = view.findViewById(R.id.compound_button);
		radioButtonHashMap.put(classification, radioButton);

		title.setText(getString(classification.nameId));
		if (classification.descriptionId != null) {
			AndroidUiHelper.updateVisibility(description, true);
			description.setText(getString(classification.descriptionId));
		}
		AndroidUiHelper.updateVisibility(divider, hasDivider);

		View button = view.findViewById(R.id.button);
		button.setOnClickListener(v -> updateClassificationPreferences(classification));

		Drawable background = UiUtilities.getColoredSelectableDrawable(app, settings.getApplicationMode().getProfileColor(nightMode), 0.3f);
		AndroidUtils.setBackground(button, background);
		return view;
	}

	private void updateClassificationPreferences(@Nullable MtbClassification selectedClassification) {
		for (MtbClassification mtbClassification : MtbClassification.values()) {
			settings.getCustomRenderBooleanProperty(mtbClassification.attrName).set(mtbClassification.equals(selectedClassification));
			radioButtonHashMap.get(mtbClassification).setChecked(mtbClassification.equals(selectedClassification));
		}
	}

	private void updateScreenMode(@NonNull View view, boolean enabled) {
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.empty_screen), !enabled);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.normal_screen), enabled);
	}

	private void refreshMap() {
		MapActivity mapActivity = (MapActivity) getMyActivity();
		if (mapActivity != null) {
			mapActivity.refreshMapComplete();
			mapActivity.updateLayers();
		}
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			fragmentManager.beginTransaction()
					.replace(R.id.content, new MtbRoutesFragment(), TAG)
					.commitAllowingStateLoss();
		}
	}
}
