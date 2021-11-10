package net.osmand.plus.settings.datastorage;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.datastorage.SkipStorageMigrationBottomSheet.OnConfirmMigrationSkipListener;

public class SharedStorageWarningFragment extends BaseOsmAndFragment implements OnConfirmMigrationSkipListener {

	private static final String TAG = SharedStorageWarningFragment.class.getSimpleName();

	private static final String USED_ON_MAP_KEY = "used_on_map";
	private static final int FOLDER_ACCESS_REQUEST = 1009;

	private OsmandApplication app;

	private boolean nightMode;
	private boolean usedOnMap;

	@Override
	public int getStatusBarColorId() {
		return nightMode ? R.color.status_bar_color_dark : R.color.status_bar_color_light;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		if (savedInstanceState != null) {
			usedOnMap = savedInstanceState.getBoolean(USED_ON_MAP_KEY);
		}
		nightMode = isNightMode(usedOnMap);

		FragmentActivity activity = requireActivity();
		activity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			public void handleOnBackPressed() {
				showSkipMigrationDialog();
			}
		});
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(requireContext(), nightMode);
		View view = themedInflater.inflate(R.layout.shared_storage_warning, container, false);
		AndroidUtils.addStatusBarPadding21v(view.getContext(), view);

		setupToolbar(view);
		setupButtons(view);
		setupMigrationDescr(view);
		setupFirstStepDescr(view);
		ViewCompat.setNestedScrollingEnabled(view.findViewById(R.id.list), true);

		return view;
	}

	@Override
	public void onMigrationSkipConfirmed() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			activity.getSupportFragmentManager().popBackStack();
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(USED_ON_MAP_KEY, usedOnMap);
	}

	private void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getIcon(AndroidUtils.getNavigationIconResId(app)));
		toolbar.setNavigationContentDescription(R.string.shared_string_close);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showSkipMigrationDialog();
			}
		});
	}

	private void setupMigrationDescr(@NonNull View view) {
		String sharedStorage = getString(R.string.shared_storage);
		String warning = getString(R.string.shared_storage_migration_descr, sharedStorage);
		Typeface typeface = FontCache.getRobotoMedium(app);
		TextView warningInfo = view.findViewById(R.id.shared_storage_migration);
		warningInfo.setText(UiUtilities.createCustomFontSpannable(typeface, warning, sharedStorage));
	}

	private void setupFirstStepDescr(@NonNull View view) {
		String sharedStorage = getString(R.string.shared_string_continue);
		TextView warningInfo = view.findViewById(R.id.shared_storage_first_step);
		warningInfo.setText(getString(R.string.shared_storage_first_step, sharedStorage));
	}

	private void setupButtons(@NonNull View view) {
		View container = view.findViewById(R.id.buttons_container);
		container.setBackgroundColor(AndroidUtils.getColorFromAttr(view.getContext(), R.attr.bg_color));

		View continueButton = container.findViewById(R.id.right_bottom_button);
		continueButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				selectFolder();
			}
		});
		View skipButton = container.findViewById(R.id.dismiss_button);
		skipButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showSkipMigrationDialog();
			}
		});
		AndroidUiHelper.updateVisibility(continueButton, true);
		AndroidUiHelper.updateVisibility(container.findViewById(R.id.buttons_divider), true);
		UiUtilities.setupDialogButton(nightMode, skipButton, DialogButtonType.SECONDARY, R.string.shared_string_skip);
		UiUtilities.setupDialogButton(nightMode, continueButton, DialogButtonType.PRIMARY, R.string.shared_string_continue);
	}

	private void showSkipMigrationDialog() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			SkipStorageMigrationBottomSheet.showInstance(activity.getSupportFragmentManager(), this, usedOnMap);
		}
	}

	public void selectFolder() {
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
		startActivityForResult(intent, FOLDER_ACCESS_REQUEST);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if (requestCode == FOLDER_ACCESS_REQUEST) {
			if (resultCode == Activity.RESULT_OK) {
				FragmentActivity activity = getActivity();
				if (activity != null && data != null && data.getData() != null) {
					SharedStorageMigrationFragment.showInstance(activity.getSupportFragmentManager(), data.getData(), usedOnMap);
				}
			} else {
				app.showShortToastMessage(R.string.folder_access_denied);
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	public static boolean dialogShowRequired(@NonNull OsmandApplication app) {
		return true;
	}

	public static void showInstance(@NonNull MapActivity mapActivity, boolean usedOnMap) {
		FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			OsmandSettings settings = mapActivity.getMyApplication().getSettings();
			settings.SHARED_STORAGE_WARNING_DIALOG_SHOWN.set(true);

			SharedStorageWarningFragment fragment = new SharedStorageWarningFragment();
			fragment.usedOnMap = usedOnMap;
			fragmentManager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss();
		}
	}
}