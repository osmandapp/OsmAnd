package net.osmand.plus.dialogs;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.AppInitializeListener;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.fragments.search.SearchablePreferenceDialog;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.plus.widgets.TextViewEx;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MapRenderingEngineDialog extends DialogFragment implements SearchablePreferenceDialog {

	private final OsmandApplication app;
	private final FragmentActivity fragmentActivity;
	@Nullable
	private final OnRenderChangeListener renderChangeListener;

	public MapRenderingEngineDialog(final OsmandApplication app,
									final FragmentActivity fragmentActivity,
									@Nullable final OnRenderChangeListener renderChangeListener) {
		this.app = app;
		this.fragmentActivity = fragmentActivity;
		this.renderChangeListener = renderChangeListener;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable final Bundle savedInstanceState) {
		AlertDialog.Builder builder =
				new AlertDialog.Builder(
						UiUtilities.getThemedContext(
								fragmentActivity,
								app.getDaynightHelper().isNightModeForMapControls()));
		final View alertDialogView = getLayoutInflater().inflate(R.layout.alert_dialog_message_with_choice_list, null, false);
		builder.setView(alertDialogView);

		final View legacyRenderingView = alertDialogView.findViewById(R.id.legacy_rendering);
		final AppCompatRadioButton radioButtonLegacy = setupRadioItem(legacyRenderingView, app.getString(getMapRenderingEngineV1()));
		final View openglRenderingView = alertDialogView.findViewById(R.id.opengl_rendering);
		final AppCompatRadioButton radioButtonOpengl = setupRadioItem(openglRenderingView, app.getString(getMapRenderingEngineV2()));
		updateRadioButtons(app.getSettings().USE_OPENGL_RENDER.get(), radioButtonLegacy, radioButtonOpengl);
		radioButtonOpengl.setEnabled(Version.isOpenGlAvailable(app));
		openglRenderingView.findViewById(R.id.button).setEnabled(Version.isOpenGlAvailable(app));
		legacyRenderingView.findViewById(R.id.button).setOnClickListener(view -> {
			updateRenderingEngineSetting(false, radioButtonLegacy, radioButtonOpengl);
			dismiss();
		});
		openglRenderingView.findViewById(R.id.button).setOnClickListener(view -> {
			updateRenderingEngineSetting(true, radioButtonLegacy, radioButtonOpengl);
			dismiss();
		});
		return builder.create();
	}

	private static @StringRes int getMapRenderingEngineV1() {
		return R.string.map_rendering_engine_v1;
	}

	private static @StringRes int getMapRenderingEngineV2() {
		return R.string.map_rendering_engine_v2;
	}

	private AppCompatRadioButton setupRadioItem(View view, String name) {
		final AppCompatRadioButton radioButton = view.findViewById(R.id.radio);
		final TextViewEx text = view.findViewById(R.id.text);
		text.setText(name);
		radioButton.setVisibility(View.VISIBLE);
		return radioButton;
	}

	private void updateRenderingEngineSetting(final boolean openglEnabled,
											  final AppCompatRadioButton radioButtonLegacy,
											  final AppCompatRadioButton radioButtonOpengl) {
		updateRadioButtons(openglEnabled, radioButtonLegacy, radioButtonOpengl);
		app.getSettings().USE_OPENGL_RENDER.set(openglEnabled);

		if (app.isApplicationInitializing()) {
			final String title = app.getString(R.string.loading_smth, "");
			final ProgressDialog progress = ProgressDialog.show(fragmentActivity, title, app.getString(R.string.loading_data));
			app.getAppInitializer().addListener(new AppInitializeListener() {
				@Override
				public void onFinish(@NonNull AppInitializer init) {
					if (AndroidUtils.isActivityNotDestroyed(fragmentActivity)) {
						progress.dismiss();
					}
					updateRenderingEngine(openglEnabled);
				}
			});
		} else {
			updateRenderingEngine(openglEnabled);
		}
	}

	private void updateRenderingEngine(boolean openglEnabled) {
		if (openglEnabled && !NativeCoreContext.isInit()) {
			final String title = app.getString(R.string.loading_smth, "");
			final ProgressDialog progress = ProgressDialog.show(fragmentActivity, title, app.getString(R.string.loading_data));
			app.getAppInitializer().initOpenglAsync(() -> {
				updateDependentAppComponents();
				if (AndroidUtils.isActivityNotDestroyed(fragmentActivity)) {
					progress.dismiss();
				}
			});
		} else {
			updateDependentAppComponents();
			if (app.getSettings().MAP_OVERLAY_TRANSPARENCY != null) {
				app.getOsmandMap().getMapLayers().getMapVectorLayer().setAlpha(255);
			}
		}
		if (renderChangeListener != null) {
			renderChangeListener.onRenderChange();
		}
	}

	private void updateDependentAppComponents() {
		app.getOsmandMap().setupRenderingView();

		final MapActivity mapActivity = app.getOsmandMap().getMapView().getMapActivity();
		if (mapActivity != null) {
			mapActivity.refreshMapComplete();
		}

		app.getDownloadThread().runReloadIndexFilesSilent();
	}

	private void updateRadioButtons(final boolean openglEnabled,
									final AppCompatRadioButton radioButtonLegacy,
									final AppCompatRadioButton radioButtonOpengl) {
		radioButtonLegacy.setChecked(!openglEnabled);
		radioButtonOpengl.setChecked(openglEnabled);
	}

	@Override
	public void show(final FragmentManager fragmentManager) {
		show(fragmentManager, null);
	}

	@Override
	public String getSearchableInfo() {
		return Stream
				.of(
						R.string.map_rendering_engine_descr,
						getMapRenderingEngineV1(),
						getMapRenderingEngineV2())
				.map(app::getString)
				.collect(Collectors.joining(", "));
	}

	public interface OnRenderChangeListener {
		void onRenderChange();
	}
}
