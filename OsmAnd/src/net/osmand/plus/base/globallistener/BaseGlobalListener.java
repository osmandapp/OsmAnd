package net.osmand.plus.base.globallistener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseGlobalListener {

	private AppCompatActivity activity;

	public void setActivity(@Nullable AppCompatActivity activity) {
		this.activity = activity;
	}

	@NonNull
	protected List<Fragment> getAddedFragments() {
		List<Fragment> res = new ArrayList<>();
		if (activity != null) {
			List<Fragment> allFragments = activity.getSupportFragmentManager().getFragments();
			for (Fragment fragment : allFragments) {
				if (fragment.isAdded()) {
					res.add(fragment);
				}
			}
		}
		return res;
	}
}
