package net.osmand.plus.plugins.topoprint;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.InputType;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.SettingsScreenType;
import net.osmand.plus.settings.preferences.EditTextPreferenceEx;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;
import net.osmand.plus.utils.AndroidUtils;

/**
 * Plugin settings page, opened from "Configure map" -> "Print map". Contains
 * the map title, the preview-frame switch, paper size (with custom
 * dimensions), orientation, DPI, scale, projection options and the map
 * generation entry.
 */
public class TopoPrintSettingsFragment extends BaseSettingsFragment {

	private static final int OUTPUT_DIR_REQUEST = 9001;

	@Override
	protected void setupPreferences() {
		TopoPrintPlugin plugin = PluginsHelper.getPlugin(TopoPrintPlugin.class);
		if (plugin == null) {
			return;
		}
			setupPreviewPref(plugin);
		setupPaperSizePref(plugin);
		setupCustomPaperPrefs(plugin);
		setupOrientationPref(plugin);
		setupDpiPref(plugin);
		setupScalePref(plugin);
		setupProjectionPref(plugin);
		setupSymbolsScalePref(plugin);
		setupOutputDirPref(plugin);
		setupGeneratePref(plugin);
	}

	private void setupOutputDirPref(TopoPrintPlugin plugin) {
		// Same picker interaction as Media storage → Manually specified:
		// a system SAF directory chooser; the chosen tree is persisted and
		// translated to a filesystem path for the File-based writer.
		Preference outputDir = new Preference(requireContext());
		outputDir.setKey(plugin.OUTPUT_DIR.getId() + "_picker");
		outputDir.setTitle(R.string.topoprint_output_dir);
		outputDir.setSummary(outputDirSummary(plugin));
		addOnPreferencesScreen(outputDir);
	}

	private String outputDirSummary(@NonNull TopoPrintPlugin plugin) {
		// Always show the effective path — the default app-private folder
		// when no custom directory is configured.
		return plugin.getOutputDir().getAbsolutePath();
	}

	/**
	 * Label/text scale for the offscreen map render. Uses a list (not a
	 * free-form EditText): the settings screen opens EditTextPreference in a
	 * custom bottom sheet that requires OsmAnd's EditTextPreferenceEx class.
	 */
	private void setupSymbolsScalePref(TopoPrintPlugin plugin) {
		String[] scales = {"0.7", "0.85", "1.0", "1.3", "1.6", "2.0"};
		String[] entries = new String[scales.length];
		for (int i = 0; i < scales.length; i++) {
			entries[i] = scales[i] + "×" + ("1.0".equals(scales[i]) ? " (default)" : "");
		}
		addOnPreferencesScreen(createListPreferenceEx(plugin.SYMBOLS_SCALE.getId(),
				entries, scales, R.string.topoprint_symbols_scale, R.layout.preference_with_descr));
	}

	private void setupPreviewPref(TopoPrintPlugin plugin) {
		SwitchPreferenceEx preview = createSwitchPreferenceEx(plugin.PREVIEW.getId(),
				R.string.topoprint_show_preview, R.layout.preference_with_descr_dialog_and_switch);
		preview.setDescription(getString(R.string.topoprint_show_preview_descr));
		addOnPreferencesScreen(preview);
	}

	private void setupPaperSizePref(TopoPrintPlugin plugin) {
		PaperSize[] values = PaperSize.values();
		String[] entries = new String[values.length];
		String[] entryValues = new String[values.length];
		for (int i = 0; i < values.length; i++) {
			PaperSize size = values[i];
			entryValues[i] = size.name();
			if (size == PaperSize.CUSTOM) {
				entries[i] = getString(R.string.topoprint_custom_paper);
			} else {
				entries[i] = size.getTag() + " (" + trimNumber(size.getShortSideMm()) + " x "
						+ trimNumber(size.getLongSideMm()) + " mm)";
			}
		}
		addOnPreferencesScreen(createListPreferenceEx(plugin.PAPER_SIZE.getId(),
				entries, entryValues, R.string.topoprint_paper_size, R.layout.preference_with_descr));
	}

	private void setupCustomPaperPrefs(TopoPrintPlugin plugin) {
		addCustomDimPref(plugin, plugin.CUSTOM_PAPER_WIDTH, R.string.topoprint_custom_width);
		addCustomDimPref(plugin, plugin.CUSTOM_PAPER_HEIGHT, R.string.topoprint_custom_height);
	}

	private void addCustomDimPref(TopoPrintPlugin plugin, CommonPreference<String> pref, int titleId) {
		// OsmAnd's EditTextPreferenceBottomSheet requires the Ex subclass;
		// the plain androidx EditTextPreference crashes with a cast error.
		// setDialogTitle is mandatory: androidx's getDialogTitle() does NOT
		// fall back to the title, and the bottom sheet NPEs on a null one.
		EditTextPreferenceEx dim = new EditTextPreferenceEx(requireContext());
		dim.setKey(pref.getId());
		dim.setTitle(titleId);
		dim.setDialogTitle(titleId);
		dim.setText(pref.get());
		dim.setSummary(pref.get() + " mm");
		dim.setOnBindEditTextListener(editText ->
				editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL));
		dim.setOnPreferenceChangeListener((preference, newValue) -> {
			String text = newValue == null ? "" : String.valueOf(newValue).trim();
			double value;
			try {
				value = Double.parseDouble(text);
			} catch (NumberFormatException e) {
				return false;
			}
			if (value < 50.0 || value > 3000.0) {
				return false;
			}
			pref.set(text);
			dim.setSummary(text + " mm");
			updateGenerateSummary(plugin);
			return true;
		});
		addOnPreferencesScreen(dim);
	}

	private void setupOrientationPref(TopoPrintPlugin plugin) {
		PrintOrientation[] values = PrintOrientation.values();
		String[] entries = new String[values.length];
		String[] entryValues = new String[values.length];
		for (int i = 0; i < values.length; i++) {
			entries[i] = getString(values[i] == PrintOrientation.PORTRAIT
					? R.string.topoprint_portrait : R.string.topoprint_landscape);
			entryValues[i] = values[i].name();
		}
		addOnPreferencesScreen(createListPreferenceEx(plugin.ORIENTATION.getId(),
				entries, entryValues, R.string.topoprint_orientation, R.layout.preference_with_descr));
	}

	private void setupDpiPref(TopoPrintPlugin plugin) {
		int[] dpiValues = PrintParameters.SUPPORTED_DPI;
		String[] entries = new String[dpiValues.length];
		Integer[] entryValues = new Integer[dpiValues.length];
		for (int i = 0; i < dpiValues.length; i++) {
			entries[i] = getString(R.string.ltr_or_rtl_combine_via_space, String.valueOf(dpiValues[i]), "DPI");
			entryValues[i] = dpiValues[i];
		}
		addOnPreferencesScreen(createListPreferenceEx(plugin.DPI.getId(),
				entries, entryValues, R.string.topoprint_dpi, R.layout.preference_with_descr));
	}

	private void setupScalePref(TopoPrintPlugin plugin) {
		int[] scaleValues = PrintParameters.SUPPORTED_SCALES;
		String[] entries = new String[scaleValues.length];
		Integer[] entryValues = new Integer[scaleValues.length];
		for (int i = 0; i < scaleValues.length; i++) {
			entries[i] = getString(R.string.ltr_or_rtl_combine_via_colon, "1", String.valueOf(scaleValues[i]));
			entryValues[i] = scaleValues[i];
		}
		addOnPreferencesScreen(createListPreferenceEx(plugin.SCALE_DENOMINATOR.getId(),
				entries, entryValues, R.string.topoprint_scale_denominator, R.layout.preference_with_descr));
	}

	private void setupProjectionPref(TopoPrintPlugin plugin) {
		MapProjection[] values = MapProjection.values();
		String[] entries = new String[values.length];
		String[] entryValues = new String[values.length];
		for (int i = 0; i < values.length; i++) {
			entries[i] = getString(values[i] == MapProjection.SPHERICAL
					? R.string.topoprint_projection_spherical : R.string.topoprint_projection_mercator);
			entryValues[i] = values[i].name();
		}
		addOnPreferencesScreen(createListPreferenceEx(plugin.PROJECTION.getId(),
				entries, entryValues, R.string.topoprint_projection, R.layout.preference_with_descr));
	}

	private void setupGeneratePref(TopoPrintPlugin plugin) {
		Preference generate = new Preference(requireContext());
		generate.setKey("generate_print_map");
		generate.setLayoutResource(R.layout.preference_with_descr);
		generate.setTitle(R.string.topoprint_generate);
		generate.setSummary(plugin.getPrintParametersSummary());
		generate.setPersistent(false);
		addOnPreferencesScreen(generate);
	}

	private void updateGenerateSummary(TopoPrintPlugin plugin) {
		Preference generate = findPreference("generate_print_map");
		if (generate != null) {
			generate.setSummary(plugin.getPrintParametersSummary());
		}
	}

	private static String trimNumber(double value) {
		String formatted = String.valueOf(Math.round(value * 10.0) / 10.0);
		return formatted.endsWith(".0") ? formatted.substring(0, formatted.length() - 2) : formatted;
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		TopoPrintPlugin plugin = PluginsHelper.getPlugin(TopoPrintPlugin.class);
		if (plugin != null && (plugin.OUTPUT_DIR.getId() + "_picker").equals(preference.getKey())) {
			Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
					| Intent.FLAG_GRANT_WRITE_URI_PERMISSION
					| Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
					| Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
			AndroidUtils.startActivityForResultIfSafe(this, intent, OUTPUT_DIR_REQUEST);
			return true;
		}
		if ("generate_print_map".equals(preference.getKey())) {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null && plugin != null) {
				// Leave the settings screen first: the render must run with the
				// MapActivity in the foreground (the GL renderer handover
				// requires the main renderer to be initialized, and the map is
				// frozen while the offscreen render is in progress).
				plugin.showGenerateDialog(mapActivity);
				mapActivity.onBackPressed();
				return true;
			}
		}
		return super.onPreferenceClick(preference);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if (requestCode == OUTPUT_DIR_REQUEST) {
			TopoPrintPlugin plugin = PluginsHelper.getPlugin(TopoPrintPlugin.class);
			if (plugin == null || resultCode != Activity.RESULT_OK || data == null
					|| data.getData() == null) {
				return;
			}
			Uri treeUri = data.getData();
			int requiredFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
					| Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
			// Inlined AndroidUtils.takePersistableUriPermission (the helper
			// does not exist on the 5.3 codebase): persist only when the
			// picker result actually grants the required mode flags.
			int requiredModeFlags = requiredFlags & (Intent.FLAG_GRANT_READ_URI_PERMISSION
					| Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
			if ((data.getFlags() & requiredModeFlags) != requiredModeFlags) {
				app.showShortToastMessage(R.string.folder_access_denied);
				return;
			}
			try {
				app.getContentResolver()
						.takePersistableUriPermission(treeUri, requiredModeFlags);
			} catch (RuntimeException e) {
				app.showShortToastMessage(R.string.folder_access_denied);
				return;
			}
			String path = treeUriToPrimaryPath(treeUri);
			if (path == null) {
				// Only the emulated primary volume maps to a filesystem path
				// the File-based writer can use.
				app.showShortToastMessage(R.string.folder_access_denied);
				return;
			}
			plugin.OUTPUT_DIR.set(path);
			Preference pref = findPreference(plugin.OUTPUT_DIR.getId() + "_picker");
			if (pref != null) {
				pref.setSummary(outputDirSummary(plugin));
			}
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * Translates a SAF tree URI of the emulated primary volume
	 * ({@code content://…/tree/primary:TopoPrint}) to a filesystem path
	 * ({@code /storage/emulated/0/TopoPrint}). Secondary volumes have no
	 * portable filesystem mapping and yield null.
	 */
	@Nullable
	private static String treeUriToPrimaryPath(@NonNull Uri treeUri) {
		String segment = treeUri.getLastPathSegment();
		if (segment == null) {
			return null;
		}
		int split = segment.indexOf(':');
		if (split < 0 || !"primary".equals(segment.substring(0, split))) {
			return null;
		}
		String dir = segment.substring(split + 1);
		return "/storage/emulated/0" + (dir.isEmpty() ? "" : "/" + dir);
	}

	public static boolean showInstance(@NonNull MapActivity mapActivity) {
		return BaseSettingsFragment.showInstance(mapActivity, SettingsScreenType.TOPOPRINT_SETTINGS);
	}
}
