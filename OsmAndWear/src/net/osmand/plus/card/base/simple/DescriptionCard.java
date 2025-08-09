package net.osmand.plus.card.base.simple;

import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;

public class DescriptionCard extends BaseCard {

	private final CharSequence initialDescription;
	private TextView textView;

	public DescriptionCard(@NonNull FragmentActivity activity, @StringRes int stringRes) {
		this(activity, stringRes, true);
	}

	public DescriptionCard(@NonNull FragmentActivity activity, @StringRes int stringRes, boolean usedOnMap) {
		this(activity, activity.getString(stringRes), usedOnMap);
	}

	public DescriptionCard(@NonNull FragmentActivity activity, @NonNull CharSequence initialDescription) {
		this(activity, initialDescription, true);
	}

	public DescriptionCard(@NonNull FragmentActivity activity,
	                       @NonNull CharSequence initialDescription, boolean usedOnMap) {
		super(activity, usedOnMap);
		this.initialDescription = initialDescription;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.card_description;
	}

	@Override
	protected void updateContent() {
		textView = view.findViewById(R.id.summary);
		setText(initialDescription);
	}

	public void setText(@StringRes int textId) {
		textView.setText(textId);
	}

	public void setText(@NonNull CharSequence text) {
		textView.setText(text);
	}

}
