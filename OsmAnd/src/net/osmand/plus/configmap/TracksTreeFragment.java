package net.osmand.plus.configmap;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class TracksTreeFragment extends Fragment {

	public static final String TAG = TracksTreeFragment.class.getSimpleName();

	String groupName;

	public void setGroupName(String name){
		groupName = name;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		TextView text = new TextView(getActivity());
		text.setText(groupName);
		text.setGravity(Gravity.CENTER);

		return text;
	}
}
