package net.osmand.plus.widgets.alert;

import static net.osmand.plus.views.MapLayers.LAYER_INSTALL_MORE;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import net.osmand.plus.settings.fragments.search.PreferenceFragmentHandler;
import net.osmand.plus.settings.fragments.search.PreferenceFragmentHandlerProvider;

import java.util.Map;
import java.util.Optional;

// FK-FIXME: 1. navigate to InstallMapLayersDialogFragment (Settings -> Driving -> Configure map -> Map source... -> Add more...)
//           2. select "Top Yandex RU"
//           3. click Apply
//           Then "Top Yandex RU" appears in MapLayerSelectionDialogFragment (Settings -> Driving -> Configure map -> Map source...) but this new entry of MapLayerSelectionDialogFragment is not yet stored in the search database and hence can't be found via a search query.
//           Fix: invoke SearchPreferenceFragments.rebuildSearchDatabase() after user clicked the Apply button in step 3.
public class MapLayerSelectionDialogFragment extends SelectionDialogFragment {

	public final Optional<InstallMapLayersDialogFragment> installMapLayersDialogFragment;

	public MapLayerSelectionDialogFragment(final Optional<InstallMapLayersDialogFragment> installMapLayersDialogFragment,
										   final AlertDialog alertDialog,
										   final AlertDialogData alertDialogData,
										   final Map<String, CharSequence> itemByKey,
										   final SelectionDialogAdapter adapter) {
		super(alertDialog, alertDialogData, itemByKey, adapter);
		this.installMapLayersDialogFragment = installMapLayersDialogFragment;
	}

	public static class MapLayerSelectionDialogFragmentProxy extends SelectionDialogFragmentProxy<MapLayerSelectionDialogFragment> implements PreferenceFragmentHandlerProvider {

		@Override
		public Optional<PreferenceFragmentHandler> getPreferenceFragmentHandler(final Preference preference) {
			return LAYER_INSTALL_MORE.equals(preference.getKey()) ?
					Optional.of(createInstallMapLayersHandler()) :
					Optional.empty();
		}

		private PreferenceFragmentHandler createInstallMapLayersHandler() {
			return new PreferenceFragmentHandler() {

				@Override
				public Class<? extends PreferenceFragmentCompat> getClassOfPreferenceFragment() {
					return createPreferenceFragment().getClass();
				}

				@Override
				public PreferenceFragmentCompat createPreferenceFragment(final Context context, final Optional<Fragment> target) {
					return createPreferenceFragment();
				}

				private static InstallMapLayersDialogFragment.InstallMapLayersDialogFragmentProxy createPreferenceFragment() {
					return new InstallMapLayersDialogFragment.InstallMapLayersDialogFragmentProxy();
				}

				@Override
				public boolean showPreferenceFragment(final PreferenceFragmentCompat preferenceFragment) {
					return false;
				}
			};
		}
	}
}
