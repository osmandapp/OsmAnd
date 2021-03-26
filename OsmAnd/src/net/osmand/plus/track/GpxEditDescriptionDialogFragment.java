package net.osmand.plus.track;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.track.SaveGpxAsyncTask.SaveGpxListener;
import net.osmand.plus.widgets.EditTextEx;

import org.apache.commons.logging.Log;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

public class GpxEditDescriptionDialogFragment extends BaseOsmAndDialogFragment {

	public static final String TAG = GpxEditDescriptionDialogFragment.class.getSimpleName();
	private static final Log log = PlatformUtil.getLog(GpxEditDescriptionDialogFragment.class);

	private static final String CONTENT_KEY = "content_key";

	private EditTextEx editableHtml;

	private String htmlCode;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		OsmandApplication app = getMyApplication();
		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		LayoutInflater themedInflater = UiUtilities.getInflater(requireContext(), nightMode);
		View view = themedInflater.inflate(R.layout.dialog_edit_gpx_description, container, false);

		editableHtml = view.findViewById(R.id.description);
		editableHtml.requestFocus();

		view.findViewById(R.id.btn_close).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (shouldClose()) {
					dismiss();
				} else {
					showDismissDialog();
				}
			}
		});

		setupSaveButton(view);

		Bundle args = getArguments();
		if (args != null) {
			htmlCode = args.getString(CONTENT_KEY);
			if (htmlCode != null) {
				editableHtml.append(htmlCode);
			}
		}

		return view;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		Activity ctx = getActivity();
		int themeId = isNightMode(true) ? R.style.OsmandDarkTheme_DarkActionbar : R.style.OsmandLightTheme_DarkActionbar_LightStatusBar;
		Dialog dialog = new Dialog(ctx, themeId);
		Window window = dialog.getWindow();
		if (window != null) {
			if (!getSettings().DO_NOT_USE_ANIMATIONS.get()) {
				window.getAttributes().windowAnimations = R.style.Animations_Alpha;
			}
			if (Build.VERSION.SDK_INT >= 21) {
				int statusBarColor = isNightMode(true) ? R.color.activity_background_color_dark : R.color.activity_background_color_light;
				window.setStatusBarColor(ContextCompat.getColor(ctx, statusBarColor));
			}
		}
		return dialog;
	}

	private boolean shouldClose() {
		Editable editable = editableHtml.getText();
		if (htmlCode == null || editable == null || editable.toString() == null) {
			return true;
		}
		return htmlCode.equals(editable.toString());
	}

	private void setupSaveButton(View view) {
		View btnSaveContainer = view.findViewById(R.id.btn_save_container);
		btnSaveContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Editable editable = editableHtml.getText();
				if (editable != null && !saveGpx(editable.toString())) {
					dismiss();
				}
			}
		});

		Context ctx = btnSaveContainer.getContext();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			AndroidUtils.setBackground(ctx, btnSaveContainer, isNightMode(true), R.drawable.ripple_light, R.drawable.ripple_dark);
		} else {
			AndroidUtils.setBackground(ctx, btnSaveContainer, isNightMode(true), R.drawable.btn_unstroked_light, R.drawable.btn_unstroked_dark);
		}

		View btnSave = view.findViewById(R.id.btn_save);
		int drawableRes = isNightMode(true) ? R.drawable.btn_solid_border_dark : R.drawable.btn_solid_border_light;
		AndroidUtils.setBackground(btnSave, getMyApplication().getUIUtilities().getIcon(drawableRes));
	}

	private void showDismissDialog() {
		Context themedContext = UiUtilities.getThemedContext(getMapActivity(), isNightMode(false));
		AlertDialog.Builder dismissDialog = new AlertDialog.Builder(themedContext);
		dismissDialog.setTitle(getString(R.string.shared_string_dismiss));
		dismissDialog.setMessage(getString(R.string.exit_without_saving));
		dismissDialog.setNegativeButton(R.string.shared_string_cancel, null);
		dismissDialog.setPositiveButton(R.string.shared_string_exit, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dismiss();
			}
		});
		dismissDialog.show();
	}

	private boolean saveGpx(final String html) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null || mapActivity.getTrackMenuFragment() == null) {
			return false;
		}
		TrackMenuFragment trackMenuFragment = mapActivity.getTrackMenuFragment();

		GPXFile gpx = trackMenuFragment.getGpx();
		gpx.metadata.getExtensionsToWrite().put("desc", html);

		File file = trackMenuFragment.getDisplayHelper().getFile();
		new SaveGpxAsyncTask(file, gpx, new SaveGpxListener() {
			@Override
			public void gpxSavingStarted() {
			}

			@Override
			public void gpxSavingFinished(Exception errorMessage) {
				if (errorMessage != null) {
					log.error(errorMessage);
				}
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null && mapActivity.getTrackMenuFragment() != null) {
					TrackMenuFragment trackMenuFragment = mapActivity.getTrackMenuFragment();
					trackMenuFragment.updateContent();
				}
				Fragment target = getTargetFragment();
				if (target instanceof GpxReadDescriptionDialogFragment) {
					((GpxReadDescriptionDialogFragment) target).updateContent(html);
				}
				dismiss();
			}
		}).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		return true;
	}

	@Nullable
	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	public static void showInstance(@NonNull FragmentActivity activity, @NonNull String description, @Nullable Fragment target) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (!fragmentManager.isStateSaved()) {
			Bundle args = new Bundle();
			args.putString(CONTENT_KEY, description);

			GpxEditDescriptionDialogFragment fragment = new GpxEditDescriptionDialogFragment();
			fragment.setArguments(args);
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, GpxEditDescriptionDialogFragment.TAG);
		}
	}
}
