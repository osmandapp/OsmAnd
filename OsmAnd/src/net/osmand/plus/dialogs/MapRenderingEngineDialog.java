package net.osmand.plus.dialogs;

import android.app.ProgressDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.plus.widgets.TextViewEx;

public class MapRenderingEngineDialog {
	private final OsmandApplication app;
	private final FragmentActivity fragmentActivity;
	private AppCompatRadioButton radioButtonLegacy;
	private AppCompatRadioButton radioButtonOpengl;
	private AlertDialog alertDialog;

	public MapRenderingEngineDialog(OsmandApplication app, FragmentActivity fragmentActivity) {
		this.app = app;
		this.fragmentActivity = fragmentActivity;
	}

	private AlertDialog createDialog(@Nullable OnRenderChangeListener renderChangeListener) {
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
			updateRenderingEngineSetting(false, renderChangeListener);
			alertDialog.dismiss();
		});

		openglRenderingView.findViewById(R.id.button).setOnClickListener(view -> {
			updateRenderingEngineSetting(true, renderChangeListener);
			alertDialog.dismiss();
		});
		return builder.create();
	}

	private AppCompatRadioButton setupRadioItem(View view, String name){
		AppCompatRadioButton radioButton = view.findViewById(R.id.radio);
		TextViewEx text = view.findViewById(R.id.text);
		text.setText(name);
		radioButton.setVisibility(View.VISIBLE);

		return radioButton;
	}

	private void updateRenderingEngineSetting(boolean openglEnabled,@Nullable OnRenderChangeListener renderChangeListener) {
		updateRadioButtons(openglEnabled);
		app.getSettings().USE_OPENGL_RENDER.set(openglEnabled);

		if (openglEnabled && !NativeCoreContext.isInit()) {
			String title = app.getString(R.string.loading_smth, "");
			ProgressDialog progress = ProgressDialog.show(fragmentActivity, title, app.getString(R.string.loading_data));
			app.getAppInitializer().initOpenglAsync(() -> {
				updateMap();
				if (AndroidUtils.isActivityNotDestroyed(fragmentActivity)) {
					progress.dismiss();
				}
			});
		} else {
			updateMap();
			if (app.getSettings().MAP_OVERLAY_TRANSPARENCY != null) {
				app.getOsmandMap().getMapLayers().getMapVectorLayer().setAlpha(255);
			}
		}

		if (renderChangeListener != null) {
			renderChangeListener.onRenderChange();
		}
	}

	private void updateMap() {
		app.getOsmandMap().setupRenderingView();

		MapActivity mapActivity = app.getOsmandMap().getMapView().getMapActivity();
		if (mapActivity != null) {
			mapActivity.refreshMapComplete();
		}
	}

	private void updateRadioButtons(boolean openglEnabled) {
		radioButtonLegacy.setChecked(!openglEnabled);
		radioButtonOpengl.setChecked(openglEnabled);
	}

	public void showDialog(@Nullable OnRenderChangeListener renderChangeListener) {
		alertDialog = createDialog(renderChangeListener);
		alertDialog.show();
	}

	public interface OnRenderChangeListener {
		void onRenderChange();
	}
}
