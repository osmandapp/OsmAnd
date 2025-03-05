package net.osmand.plus.configmap;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.utils.AndroidUtils;

public class CoordinatesGridFragment extends BaseOsmAndFragment {

	public static final String TAG = CoordinatesGridFragment.class.getSimpleName();

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
	                         @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		return inflate(R.layout.fragment_coordinates_grid, container);
	}

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			manager.beginTransaction()
					.replace(R.id.content, new CoordinatesGridFragment(), TAG)
					.commitAllowingStateLoss();
		}
	}
}