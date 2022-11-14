package net.osmand.plus.dialogs;

import android.app.ProgressDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.plus.widgets.TextViewEx;

public class MapRenderingEngineDialog {
	private final OsmandApplication app;
	private final FragmentActivity fragmentActivity;
	private AppCompatRadioButton radioButton1;
	private AppCompatRadioButton radioButton2;

	public MapRenderingEngineDialog(OsmandApplication app, FragmentActivity fragmentActivity) {
		this.app = app;
		this.fragmentActivity = fragmentActivity;
	}

	private AlertDialog createDialog(OnRenderChangeListener renderChangeListener) {
		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(fragmentActivity, getThemeRes(app)));
		Context themedContext = UiUtilities.getThemedContext(app, nightMode);
		View alertDialogView = LayoutInflater.from(themedContext).inflate(R.layout.alert_dialog_message_with_choice_list, null, false);
		builder.setView(alertDialogView);

		View version1 = alertDialogView.findViewById(R.id.version_1);
		TextViewEx text1 = version1.findViewById(R.id.text);
		text1.setText(app.getResources().getString(R.string.map_rendering_engine_v1));
		radioButton1 = version1.findViewById(R.id.radio);
		radioButton1.setVisibility(View.VISIBLE);

		View version2 = alertDialogView.findViewById(R.id.version_2);
		radioButton2 = version2.findViewById(R.id.radio);
		TextViewEx text2 = version2.findViewById(R.id.text);
		text2.setText(app.getResources().getString(R.string.map_rendering_engine_v2));
		radioButton2.setVisibility(View.VISIBLE);

		updateRadioButtons(app.getSettings().USE_OPENGL_RENDER.get());

		version1.findViewById(R.id.button).setOnClickListener(view -> {
			updateRenderingEngineSetting(false, renderChangeListener);
		});

		version2.findViewById(R.id.button).setOnClickListener(view -> {
			updateRenderingEngineSetting(true, renderChangeListener);
		});
		return builder.create();
	}

	private void updateRenderingEngineSetting(boolean openglEnabled, OnRenderChangeListener renderChangeListener) {
		updateRadioButtons(openglEnabled);
		app.getSettings().USE_OPENGL_RENDER.set(openglEnabled);

		if (openglEnabled && !NativeCoreContext.isInit()) {
			FragmentActivity activity = fragmentActivity;
			String title = app.getString(R.string.loading_smth, "");
			ProgressDialog progress = ProgressDialog.show(activity, title, app.getString(R.string.loading_data));
			app.getAppInitializer().initOpenglAsync(() -> {
				updateMap();
				if (AndroidUtils.isActivityNotDestroyed(activity)) {
					progress.dismiss();
				}
			});
		} else {
			updateMap();
		}
		renderChangeListener.onRenderChange();
	}

	private void updateMap() {
		app.getOsmandMap().setupRenderingView();

		MapActivity mapActivity = app.getOsmandMap().getMapView().getMapActivity();
		if (mapActivity != null) {
			mapActivity.refreshMapComplete();
		}
	}

	private void updateRadioButtons(boolean openglEnabled) {
		radioButton1.setChecked(!openglEnabled);
		radioButton2.setChecked(openglEnabled);
	}

	public static int getThemeRes(OsmandApplication app) {
		return app.getDaynightHelper().isNightModeForMapControls() ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
	}

	public void showDialog(OnRenderChangeListener renderChangeListener) {
		createDialog(renderChangeListener).show();
	}

	public interface OnRenderChangeListener {
		void onRenderChange();
	}
}
