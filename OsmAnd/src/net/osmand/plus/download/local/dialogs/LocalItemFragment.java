package net.osmand.plus.download.local.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.LocalItemsHolder;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class LocalItemFragment extends LocalBaseFragment {

	private static final String TAG = LocalItemFragment.class.getSimpleName();

	private LocalItem localItem;

	@Nullable
	@Override
	public LocalItemsHolder getItemsHolder() {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof LocalCategoriesFragment) {
			return ((LocalCategoriesFragment) fragment).getItemsHolder();
		}
		return null;
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.local_item_fragment, container, false);
		setupContent(view);

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();

		DownloadActivity activity = getDownloadActivity();
		ActionBar actionBar = activity != null ? activity.getSupportActionBar() : null;
		if (actionBar != null) {
			actionBar.setTitle(localItem.getName());
		}
	}

	private void setupContent(@NonNull View view) {
		ViewGroup container = view.findViewById(R.id.container);

		String type = getString(localItem.getType().getTitleId());
		addRow(container, getString(R.string.shared_string_type), type, false);

		DateFormat format = new SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault());
		String date = format.format(localItem.getFile().lastModified());
		addRow(container, getString(R.string.shared_string_created), date, false);

		String size = AndroidUtils.formatSize(app, localItem.getSize());
		addRow(container, getString(R.string.shared_string_size), size, true);
	}

	private void addRow(@NonNull ViewGroup container, String title, String description, boolean lastItem) {
		View view = themedInflater.inflate(R.layout.local_item_row, container, false);
		container.addView(view);

		TextView tvTitle = view.findViewById(R.id.title);
		tvTitle.setText(title);

		TextView tvDescription = view.findViewById(R.id.description);
		tvDescription.setText(description);

		AndroidUiHelper.updateVisibility(view.findViewById(R.id.bottom_divider), !lastItem);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.bottom_shadow), lastItem);
	}

	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		menu.clear();
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull LocalItem localItem, @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			LocalItemFragment fragment = new LocalItemFragment();
			fragment.localItem = localItem;
			fragment.setRetainInstance(true);
			fragment.setTargetFragment(target, 0);

			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}
