package net.osmand.plus.plugins.topoprint;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_TOPOPRINT_PREVIEW;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_TOPOPRINT;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.topoprint.compose.UtmCoordinate;
import net.osmand.plus.plugins.topoprint.compose.UtmMath;
import net.osmand.plus.plugins.topoprint.export.PrintExportHelper;
import net.osmand.plus.plugins.topoprint.render.PrintMapTask;
import net.osmand.plus.plugins.topoprint.views.PrintPreviewLayer;
import net.osmand.plus.plugins.topoprint.widgets.PrintMapButton;
import net.osmand.plus.plugins.topoprint.widgets.PrintMapButtonState;
import net.osmand.plus.views.controls.MapHudLayout;
import net.osmand.plus.views.layers.MapControlsLayer;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.settings.fragments.SettingsScreenType;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.render.RenderingRuleProperty;

import java.io.File;
import java.util.List;

/**
 * Plugin that previews a printable map extent on the map, renders the selected
 * area off-screen with the current OsmAnd rendering style, and composes a
 * print-ready map (UTM grid, map scale, declination) exported as SVG and PNG.
 */
public class TopoPrintPlugin extends OsmandPlugin {

	public static final int DEFAULT_DPI = 300;
	public static final int DEFAULT_SCALE_DENOMINATOR = 25000;

	public final CommonPreference<String> CUSTOM_PAPER_WIDTH;
	public final CommonPreference<String> CUSTOM_PAPER_HEIGHT;
	public final CommonPreference<String> PAPER_SIZE;
	public final CommonPreference<String> ORIENTATION;
	public final CommonPreference<Integer> DPI;
	public final CommonPreference<Integer> SCALE_DENOMINATOR;
	public final CommonPreference<String> PROJECTION;
	public final CommonPreference<String> SYMBOLS_SCALE;
	public final CommonPreference<String> OUTPUT_DIR;
	public final CommonPreference<Boolean> PREVIEW;

	private PrintPreviewLayer previewLayer;
	private PrintMapButtonState printMapButtonState;
	private PrintMapButton printMapButton;
	private MapActivity printMapButtonActivity;

	public TopoPrintPlugin(OsmandApplication app) {
		super(app);

			CUSTOM_PAPER_WIDTH = registerStringPreference("topo_print_custom_paper_width", "210").makeGlobal().cache();
		CUSTOM_PAPER_HEIGHT = registerStringPreference("topo_print_custom_paper_height", "297").makeGlobal().cache();
		PAPER_SIZE = registerStringPreference("topo_print_paper_size", PaperSize.A4.name()).makeGlobal().cache();
		ORIENTATION = registerStringPreference("topo_print_orientation", PrintOrientation.PORTRAIT.name()).makeGlobal().cache();
		DPI = registerIntPreference("topo_print_dpi", DEFAULT_DPI).makeGlobal().cache();
		SCALE_DENOMINATOR = registerIntPreference("topo_print_scale_denominator", DEFAULT_SCALE_DENOMINATOR).makeGlobal().cache();
		PROJECTION = registerStringPreference("topo_print_projection", MapProjection.SPHERICAL.name()).makeGlobal().cache();
		SYMBOLS_SCALE = registerStringPreference("topo_print_symbols_scale", "1.0").makeGlobal().cache();
		OUTPUT_DIR = registerStringPreference("topo_print_output_dir", "").makeGlobal().cache();
		PREVIEW = registerBooleanPreference("topo_print_preview", true).makeGlobal().cache();
	}

	@Override
	public String getId() {
		return PLUGIN_TOPOPRINT;
	}

	@Override
	public String getName() {
		return app.getString(R.string.topoprint_plugin_name);
	}

	@Override
	public String getDescription(boolean linksEnabled) {
		return app.getString(R.string.topoprint_plugin_description);
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_plugin_topoprint;
	}

	@Nullable
	@Override
	public SettingsScreenType getSettingsScreenType() {
		return SettingsScreenType.TOPOPRINT_SETTINGS;
	}

	@Override
	public void registerLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		if (previewLayer == null) {
			previewLayer = new PrintPreviewLayer(context, this);
			app.getOsmandMap().getMapView().addLayer(previewLayer, 7.85f);
		}
		if (mapActivity != null) {
			addMapButton(mapActivity);
		}
	}

	/** Adds the print map button to the map hud (no-op while registered for the same activity). */
	private void addMapButton(@NonNull MapActivity mapActivity) {
		if (printMapButton != null && printMapButtonActivity == mapActivity) {
			return;
		}
		MapControlsLayer layer = mapActivity.getMapLayers().getMapControlsLayer();
		if (layer == null) {
			return;
		}
		boolean nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.MAP);
		// Inflate against the hud layout so the button gets valid LayoutParams
		// — inflating with a null parent leaves them null and
		// MapButton.updateSize() crashes on resume.
		MapHudLayout hudLayout = mapActivity.findViewById(R.id.map_hud_layout);
		printMapButton = (PrintMapButton) UiUtilities.getInflater(mapActivity, nightMode)
				.inflate(R.layout.map_print_button, hudLayout, false);
		printMapButtonActivity = mapActivity;
		// Clickability + appearance management...
		layer.addCustomMapButton(printMapButton, false, false, false, true);
		// ...and the actual attach to the hud (addCustomMapButton does not attach).
		if (hudLayout != null) {
			hudLayout.addMapButton(printMapButton);
		}
	}

	@Override
	public void updateLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		if (isActive() && PREVIEW.get()) {
			registerLayers(context, mapActivity);
		} else if (previewLayer != null) {
			mapView.removeLayer(previewLayer);
			previewLayer = null;
		}
		if (isActive() && mapActivity != null) {
			// Plugin enabled mid-session: the button is registered now instead
			// of at map creation. While the plugin is disabled the button
			// stays registered but shouldShow() keeps it invisible.
			addMapButton(mapActivity);
		}
		mapView.refreshMap();
	}

	@Override
	protected void registerConfigureMapCategoryActions(@NonNull ContextMenuAdapter adapter,
	                                                   @NonNull MapActivity mapActivity,
	                                                   @NonNull List<RenderingRuleProperty> customRules) {
		adapter.addItem(new ContextMenuItem(MAP_CONTEXT_MENU_TOPOPRINT_PREVIEW)
				.setTitleId(R.string.topoprint_plugin_name, mapActivity)
				.setIcon(R.drawable.ic_plugin_topoprint)
				.setDescription(getPrintParametersSummary())
				.setListener((uiAdapter, view, item, isChecked) -> {
					TopoPrintSettingsFragment.showInstance(mapActivity);
					return false;
				}));
	}

	/** Label/text scale for the offscreen render; 1.0 keeps the app's map text setting. */
	public float getSymbolsScale() {
		try {
			float value = Float.parseFloat(SYMBOLS_SCALE.get().trim());
			if (value >= 0.5f && value <= 3.0f) {
				return value;
			}
		} catch (NumberFormatException ignored) {
		}
		return 1.0f;
	}

	/**
	 * Map output directory: the user-configured path, or the app-private
	 * {@code topoprint} folder when unset.
	 */
	@NonNull
	public File getOutputDir() {
		String path = OUTPUT_DIR.get().trim();
		if (path.length() > 0) {
			return new File(path);
		}
		return new File(app.getExternalFilesDir(null), "topoprint");
	}

	@NonNull
	public PrintMapButtonState getPrintMapButtonState() {
		if (printMapButtonState == null) {
			printMapButtonState = new PrintMapButtonState(app);
		}
		return printMapButtonState;
	}

	@NonNull
	public PrintParameters getPrintParameters() {
		PaperSize paperSize = PaperSize.parse(PAPER_SIZE.get(), PaperSize.A4);
		PrintOrientation orientation = PrintOrientation.parse(ORIENTATION.get(), PrintOrientation.PORTRAIT);
		MapProjection projection = MapProjection.parse(PROJECTION.get(), MapProjection.SPHERICAL);
		return new PrintParameters(paperSize, orientation, parseDpi(DPI.get()), parseScale(SCALE_DENOMINATOR.get()),
				projection, parseCustomDim(CUSTOM_PAPER_WIDTH.get()), parseCustomDim(CUSTOM_PAPER_HEIGHT.get()));
	}

	/** Short human-readable summary of the current print settings, e.g. "A4 · Portrait · 300 DPI · 1:25000". */
	@NonNull
	public String getPrintParametersSummary() {
		PrintParameters params = getPrintParameters();
		String orientation = app.getString(params.getOrientation() == PrintOrientation.PORTRAIT
				? R.string.topoprint_portrait : R.string.topoprint_landscape);
		String projection = app.getString(params.getProjection() == MapProjection.SPHERICAL
				? R.string.topoprint_projection_spherical : R.string.topoprint_projection_mercator);
		return app.getString(R.string.ltr_or_rtl_combine_via_space,
				params.getPaper().name() + " · " + orientation + " · " + params.getDpi() + " DPI · " + projection,
				"1:" + params.getScaleDenominator());
	}

	/**
	 * Shows the map generation dialog: the title input (defaulting to the
	 * current centre coordinates) plus a preview of the current print
	 * parameters. The export itself starts only after the user confirms.
	 */
	private String defaultLabel(@NonNull LatLon center) {
		UtmCoordinate utm = UtmMath.INSTANCE.fromLatLon(center.getLatitude(), center.getLongitude());
		String zone = utm.getZoneNumber() + String.valueOf(UtmMath.INSTANCE.latitudeBand(center.getLatitude()));
		return app.getString(R.string.ltr_or_rtl_combine_via_space, zone,
			Math.round(utm.getEasting()) + " " + Math.round(utm.getNorthing()));
	}

	public void showGenerateDialog(@NonNull MapActivity mapActivity) {
		LatLon center = app.getOsmandMap().getMapView().getCurrentRotatedTileBox().getCenterLatLon();
		String defaultTitle = defaultLabel(center);

		// Build the custom views against the same themed context the dialog
		// uses: with the raw activity context their text colors follow the
		// app theme instead of the dialog's day/night mode, which turns them
		// light gray (unreadable) whenever the two modes disagree.
		Context themedContext = UiUtilities.getThemedContext(mapActivity,
				app.getDaynightHelper().isNightMode(ThemeUsageContext.OVER_MAP));

		android.widget.LinearLayout layout = new android.widget.LinearLayout(themedContext);
		layout.setOrientation(android.widget.LinearLayout.VERTICAL);
		float density = mapActivity.getResources().getDisplayMetrics().density;
		int pad = (int) (density * 20);
		int padHalf = (int) (density * 10);
		layout.setPadding(pad, padHalf, pad, 0);

		final android.widget.EditText titleInput = new android.widget.EditText(themedContext);
		titleInput.setHint(R.string.topoprint_title);
		titleInput.setText(defaultTitle);
		titleInput.setSingleLine(true);
		layout.addView(titleInput);
		// Select-all the default so typing replaces it outright.
		titleInput.post(() -> titleInput.setSelection(0, titleInput.getText().length()));

		android.widget.TextView paramsView = new android.widget.TextView(themedContext);
		paramsView.setText(app.getString(R.string.topoprint_params_preview, getPrintParametersSummary()));
		paramsView.setTextIsSelectable(true);
		android.widget.LinearLayout.LayoutParams paramsLayout =
				new android.widget.LinearLayout.LayoutParams(
						android.view.ViewGroup.LayoutParams.MATCH_PARENT,
						android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
		paramsLayout.setMargins(0, padHalf, 0, 0);
		layout.addView(paramsView, paramsLayout);

		AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);
		builder.setTitle(R.string.topoprint_generate);
		builder.setView(layout);
		builder.setPositiveButton(R.string.topoprint_output, (dialog, which) -> {
			String title = titleInput.getText().toString().trim();
			if (title.isEmpty()) {
				title = defaultTitle;
			}
			startMapTask(mapActivity, title);
		});
		// Jump straight to the print settings from the generation dialog.
		builder.setNeutralButton(R.string.shared_string_settings, (dialog, which) -> {
			dialog.dismiss();
			TopoPrintSettingsFragment.showInstance(mapActivity);
		});
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		AlertDialog dialog = builder.show();
		// Focus the title input (and raise the keyboard) so the pre-selected
		// title can be replaced by typing right away.
		dialog.getWindow().setSoftInputMode(
				android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		titleInput.post(() -> {
			titleInput.requestFocus();
			InputMethodManager imm = (InputMethodManager) app
					.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showSoftInput(titleInput, InputMethodManager.SHOW_IMPLICIT);
		});
	}

	private void startMapTask(@NonNull MapActivity mapActivity, @NonNull String label) {
		PrintParameters params = getPrintParameters();
		LatLon center = app.getOsmandMap().getMapView().getCurrentRotatedTileBox().getCenterLatLon();
		File outputDir = getOutputDir();

		// Modal progress dialog: the map is frozen while the offscreen render
		// owns the shared EGL renderer, so user interaction must be locked out.
		// Cancelling (button or back) aborts the render at the next stage
		// boundary and restores the map.
		final PrintMapTask[] taskHolder = new PrintMapTask[1];
		AlertDialog progress = new AlertDialog.Builder(UiUtilities.getThemedContext(mapActivity,
				app.getDaynightHelper().isNightMode(ThemeUsageContext.OVER_MAP)))
				.setTitle(R.string.topoprint_plugin_name)
				.setMessage(R.string.topoprint_rendering)
				.setCancelable(true)
				.setOnCancelListener(dialog -> {
					PrintMapTask task = taskHolder[0];
					if (task != null) {
						task.cancel();
					}
				})
				.create();
		// Dialog method — the androidx Builder has no such fluent setter.
		progress.setCanceledOnTouchOutside(false);
		progress.setButton(AlertDialog.BUTTON_NEGATIVE, app.getString(R.string.shared_string_cancel),
				(dialog, which) -> {
					PrintMapTask task = taskHolder[0];
					if (task != null) {
						task.cancel();
					}
				});
		progress.show();

		PrintMapTask task = new PrintMapTask(app, params, center, label, outputDir,
				new PrintMapTask.Listener() {
					@Override
					public void onProgress(@NonNull String message) {
						// notifyProgress() always dispatches on the UI thread
						progress.setMessage(message);
					}

					@Override
					public void onOverwriteQuery(@NonNull List<? extends File> existing,
							@NonNull PrintMapTask.OverwriteDecision decision) {
						// Delivered on the UI thread. If the activity died
						// during the render, decline and release the task's
						// latch instead of showing a dialog on a dead token.
						if (mapActivity.isDestroyed() || mapActivity.isFinishing()) {
							decision.onDecision(false);
							return;
						}
						StringBuilder names = new StringBuilder();
						for (File f : existing) {
							if (names.length() > 0) {
								names.append("\n");
							}
							names.append(f.getName());
						}
						new AlertDialog.Builder(UiUtilities.getThemedContext(mapActivity,
										app.getDaynightHelper().isNightMode(ThemeUsageContext.OVER_MAP)))
								.setTitle(R.string.topoprint_plugin_name)
								.setMessage(names + "\n\n" + app.getString(R.string.shared_string_replace) + "?")
								.setCancelable(false)
								.setPositiveButton(R.string.shared_string_replace,
										(d, which) -> decision.onDecision(true))
								.setNegativeButton(R.string.shared_string_cancel,
										(d, which) -> decision.onDecision(false))
								.show();
					}

					@Override
					public void onCancelled() {
						progress.dismiss();
						Toast.makeText(app, R.string.topoprint_cancelled, Toast.LENGTH_SHORT).show();
					}

					@Override
					public void onCompleted(@Nullable PrintMapTask.Result result) {
						progress.dismiss();
						// The render outlives configuration changes and
						// backgrounding: showing a dialog on a destroyed
						// activity's token crashes with BadTokenException.
						if (mapActivity.isDestroyed() || mapActivity.isFinishing()) {
							return;
						}
						if (result != null) {
							showResultDialog(mapActivity, params, label, result);
						} else {
							Toast.makeText(app, R.string.topoprint_failed, Toast.LENGTH_LONG).show();
						}
					}
				});
		new Thread(task::run, "TopoPrintGenerate").start();
	}

	private void showResultDialog(@NonNull MapActivity mapActivity, @NonNull PrintParameters params,
	                              @NonNull String label, @NonNull PrintMapTask.Result result) {
		boolean hasPng = result.getPngFile().isFile();
		StringBuilder message = new StringBuilder();
		message.append(app.getString(R.string.topoprint_saved_to,
				result.getSvgFile().getParentFile()));
		if (!hasPng) {
			message.append("\n\n").append(app.getString(R.string.topoprint_svg_only_hint));
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(UiUtilities.getThemedContext(mapActivity,
				app.getDaynightHelper().isNightMode(ThemeUsageContext.OVER_MAP)));
		builder.setTitle(label);
		builder.setMessage(message);
		if (hasPng) {
			builder.setPositiveButton(R.string.topoprint_print, (dialog, which) -> {
				int widthMils = (int) Math.round(params.getLayout().getPaperWidthMm() * 1000 / 25.4);
				int heightMils = (int) Math.round(params.getLayout().getPaperHeightMm() * 1000 / 25.4);
				PrintExportHelper.printPng(mapActivity, result.getPngFile(), label, widthMils, heightMils);
			});
			builder.setNeutralButton(R.string.shared_string_share, (dialog, which) ->
					PrintExportHelper.shareFile(mapActivity, result.getPngFile(), "image/png"));
			builder.setNegativeButton(R.string.topoprint_open, (dialog, which) ->
					PrintExportHelper.openFile(mapActivity, result.getPngFile(), "image/png"));
		} else {
			builder.setPositiveButton(R.string.shared_string_share, (dialog, which) ->
					PrintExportHelper.shareFile(mapActivity, result.getSvgFile(), "image/svg+xml"));
			builder.setNegativeButton(R.string.shared_string_close, null);
		}
		builder.show();
	}

	private static double parseCustomDim(String value) {
		try {
			return Double.parseDouble(value.trim());
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}

	private static int parseDpi(int dpi) {
		for (int supported : PrintParameters.SUPPORTED_DPI) {
			if (supported == dpi) {
				return dpi;
			}
		}
		return DEFAULT_DPI;
	}

	private static int parseScale(int scale) {
		for (int supported : PrintParameters.SUPPORTED_SCALES) {
			if (supported == scale) {
				return scale;
			}
		}
		return DEFAULT_SCALE_DENOMINATOR;
	}
}
