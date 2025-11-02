package net.osmand.plus.configmap;

import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.CONFIGURE_MAP;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.dialog.interfaces.dialog.IDialog;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.DayNightMode;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.alert.PreferenceScreenFactory;
import net.osmand.plus.widgets.multistatetoggle.IconToggleButton;
import net.osmand.plus.widgets.multistatetoggle.IconToggleButton.IconRadioItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import de.KnollFrank.lib.settingssearch.client.searchDatabaseConfig.InitializePreferenceFragmentWithFragmentBeforeOnCreate;

public class MapModeFragment extends ConfigureMapOptionFragment implements IDialog {

	private MapModeController controller;
	private IconToggleButton toggleButton;

	public static MapModeFragment createInstanceAndRegisterMapModeController(final OsmandApplication app,
																			 final ApplicationMode appMode) {
		MapModeController.registerNewInstance(app);
		final MapModeFragment mapModeFragment = new MapModeFragment();
		mapModeFragment.setArguments(BaseSettingsFragment.buildArguments(appMode));
		return mapModeFragment;
	}

	public ApplicationMode getAppMode() {
		return ApplicationMode.valueOfStringKey(requireArguments().getString(BaseSettingsFragment.APP_MODE_KEY), null);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		controller =
				MapModeController
						.findRegisteredInstance(app)
						.orElse(null);
		if (controller != null) {
			controller.registerDialog(this);
		} else {
			dismiss();
		}
		MapActivity activity = requireMapActivity();
		activity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				activity.getSupportFragmentManager().popBackStack();
				activity.getDashboard().setDashboardVisibility(true, CONFIGURE_MAP, false);
			}
		});
	}

	@Nullable
	@Override
	protected String getToolbarTitle() {
		return getString(R.string.map_mode);
	}

	@Override
	protected void setupMainContent(@NonNull ViewGroup container) {
		View view = inflate(R.layout.fragment_map_mode, container);
		setupToggleButton(view);
		updateContent(view);
		container.addView(view);
	}

	private void setupToggleButton(@NonNull View view) {
		List<IconRadioItem> items = new ArrayList<>();
		for (DayNightMode mode : getMapModes()) {
			items.add(createRadioItem(mode));
		}
		LinearLayout container = view.findViewById(R.id.custom_radio_buttons);
		toggleButton = new IconToggleButton(app, container, nightMode);
		toggleButton.setItems(items);
		toggleButton.setSelectedItemByTag(controller.getMapTheme());
	}

	protected List<DayNightMode> getMapModes() {
		return List.of(DayNightMode.values());
	}

	@NonNull
	private IconRadioItem createRadioItem(@NonNull DayNightMode mode) {
		IconRadioItem radioItem = new IconRadioItem(mode.getDefaultIcon());
		radioItem.setSelectedIconId(mode.getSelectedIcon())
				.setTag(mode)
				.setContentDescription(mode.toHumanString(app))
				.setOnClickListener((item, v) -> {
					if (controller.askSelectMapTheme((DayNightMode) item.requireTag())) {
						updateAfterThemeSelected();
					}
					return false;
				});
		return radioItem;
	}

	private void updateAfterThemeSelected() {
		toggleButton.setSelectedItemByTag(controller.getMapTheme());
		askUpdateContent();
		updateApplyButton(true);
		refreshMap();
	}

	private void askUpdateContent() {
		View view = getView();
		if (view != null) {
			updateContent(view);
		}
	}

	private void updateContent(@NonNull View view) {
		updateHeaderSummary(view);
		updateDescription(view);
	}

	private void updateHeaderSummary(@NonNull View view) {
		TextView tvSummary = view.findViewById(R.id.summary);
		tvSummary.setText(controller.getHeaderSummary());
	}

	private void updateDescription(@NonNull View view) {
		TextView tvDescription = view.findViewById(R.id.description);
		tvDescription.setText(controller.getDescription());

		TextView tvSecondaryDescription = view.findViewById(R.id.secondary_description);
		String secondaryDescription = controller.getSecondaryDescription();
		boolean hasSecondaryDescription = secondaryDescription != null;
		if (hasSecondaryDescription) {
			tvSecondaryDescription.setText(secondaryDescription);
		}
		AndroidUiHelper.updateVisibility(tvSecondaryDescription, hasSecondaryDescription);
	}

	@Override
	protected void resetToDefault() {
		controller.onResetToDefault();
		updateAfterThemeSelected();
	}

	@Override
	protected void applyChanges() {
		controller.onApplyChanges();
	}

	@Override
	protected void refreshMap() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.refreshMapComplete();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		controller.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		controller.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		controller.finishProcessIfNeeded(getActivity());
	}

	public void show(final MapActivity mapActivity, final boolean registerMapModeController) {
		if (registerMapModeController) {
			MapModeController.registerNewInstance(mapActivity.getMyApplication());
		}
		show(mapActivity.getSupportFragmentManager());
	}

	private void show(final FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, this, TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss();
		}
	}

	public static class MapModeFragmentProxy extends PreferenceFragmentCompat implements InitializePreferenceFragmentWithFragmentBeforeOnCreate<MapModeFragment> {

		private MapModeFragment mapModeFragment;

		@Override
		public void initializePreferenceFragmentWithFragmentBeforeOnCreate(final MapModeFragment mapModeFragment) {
			this.mapModeFragment = mapModeFragment;
			setArguments(mapModeFragment.getArguments());
		}

		public MapModeFragment getPrincipal() {
			return mapModeFragment;
		}

		@Override
		public void onCreatePreferences(@Nullable final Bundle savedInstanceState, @Nullable final String rootKey) {
			setPreferenceScreen(asPreferenceScreen(asPreferences(mapModeFragment.getMapModes())));
		}

		private Collection<Preference> asPreferences(final List<DayNightMode> dayNightModes) {
			return dayNightModes
					.stream()
					.map(this::asPreference)
					.collect(Collectors.toUnmodifiableList());
		}

		private Preference asPreference(final DayNightMode dayNightMode) {
			final Preference preference = new Preference(requireContext());
			preference.setKey(dayNightMode.name());
			preference.setTitle(dayNightMode.getKey());
			preference.setSummary(dayNightMode.getSummaryRes());
			return preference;
		}

		private PreferenceScreen asPreferenceScreen(final Collection<Preference> preferences) {
			return new PreferenceScreenFactory(this).asPreferenceScreen(preferences);
		}
	}
}
