package net.osmand.plus.configmap;

import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.card.base.multistate.IMultiStateCard;
import net.osmand.plus.card.base.multistate.IMultiStateCardController;

public abstract class MultiStateColorPaletteController extends MapColorPaletteController implements IMultiStateCardController {

	public interface IStateUIProvider {
		void bindCustomStateContent(@NonNull FragmentActivity activity, @NonNull ViewGroup container);
	}

	protected IMultiStateCard cardInstance;
	private IStateUIProvider uiProvider;

	public MultiStateColorPaletteController(@NonNull OsmandApplication app,
	                                        @ColorInt int initialColorDay,
	                                        @ColorInt int initialColorNight) {
		super(app, initialColorDay, initialColorNight);
	}

	public void setUiProvider(IStateUIProvider uiProvider) {
		this.uiProvider = uiProvider;
	}

	@Override
	public void bindComponent(@NonNull IMultiStateCard cardInstance) {
		this.cardInstance = cardInstance;
	}

	protected void notifyCardStateChanged() {
		if (cardInstance != null) {
			cardInstance.updateSelectedCardState();
		}
	}

	@Override
	public boolean shouldShowCardHeader() {
		return true;
	}

	public abstract boolean hasSelector();

	@Override
	public void onBindCardContent(@NonNull FragmentActivity activity,
	                              @NonNull ViewGroup container,
	                              boolean nightMode,
	                              boolean usedOnMap) {
		container.removeAllViews();
		if (uiProvider != null) {
			uiProvider.bindCustomStateContent(activity, container);
		}
	}
}