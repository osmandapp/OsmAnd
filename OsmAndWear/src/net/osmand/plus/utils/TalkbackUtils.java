package net.osmand.plus.utils;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.util.List;

public class TalkbackUtils {
	static public void setActivityViewsAccessibility(@NonNull View v, boolean hideActivity, @NonNull TalkbackHandler talkbackHandler) {
		if (hideActivity != talkbackHandler.isActivityHiddenForTalkback()) {
			talkbackHandler.setActivityHiddenForTalkback(hideActivity);
			setViewAccessibility(v, hideActivity);
		}
	}

	static private void setViewAccessibility(@NonNull View v, boolean hideActivity) {
		ViewGroup viewgroup = (ViewGroup) v;
		for (int i = 0; i < viewgroup.getChildCount(); i++) {
			View v1 = viewgroup.getChildAt(i);
			if (v1 instanceof ViewGroup) {
				setViewAccessibility(v1, hideActivity);
			} else {
				v1.setImportantForAccessibility(hideActivity ? View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS : View.IMPORTANT_FOR_ACCESSIBILITY_YES);
			}
		}
	}

	static public FragmentManager.FragmentLifecycleCallbacks getLifecycleCallbacks(@NonNull TalkbackHandler talkbackHandler) {
		return new FragmentManager.FragmentLifecycleCallbacks() {
			@Override
			public void onFragmentViewCreated(@NonNull FragmentManager fm, @NonNull Fragment f, @NonNull View v, @Nullable Bundle savedInstanceState) {
				super.onFragmentViewCreated(fm, f, v, savedInstanceState);
				List<Fragment> activeTalkbackFragments = talkbackHandler.getActiveTalkbackFragments();

				for (int i = 0; i < activeTalkbackFragments.size(); i++) {
					if (i != activeTalkbackFragments.size() - 1) {
						View fragmentView = activeTalkbackFragments.get(i).getView();
						if (fragmentView != null) {
							fragmentView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
						}
					} else {
						View fragmentView = activeTalkbackFragments.get(i).getView();
						if (fragmentView != null) {
							fragmentView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
						}
					}
				}
				if (!activeTalkbackFragments.isEmpty()) {
					talkbackHandler.setActivityAccessibility(true);
				}
			}

			@Override
			public void onFragmentDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
				super.onFragmentDestroyed(fm, f);
				List<Fragment> fragments = talkbackHandler.getActiveTalkbackFragments();
				if (!fragments.isEmpty()) {
					View topFragmentView = fragments.get(fragments.size() - 1).getView();
					if (topFragmentView != null) {
						topFragmentView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
					}

				} else {
					talkbackHandler.setActivityAccessibility(false);
				}
			}
		};
	}

	public interface TalkbackHandler {
		List<Fragment> getActiveTalkbackFragments();
		void setActivityAccessibility(boolean hideActivity);
		boolean isActivityHiddenForTalkback();
		void setActivityHiddenForTalkback(boolean hideActivity);
	}
}
