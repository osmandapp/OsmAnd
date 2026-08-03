package net.osmand.plus.settings.fragments.voice;

import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.fragments.voice.VoiceItemsAdapter.VoiceItemsListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;

class HeaderViewHolder extends RecyclerView.ViewHolder {

	private final OsmandApplication app;

	private final TextView title;
	private final TextView description;
	private final TextView typeDescription;
	private final LinearLayout voiceTypeButtons;

	HeaderViewHolder(@NonNull View itemView) {
		super(itemView);
		app = (OsmandApplication) itemView.getContext().getApplicationContext();
		title = itemView.findViewById(R.id.title);
		description = itemView.findViewById(R.id.description);
		typeDescription = itemView.findViewById(R.id.voice_type_description);
		voiceTypeButtons = itemView.findViewById(R.id.voice_type_buttons);
	}

	public void bindView(@NonNull VoiceType voiceType, @NonNull VoiceItemsListener listener, boolean nightMode) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.bottomMargin = app.getResources().getDimensionPixelSize(R.dimen.bottom_sheet_title_padding_top);

		title.setText(R.string.shared_string_language);
		title.setLayoutParams(params);

		description.setText(R.string.language_description);
		description.setTextColor(AndroidUtils.getColorFromAttr(app, android.R.attr.textColorPrimary));

		TextRadioItem ttsButton = createRadioButton(VoiceType.TTS, listener);
		TextRadioItem recordedButton = createRadioButton(VoiceType.RECORDED, listener);

		TextToggleButton toggleButton = new TextToggleButton(app, voiceTypeButtons, nightMode);
		toggleButton.setItems(ttsButton, recordedButton);
		toggleButton.setSelectedItem(voiceType == VoiceType.TTS ? ttsButton : recordedButton);

		typeDescription.setTextSize(TypedValue.COMPLEX_UNIT_PX, app.getResources().getDimensionPixelSize(R.dimen.default_list_text_size));
		typeDescription.setText(voiceType.descriptionRes);
	}

	@NonNull
	private TextRadioItem createRadioButton(@NonNull VoiceType voiceType, @NonNull VoiceItemsListener listener) {
		TextRadioItem item = new TextRadioItem(app.getString(voiceType.titleRes));
		item.setOnClickListener((radioItem, view) -> {
			listener.onVoiceTypeSelected(voiceType);
			return true;
		});
		return item;
	}
}
