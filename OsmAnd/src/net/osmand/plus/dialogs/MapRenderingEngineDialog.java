package net.osmand.plus.dialogs;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.AppInitializeListener;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.plus.widgets.TextViewEx;

public class MapRenderingEngineDialog extends DialogFragment {
	private final OsmandApplication app;
	private final FragmentActivity fragmentActivity;
	@Nullable
	private final OnRenderChangeListener renderChangeListener;
	private AppCompatRadioButton radioButtonLegacy;
	private AppCompatRadioButton radioButtonOpengl;

	public MapRenderingEngineDialog(final OsmandApplication app,
									final FragmentActivity fragmentActivity,
									final @Nullable OnRenderChangeListener renderChangeListener) {
		this.app = app;
		this.fragmentActivity = fragmentActivity;
		this.renderChangeListener = renderChangeListener;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable final Bundle savedInstanceState) {
		return createDialog();
	}

	private AlertDialog createDialog() {
		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		Context themedContext = UiUtilities.getThemedContext(fragmentActivity, nightMode);
		AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);
		View alertDialogView = LayoutInflater.from(themedContext).inflate(R.layout.alert_dialog_message_with_choice_list, null, false);
		builder.setView(alertDialogView);

		View legacyRenderingView = alertDialogView.findViewById(R.id.legacy_rendering);
		radioButtonLegacy = setupRadioItem(legacyRenderingView, app.getResources().getString(R.string.map_rendering_engine_v1));
		View openglRenderingView = alertDialogView.findViewById(R.id.opengl_rendering);
		radioButtonOpengl = setupRadioItem(openglRenderingView, app.getResources().getString(R.string.map_rendering_engine_v2));
		updateRadioButtons(app.getSettings().USE_OPENGL_RENDER.get());
		radioButtonOpengl.setEnabled(Version.isOpenGlAvailable(app));
		openglRenderingView.findViewById(R.id.button).setEnabled(Version.isOpenGlAvailable(app));

		legacyRenderingView.findViewById(R.id.button).setOnClickListener(view -> {
			updateRenderingEngineSetting(false);
			dismiss();
		});

		openglRenderingView.findViewById(R.id.button).setOnClickListener(view -> {
			updateRenderingEngineSetting(true);
			dismiss();
		});
		return builder.create();
	}

	private AppCompatRadioButton setupRadioItem(View view, String name) {
		AppCompatRadioButton radioButton = view.findViewById(R.id.radio);
		TextViewEx text = view.findViewById(R.id.text);
		text.setText(name);
		radioButton.setVisibility(View.VISIBLE);

		return radioButton;
	}

	private void updateRenderingEngineSetting(boolean openglEnabled) {
		updateRadioButtons(openglEnabled);
		app.getSettings().USE_OPENGL_RENDER.set(openglEnabled);

		if (app.isApplicationInitializing()) {
			String title = app.getString(R.string.loading_smth, "");
			ProgressDialog progress = ProgressDialog.show(fragmentActivity, title, app.getString(R.string.loading_data));
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
			String title = app.getString(R.string.loading_smth, "");
			ProgressDialog progress = ProgressDialog.show(fragmentActivity, title, app.getString(R.string.loading_data));
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

		MapActivity mapActivity = app.getOsmandMap().getMapView().getMapActivity();
		if (mapActivity != null) {
			mapActivity.refreshMapComplete();
		}

		app.getDownloadThread().runReloadIndexFilesSilent();
	}

	private void updateRadioButtons(boolean openglEnabled) {
		radioButtonLegacy.setChecked(!openglEnabled);
		radioButtonOpengl.setChecked(openglEnabled);
	}

	public interface OnRenderChangeListener {
		void onRenderChange();
	}
}
