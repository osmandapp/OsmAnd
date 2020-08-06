package net.osmand.plus.measurementtool;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.settings.backend.ApplicationMode;

import java.util.ArrayList;
import java.util.List;

public class ProfileCard extends BaseCard {

	private ApplicationMode selectedMode;
	private ProfileCardListener listener;

	public ProfileCard(MapActivity mapActivity) {
		super(mapActivity);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.navigation_profiles_card;
	}

	@Override
	protected void updateContent() {

		final List<ApplicationMode> modes = new ArrayList<>(ApplicationMode.values(app));
		modes.remove(ApplicationMode.DEFAULT);
		for (int i = 0; i < modes.size(); i++) {
			ApplicationMode mode = modes.get(i);
			LinearLayout container = view.findViewById(R.id.content_container);
			Drawable icon = app.getUIUtilities().getIcon(mode.getIconRes(), mode.getIconColorInfo().getColor(nightMode));
			String title = mode.toHumanString();
			View.OnClickListener onClickListener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					RadioButton selectedProfile = v.findViewById(R.id.compound_button);
					selectedMode = modes.get((Integer) v.getTag());
					clearChecked();
					selectedProfile.setChecked(true);
					if (listener != null) {
						listener.onProfileSelect(selectedMode);
					}
				}

				private void clearChecked() {
					for (int i = 0; i < modes.size(); i++) {
						RadioButton profile = view.findViewWithTag(i).findViewById(R.id.compound_button);
						profile.setChecked(false);
					}
				}
			};
			addProfileView(container, onClickListener, i, icon, title);
		}
		resetSelected(modes);
	}

	private void resetSelected(List<ApplicationMode> modes) {
		selectedMode = modes.get(0);
		((RadioButton) view.findViewWithTag(0).findViewById(R.id.compound_button)).setChecked(true);
	}

	private void addProfileView(LinearLayout container, View.OnClickListener onClickListener, Object tag,
	                            Drawable icon, CharSequence title) {
		View row = UiUtilities.getInflater(mapActivity, nightMode)
				.inflate(R.layout.bottom_sheet_item_with_radio_btn, container, false);
		ImageView imageView = row.findViewById(R.id.icon);
		imageView.setImageDrawable(icon);
		ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) imageView.getLayoutParams();
		params.rightMargin = container.getContext().getResources().getDimensionPixelSize(R.dimen.bottom_sheet_icon_margin_large);
		((TextView) row.findViewById(R.id.title)).setText(title);
		row.setOnClickListener(onClickListener);
		row.setTag(tag);
		container.addView(row);
	}

	public void setListener(ProfileCardListener listener) {
		this.listener = listener;
	}

	interface ProfileCardListener {

		void onProfileSelect(ApplicationMode applicationMode);

	}
}
