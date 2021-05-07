package net.osmand.plus.measurementtool;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.settings.backend.ApplicationMode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static net.osmand.plus.routing.TransportRoutingHelper.PUBLIC_TRANSPORT_KEY;

public class ProfileCard extends BaseCard {

	private ApplicationMode selectedMode;
	private ProfileCardListener listener;

	public ProfileCard(@NonNull MapActivity mapActivity, @NonNull ApplicationMode selectedMode) {
		super(mapActivity);
		this.selectedMode = selectedMode;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.navigation_profiles_card;
	}

	@Override
	protected void updateContent() {
		final List<ApplicationMode> modes = new ArrayList<>(ApplicationMode.values(app));
		modes.remove(ApplicationMode.DEFAULT);
		Iterator<ApplicationMode> iterator = modes.iterator();
		while (iterator.hasNext()) {
			ApplicationMode mode = iterator.next();
			if (PUBLIC_TRANSPORT_KEY.equals(mode.getRoutingProfile())) {
				iterator.remove();
			}
		}
		for (int i = 0; i < modes.size(); i++) {
			ApplicationMode mode = modes.get(i);
			LinearLayout container = view.findViewById(R.id.content_container);
			Drawable icon = app.getUIUtilities().getPaintedIcon(mode.getIconRes(), mode.getProfileColor(nightMode));
			String title = mode.toHumanString();
			View.OnClickListener onClickListener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					RadioButton selectedProfile = v.findViewById(R.id.compound_button);
					selectedMode = ApplicationMode.valueOfStringKey((String) v.getTag(), ApplicationMode.CAR);
					clearChecked();
					selectedProfile.setChecked(true);
					if (listener != null) {
						listener.onProfileSelect(selectedMode);
					}
				}

				private void clearChecked() {
					for (int i = 0; i < modes.size(); i++) {
						RadioButton profile = view.findViewWithTag(modes.get(i).getStringKey())
								.findViewById(R.id.compound_button);
						profile.setChecked(false);
					}
				}
			};
			addProfileView(container, onClickListener, mode.getStringKey(), icon, title);
		}
		resetSelected(modes);
	}

	private void resetSelected(List<ApplicationMode> modes) {
		View profileView = view.findViewWithTag(selectedMode.getStringKey());
		if (profileView != null) {
			((RadioButton) profileView.findViewById(R.id.compound_button)).setChecked(true);
		}
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
