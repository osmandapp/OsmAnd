package net.osmand.plus.openplacereviews;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;

public class OprStartFragment extends BaseOsmAndFragment {

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_opr_login, container, false);
		return v;
	}
}
