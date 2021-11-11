package net.osmand.plus.settings.datastorage;

import static net.osmand.plus.settings.datastorage.StorageMigrationAsyncTask.getFilesSize;

import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.plus.ColorUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.base.BasicProgressAsyncTask;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.settings.datastorage.StorageMigrationAsyncTask.StorageMigrationListener;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;

import java.util.ArrayList;
import java.util.List;

public class StorageMigrationFragment extends BaseOsmAndDialogFragment implements StorageMigrationListener {

	private static final String TAG = StorageMigrationFragment.class.getSimpleName();

	private static final String USED_ON_MAP_KEY = "used_on_map";
	private static final String COPY_FINISHED_KEY = "copy_finished";

	private OsmandApplication app;
	private DataStorageHelper dataStorageHelper;
	private final List<DocumentFile> documentFiles = new ArrayList<>();

	private Toolbar toolbar;
	private View actionButton;
	private View remainingFiles;
	private View copyFilesDescr;
	private TextView progressTitle;
	private ProgressBar progressBar;

	private boolean nightMode;
	private boolean usedOnMap;
	private boolean copyFinished;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
		dataStorageHelper = new DataStorageHelper(app);

		if (savedInstanceState != null) {
			usedOnMap = savedInstanceState.getBoolean(USED_ON_MAP_KEY);
			copyFinished = savedInstanceState.getBoolean(COPY_FINISHED_KEY);
		}
		nightMode = isNightMode(usedOnMap);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(requireContext(), nightMode);
		View view = themedInflater.inflate(R.layout.copy_files_fragment, container, false);
		ViewCompat.setNestedScrollingEnabled(view.findViewById(R.id.list), true);

		setupToolbar(view);
		setupButtons(view);
		setupProgress(view);
		setupProgressDescr(view);
		setupRemainingFiles(view);

		return view;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(USED_ON_MAP_KEY, usedOnMap);
		outState.putBoolean(COPY_FINISHED_KEY, copyFinished);
	}

	private void setupToolbar(@NonNull View view) {
		toolbar = view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getIcon(AndroidUtils.getNavigationIconResId(app)));
		toolbar.setNavigationContentDescription(R.string.shared_string_close);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					activity.onBackPressed();
				}
			}
		});
	}

	private void setupProgress(@NonNull View view) {
		progressBar = view.findViewById(R.id.progress_bar);
		progressTitle = view.findViewById(R.id.progress_title);
		progressBar.setMin(0);
		progressBar.setMax((int) (getFilesSize(documentFiles) / 1024));
	}

	private void setupButtons(@NonNull View view) {
		View container = view.findViewById(R.id.buttons_container);
		actionButton = container.findViewById(R.id.dismiss_button);
		actionButton.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity instanceof MapActivity) {
				MapActivity.doRestart(activity);
			} else {
				android.os.Process.killProcess(android.os.Process.myPid());
			}
		});
		UiUtilities.setupDialogButton(nightMode, actionButton, DialogButtonType.PRIMARY, R.string.start_copying);
	}

	private void setupProgressDescr(@NonNull View view) {
		copyFilesDescr = view.findViewById(R.id.copy_files_descr);
		setupFilesTitle();
		setupFilesDescr();
		AndroidUiHelper.updateVisibility(copyFilesDescr.findViewById(android.R.id.icon), false);
	}

	private void setupFilesTitle() {
		String amount = String.valueOf(documentFiles.size());
		String formattedSize = "(" + AndroidUtils.formatSize(app, getFilesSize(documentFiles)) + ")";
		String warning = getString(copyFinished ? R.string.storage_copied_files_size : R.string.storage_copying_files_size, amount, formattedSize);

		SpannableString spannable = new SpannableString(warning);
		int index = warning.indexOf(amount);
		spannable.setSpan(new CustomTypefaceSpan(FontCache.getRobotoMedium(app)), index, index + amount.length(), 0);
		index = warning.indexOf(formattedSize);
		spannable.setSpan(new ForegroundColorSpan(ColorUtilities.getSecondaryTextColor(app, nightMode)), index, index + formattedSize.length(), 0);

		TextView warningInfo = copyFilesDescr.findViewById(android.R.id.title);
		warningInfo.setText(spannable);
	}

	private void setupFilesDescr() {
		String sharedStorage = "\"" + getString(R.string.shared_storage) + "\"";
		String currentStorage = "\"" + dataStorageHelper.getCurrentStorage().getTitle() + "\"";
		String description = getString(R.string.from_to_with_params, sharedStorage, currentStorage);

		TextView summary = copyFilesDescr.findViewById(android.R.id.summary);
		summary.setText(UiUtilities.createCustomFontSpannable(FontCache.getRobotoMedium(app), description, sharedStorage, currentStorage));
	}

	private void setupRemainingFiles(@NonNull View view) {
		remainingFiles = view.findViewById(R.id.remaining_files);
		AndroidUiHelper.updateVisibility(remainingFiles.findViewById(android.R.id.icon), false);
	}

	@Override
	public void onFileCopyStarted(String fileName) {
		TextView summary = remainingFiles.findViewById(android.R.id.summary);
		summary.setText(getString(R.string.copying_file, fileName));
	}

	@Override
	public void onRemainingFilesUpdate(List<DocumentFile> files) {
		String amount = String.valueOf(files.size());
		String formattedSize = "(" + AndroidUtils.formatSize(app, getFilesSize(documentFiles)) + ")";
		String warning = getString(R.string.files_remaining, amount, formattedSize);

		SpannableString spannable = new SpannableString(warning);
		int index = warning.indexOf(amount);
		spannable.setSpan(new CustomTypefaceSpan(FontCache.getRobotoMedium(app)), index, index + amount.length(), 0);
		index = warning.indexOf(formattedSize);
		spannable.setSpan(new ForegroundColorSpan(ColorUtilities.getSecondaryTextColor(app, nightMode)), index, index + formattedSize.length(), 0);

		TextView title = remainingFiles.findViewById(android.R.id.title);
		title.setText(spannable);
	}

	@Override
	public void onFilesCopyProgress(int progress) {
		progressBar.setProgress(progress);

		int maxProgress = progressBar.getMax();
		int percentage = maxProgress != 0 ? BasicProgressAsyncTask.normalizeProgress(progress * 100 / maxProgress) : 0;
		progressTitle.setText(getString(R.string.progress_complete, percentage + "%"));
	}

	@Override
	public void onFilesCopyFinished() {
		copyFinished = true;
		toolbar.setNavigationIcon(null);
		toolbar.setTitle(R.string.copying_completed);

		setupFilesTitle();
		progressBar.setProgress(progressBar.getMax());
		progressTitle.setText(getString(R.string.progress_complete, 100 + "%"));

		TextView title = remainingFiles.findViewById(android.R.id.title);
		title.setText(R.string.restart_is_required);

		AndroidUiHelper.updateVisibility(actionButton, true);
		AndroidUiHelper.updateVisibility(remainingFiles.findViewById(android.R.id.summary), false);
		UiUtilities.setupDialogButton(nightMode, actionButton, DialogButtonType.PRIMARY, R.string.shared_string_restart);
	}

	public static StorageMigrationFragment showInstance(@NonNull FragmentManager fragmentManager,
	                                                    @NonNull List<DocumentFile> documentFiles,
	                                                    boolean usedOnMap) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			StorageMigrationFragment fragment = new StorageMigrationFragment();
			fragment.usedOnMap = usedOnMap;
			fragment.documentFiles.addAll(documentFiles);
			fragment.setRetainInstance(true);
			fragment.show(fragmentManager, TAG);
			return fragment;
		}
		return null;
	}
}
