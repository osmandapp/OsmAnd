package net.osmand.plus.settings.fragments.configureitems.viewholders;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.plugins.PluginsFragment;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;

public class CustomizationDescriptionViewHolder extends RecyclerView.ViewHolder {

	private final FragmentActivity activity;
	private final TextView description;

	public CustomizationDescriptionViewHolder(@NonNull View itemView, @NonNull FragmentActivity activity) {
		super(itemView);
		this.activity = activity;

		description = itemView.findViewById(R.id.description);
		AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.image), false);
	}

	public void bindView(@NonNull String text, boolean nightMode) {
		String plugins = activity.getString(R.string.prefs_plugins);
		SpannableString spannable = UiUtilities.createClickableSpannable(text, plugins, unused -> {
			PluginsFragment.showInstance(activity.getSupportFragmentManager());
			return true;
		});
		Typeface typeface = FontCache.getRobotoMedium(activity);
		UiUtilities.setSpan(spannable, new CustomTypefaceSpan(typeface), text, plugins);
		UiUtilities.setupClickableText(description, spannable, nightMode);
	}
}
