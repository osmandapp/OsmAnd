package net.osmand.plus.openplacereviews;

import android.content.Intent;
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
		v.findViewById(R.id.register_opr_create_account).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent i = new Intent(requireContext(),OPRWebviewActivity.class);
				i.putExtra(OPRWebviewActivity.KEY_TITLE,getString(R.string.register_opr_create_new_account));
				i.putExtra(OPRWebviewActivity.KEY_LOGIN,false);
				startActivity(i);
			}
		});
		v.findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				getActivity().getSupportFragmentManager().popBackStack();
			}
		});
		v.findViewById(R.id.register_opr_have_account).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent i = new Intent(requireContext(),OPRWebviewActivity.class);
				i.putExtra(OPRWebviewActivity.KEY_TITLE,getString(R.string.user_login));
				i.putExtra(OPRWebviewActivity.KEY_LOGIN,true);
				startActivity(i);
			}
		});
		return v;
	}
}
