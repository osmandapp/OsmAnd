package net.osmand.plus.configmap;

import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;

import org.threeten.bp.Duration;

import de.KnollFrank.lib.settingssearch.results.PositionOfSettingProvider;
import de.KnollFrank.lib.settingssearch.results.Setting;
import de.KnollFrank.lib.settingssearch.results.SettingHighlighter;
import de.KnollFrank.lib.settingssearch.results.ViewHighlighter;

// FK-TODO: DRY with ItemOfLinearLayoutHighlighter
public class ItemOfLinearLayoutHighlighter2 implements SettingHighlighter {

	private final LinearLayout itemsContainer;
	private final PositionOfSettingProvider positionOfSettingProvider;
	private final Duration highlightDuration;

	public ItemOfLinearLayoutHighlighter2(final LinearLayout itemsContainer,
										  final PositionOfSettingProvider positionOfSettingProvider,
										  final Duration highlightDuration) {
		this.itemsContainer = itemsContainer;
		this.positionOfSettingProvider = positionOfSettingProvider;
		this.highlightDuration = highlightDuration;
	}

	@Override
	public void highlightSetting(final Fragment settingsFragment, final Setting setting) {
		highlightItem(positionOfSettingProvider.getPositionOfSetting(setting).orElseThrow());
	}

	private void highlightItem(final int itemPosition) {
		new Handler().post(() -> _highlightItem(itemPosition));
	}

	private void _highlightItem(final int itemPosition) {
		// itemsContainer.scrollToPosition(itemPosition);
		final View view = itemsContainer.getChildAt(itemPosition);
		if (view != null) {
			view.postDelayed(
					() -> ViewHighlighter.highlightView(view, highlightDuration),
					200);
		}
	}
}
