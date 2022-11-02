package net.osmand.plus.settings.fragments;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.TwoStatePreference;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.corenative.NativeCoreContext;

public class RenderingEngineFragment extends BaseSettingsFragment {

	private static final String PREFERENCES_INFO = "preferences_info";
	private static final String ENGINE_OPENGL = "engine_opengl";
	private static final String ENGINE_LEGACY = "engine_legacy";
	private static final String USE_OPENGL_RENDER_KEY = "use_opengl_render";

	private TwoStatePreference engineOpengl;
	private TwoStatePreference engineLegacy;

	private boolean useOpenglRender;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		boolean openglEnabled = settings.USE_OPENGL_RENDER.get();
		useOpenglRender = savedInstanceState == null ? openglEnabled :
				savedInstanceState.getBoolean(USE_OPENGL_RENDER_KEY, openglEnabled);

		FragmentActivity activity = requireMyActivity();
		activity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			public void handleOnBackPressed() {
				if (engineChanged()) {
					showExitDialog();
				} else {
					dismiss();
				}
			}
		});
	}

	@Override
	protected void createToolbar(LayoutInflater inflater, View view) {
		super.createToolbar(inflater, view);

		ViewGroup container = view.findViewById(R.id.action_container);
		AndroidUiHelper.updateVisibility(container, true);

		TextView textView = (TextView) inflater.inflate(R.layout.preference_action_text, container, false);
		textView.setText(R.string.shared_string_apply);
		container.addView(textView);

		container.setOnClickListener(v -> apply());
	}

	private void showExitDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
		builder.setTitle(R.string.dismiss_changes);
		builder.setMessage(R.string.dismiss_changes_descr);
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.setPositiveButton(R.string.shared_string_exit, (dialog, which) -> dismiss());
		builder.show();
	}

	private void apply() {
		if (engineChanged()) {
			settings.USE_OPENGL_RENDER.set(useOpenglRender);

			if (useOpenglRender && !NativeCoreContext.isInit()) {
				FragmentActivity activity = getActivity();
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
		}
		dismiss();
	}

	private void updateMap() {
		app.getOsmandMap().setupOpenGLView();

		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.refreshMapComplete();
		}
	}

	private boolean engineChanged() {
		return settings.USE_OPENGL_RENDER.get() != useOpenglRender;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(USE_OPENGL_RENDER_KEY, useOpenglRender);
	}

	@Override
	protected void setupPreferences() {
		Preference preferencesInfo = findPreference(PREFERENCES_INFO);
		preferencesInfo.setIconSpaceReserved(false);

		engineOpengl = findPreference(ENGINE_OPENGL);
		engineLegacy = findPreference(ENGINE_LEGACY);
		updateEnginePrefs();
	}

	private void updateEnginePrefs() {
		engineOpengl.setChecked(useOpenglRender);
		engineLegacy.setChecked(!useOpenglRender);
	}

	@Override
	protected void onBindPreferenceViewHolder(Preference preference, PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);
		if (PREFERENCES_INFO.equals(preference.getKey())) {
			TextView titleView = (TextView) holder.findViewById(android.R.id.title);
			if (titleView != null) {
				titleView.setTextColor(getDisabledTextColor());
			}
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String prefId = preference.getKey();
		if (ENGINE_OPENGL.equals(prefId) || ENGINE_LEGACY.equals(prefId)) {
			if (newValue instanceof Boolean && (Boolean) newValue) {
				useOpenglRender = ENGINE_OPENGL.equals(prefId);
				updateEnginePrefs();
				return true;
			}
			return false;
		}
		return super.onPreferenceChange(preference, newValue);
	}
}
