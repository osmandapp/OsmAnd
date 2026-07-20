package net.osmand.plus.settings.fragments;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.search.dialogs.QuickSearchDialogFragment;
import net.osmand.plus.search.dialogs.QuickSearchHistoryFragment;
import net.osmand.plus.utils.AndroidUtils;

public class HistorySettingsDialogFragment extends BaseFullScreenDialogFragment {

	public static final String TAG = HistorySettingsDialogFragment.class.getSimpleName();
	private static final String DIALOG_HISTORY_SETTINGS_TAG = "dialog_history_settings";
	private final FragmentManager.FragmentLifecycleCallbacks childFragmentLifecycleCallbacks =
			new FragmentManager.FragmentLifecycleCallbacks() {
				@Override
				public void onFragmentViewCreated(@NonNull FragmentManager fm, @NonNull Fragment fragment,
				                                  @NonNull View view, @Nullable Bundle savedInstanceState) {
					if (isRootHistorySettingsFragment(fragment)) {
						setupHostedSettingsView(view);
					}
				}
			};

	@Nullable
	@Override
	public View onCreateView(@NonNull android.view.LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		FrameLayout view = new FrameLayout(requireContext());
		view.setId(R.id.fragmentContainer);
		view.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		return view;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		FragmentManager fragmentManager = getChildFragmentManager();
		fragmentManager.registerFragmentLifecycleCallbacks(childFragmentLifecycleCallbacks, false);
		Fragment fragment = fragmentManager.findFragmentByTag(DIALOG_HISTORY_SETTINGS_TAG);
		if (fragment == null) {
			fragment = new HistorySettingsFragment();
			fragmentManager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, DIALOG_HISTORY_SETTINGS_TAG)
					.commitNow();
		}
		if (isRootHistorySettingsFragment(fragment)) {
			setupHostedSettingsView(fragment.getView());
		}
	}

	private boolean isRootHistorySettingsFragment(@NonNull Fragment fragment) {
		return DIALOG_HISTORY_SETTINGS_TAG.equals(fragment.getTag());
	}

	private void setupHostedSettingsView(@Nullable View view) {
		removeTopPadding(view);
		setupCloseButton(view);
	}

	private void removeTopPadding(@Nullable View view) {
		if (view != null && view.getPaddingTop() != 0) {
			view.setPadding(view.getPaddingLeft(), 0, view.getPaddingRight(), view.getPaddingBottom());
		}
	}

	private void setupCloseButton(@Nullable View view) {
		View closeButton = view != null ? view.findViewById(R.id.close_button) : null;
		if (closeButton != null) {
			closeButton.setOnClickListener(v -> dismissAllowingStateLoss());
		}
	}

	@Override
	public void onDestroyView() {
		getChildFragmentManager().unregisterFragmentLifecycleCallbacks(childFragmentLifecycleCallbacks);
		super.onDestroyView();
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		super.onDismiss(dialog);
		Fragment target = getTargetFragment();
		if (target instanceof QuickSearchDialogFragment quickSearchDialogFragment) {
			quickSearchDialogFragment.reloadHistory();
		} else if (target instanceof QuickSearchHistoryFragment quickSearchHistoryFragment) {
			quickSearchHistoryFragment.reloadHistory();
		}
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			HistorySettingsDialogFragment fragment = new HistorySettingsDialogFragment();
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}
}
