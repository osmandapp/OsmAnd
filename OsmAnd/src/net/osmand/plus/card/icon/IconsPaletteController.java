package net.osmand.plus.card.icon;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.fragments.profileappearance.IconsPaletteFragment;
import net.osmand.plus.utils.ColorUtilities;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public abstract class IconsPaletteController<IconData> implements IIconsPaletteController<IconData> {

	protected final OsmandApplication app;
	protected final List<WeakReference<IIconsPalette<IconData>>> palettes = new ArrayList<>();

	protected List<IconData> icons;
	protected IconData selectedIcon;
	protected OnIconsPaletteListener<IconData> listener;

	public IconsPaletteController(@NonNull OsmandApplication app,
	                              @NonNull List<IconData> icons, @Nullable IconData selectedIcon) {
		this(app);
		this.icons = icons;
		this.selectedIcon = selectedIcon;
	}

	public IconsPaletteController(@NonNull OsmandApplication app) {
		this.app = app;
	}

	@Override
	public void bindPalette(@NonNull IIconsPalette<IconData> palette) {
		this.palettes.add(new WeakReference<>(palette));
	}

	@Override
	public void unbindPalette(@NonNull IIconsPalette<IconData> palette) {
		WeakReference<IIconsPalette<IconData>> referenceToRemove = null;
		for (WeakReference<IIconsPalette<IconData>> reference : palettes) {
			if (Objects.equals(palette, reference.get())) {
				referenceToRemove = reference;
				break;
			}
		}
		if (referenceToRemove != null) {
			palettes.remove(referenceToRemove);
		}
	}

	public void setIcons(@NonNull List<IconData> icons) {
		this.icons = icons;
	}

	public void setSelectedIcon(@NonNull IconData selectedIcon) {
		this.selectedIcon = selectedIcon;
	}

	public void askUpdateColoredPaletteElements() {
		notifyUpdatePaletteColors();
	}

	protected void notifyUpdatePaletteColors() {
		for (IIconsPalette<IconData> palette : collectActivePalettes()) {
			palette.updatePaletteColors();
		}
	}

	protected void notifyUpdatePaletteSelection(@Nullable IconData oldIcon, @NonNull IconData newIcon) {
		for (IIconsPalette<IconData> palette : collectActivePalettes()) {
			palette.updatePaletteSelection(oldIcon, newIcon);
		}
	}

	@Override
	public void setPaletteListener(@NonNull OnIconsPaletteListener<IconData> onIconsPaletteListener) {
		this.listener = onIconsPaletteListener;
	}

	@Override
	public int getIconsAccentColor(boolean nightMode) {
		return getControlsAccentColor(nightMode);
	}

	@Override
	public int getControlsAccentColor(boolean nightMode) {
		return ColorUtilities.getActiveColor(app, nightMode);
	}

	@Override
	public boolean isAccentColorCanBeChanged() {
		return false;
	}

	@Override
	public void onSelectIconFromPalette(@NonNull IconData icon) {
		if (!Objects.equals(selectedIcon, icon)) {
			IconData oldSelectedIcon = selectedIcon;
			selectIcon(icon);
			notifyUpdatePaletteSelection(oldSelectedIcon, selectedIcon);
		}
	}

	@Override
	public void selectIcon(@NonNull IconData iconId) {
		selectedIcon = iconId;
		onIconSelected(iconId);
	}

	protected void onIconSelected(@NonNull IconData icon) {
		if (listener != null) {
			listener.onIconSelectedFromPalette(icon);
		}
	}

	@Override
	public void onAllIconsButtonClicked(@NonNull FragmentActivity activity) {
		IconsPaletteFragment.showInstance(activity, this);
	}

	@Override
	public IconData getSelectedIcon() {
		return selectedIcon;
	}

	@Override
	public boolean isSelectedIcon(@NonNull IconData icon) {
		return Objects.equals(selectedIcon, icon);
	}

	@Override
	public int getHorizontalIconsSpace() {
		return getDimen(R.dimen.content_padding_small_half);
	}

	@Override
	public int getRecycleViewHorizontalPadding() {
		return getDimen(R.dimen.content_padding);
	}

	protected int getDimen(@DimenRes int id){
		return app.getResources().getDimensionPixelSize(id);
	}

	@NonNull
	@Override
	public List<IconData> getIcons() {
		return icons;
	}

	@NonNull
	private List<IIconsPalette<IconData>> collectActivePalettes() {
		List<IIconsPalette<IconData>> result = new ArrayList<>();
		Iterator<WeakReference<IIconsPalette<IconData>>> it = palettes.iterator();
		while (it.hasNext()) {
			IIconsPalette<IconData> palette = it.next().get();
			if (palette != null) {
				result.add(palette);
			} else {
				it.remove();
			}
		}
		return result;
	}
}
