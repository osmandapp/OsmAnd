package net.osmand.plus.activities.actions;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import java.text.MessageFormat;
import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.helpers.FontCache;

public class OsmandRestoreOrExitDialog extends BottomSheetDialogFragment {

	private String clientAppTitle = "";

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater()
				.inflate(R.layout.dash_restore_osmand_fragment, container, false);
		try {
			clientAppTitle = getMyApplication().getAppCustomization().getNavFooterAppName();
		} catch (Exception e) {
			e.printStackTrace();
		}

		String msg = MessageFormat.format(getString(R.string.run_full_osmand_msg), clientAppTitle);
		Typeface typeface = FontCache.getRobotoMedium(getActivity());
		Typeface typefaceReg = FontCache.getRobotoRegular(getActivity());
		TextView header = view.findViewById(R.id.run_full_osmand_header);
		header.setTypeface(typeface);

		TextView message = view.findViewById(R.id.run_full_osmand_message);
		message.setTypeface(typefaceReg);
		message.setText(msg);

		Button cancelBtn = view.findViewById(R.id.cancel_full_osmand_btn);
		cancelBtn.setTypeface(typeface);
		cancelBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		Button launchBtn = view.findViewById(R.id.launch_full_osmand_btn);
		launchBtn.setTypeface(typeface);
		launchBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				getMyApplication().getAppCustomization().restoreOsmand();
				dismiss();
			}
		});

		return view;
	}
}
