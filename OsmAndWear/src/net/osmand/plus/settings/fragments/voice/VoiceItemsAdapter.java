package net.osmand.plus.settings.fragments.voice;

import static net.osmand.plus.utils.UiUtilities.CompoundButtonType.GLOBAL;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class VoiceItemsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private static final int TYPE_HEADER = 0;
	private static final int TYPE_VOICE_ITEM = 1;

	private final List<Object> items = new ArrayList<>();
	private final VoiceItemsListener listener;
	private final boolean nightMode;
	private VoiceType voiceType = VoiceType.TTS;

	public VoiceItemsAdapter(@NonNull VoiceItemsListener listener, boolean nightMode) {
		this.listener = listener;
		this.nightMode = nightMode;
	}

	public void setVoiceType(@NonNull VoiceType voiceType) {
		this.voiceType = voiceType;
	}

	public void setVoiceItems(@NonNull List<IndexItem> indexItems) {
		items.clear();
		items.add(TYPE_HEADER);
		items.addAll(indexItems);
		notifyDataSetChanged();
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		Context context = parent.getContext();
		LayoutInflater inflater = UiUtilities.getInflater(parent.getContext(), nightMode);
		switch (viewType) {
			case TYPE_VOICE_ITEM:
				View view = inflater.inflate(R.layout.list_item_icon_and_download, parent, false);

				AndroidUtils.setBackground(view, UiUtilities.getSelectableDrawable(context));
				UiUtilities.setupCompoundButton(view.findViewById(R.id.compound_button), nightMode, GLOBAL);

				AndroidUiHelper.updateVisibility(view.findViewById(R.id.icon), false);
				AndroidUiHelper.updateVisibility(view.findViewById(R.id.divider), false);

				return new VoiceItemViewHolder(view);
			case TYPE_HEADER:
				return new HeaderViewHolder(inflater.inflate(R.layout.voice_lang_header, parent, false));
			default:
				throw new IllegalArgumentException("Unsupported view type " + viewType);
		}
	}

	@Override
	public int getItemViewType(int position) {
		Object object = items.get(position);
		if (object instanceof IndexItem) {
			return TYPE_VOICE_ITEM;
		} else if (object instanceof Integer) {
			int item = (Integer) object;
			if (TYPE_HEADER == item) {
				return TYPE_HEADER;
			}
		}
		throw new IllegalArgumentException("Unsupported view type");
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
		if (viewHolder instanceof HeaderViewHolder) {
			HeaderViewHolder holder = (HeaderViewHolder) viewHolder;
			holder.bindView(voiceType, listener, nightMode);
		} else if (viewHolder instanceof VoiceItemViewHolder) {
			IndexItem item = (IndexItem) items.get(position);
			VoiceItemViewHolder holder = (VoiceItemViewHolder) viewHolder;
			holder.bindView(item, listener, nightMode);
		}
	}

	public void updateItem(@NonNull Object item) {
		int index = items.indexOf(item);
		if (index != -1) {
			notifyItemChanged(index);
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	public interface VoiceItemsListener {

		boolean isItemSelected(@NonNull IndexItem item);

		void onItemClicked(@NonNull IndexItem item);

		void onVoiceTypeSelected(@NonNull VoiceType voiceType);
	}
}
