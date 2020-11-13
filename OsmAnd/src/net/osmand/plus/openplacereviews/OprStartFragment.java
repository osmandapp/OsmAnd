package net.osmand.plus.openplacereviews;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;

public class OprStartFragment extends BaseOsmAndFragment {
	private static String openPlaceReviewsUrl = "OpenPlaceReviews.org";

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_opr_login, container, false);
		v.findViewById(R.id.register_opr_create_account).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent i = new Intent(requireContext(), OPRWebviewActivity.class);
				i.putExtra(OPRWebviewActivity.KEY_TITLE, getString(R.string.register_opr_create_new_account));
				i.putExtra(OPRWebviewActivity.KEY_LOGIN, false);
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
				Intent i = new Intent(requireContext(), OPRWebviewActivity.class);
				i.putExtra(OPRWebviewActivity.KEY_TITLE, getString(R.string.user_login));
				i.putExtra(OPRWebviewActivity.KEY_LOGIN, true);
				startActivity(i);
			}
		});
		setURLSpan(v);
		return v;
	}

	private void setURLSpan(View v) {
		String desc = requireContext().getString(R.string.register_on_openplacereviews_desc);
		SpannableString ss = new SpannableString(desc);
		ss.setSpan(new URLSpanNoUnderline("https://" + openPlaceReviewsUrl), desc.indexOf(openPlaceReviewsUrl),
				desc.indexOf(openPlaceReviewsUrl) + openPlaceReviewsUrl.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
		v.<TextView>findViewById(R.id.start_opr_description).setText(ss);
		v.<TextView>findViewById(R.id.start_opr_description).setMovementMethod(LinkMovementMethod.getInstance());
	}


	private class URLSpanNoUnderline extends URLSpan {
		public URLSpanNoUnderline(String url) {
			super(url);
		}

		@Override
		public void updateDrawState(TextPaint ds) {
			super.updateDrawState(ds);
			ds.setUnderlineText(false);
		}
	}

}
