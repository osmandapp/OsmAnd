package net.osmand.plus.utils;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsCompat.Type.InsetsType;

import net.osmand.plus.utils.InsetsUtils.InsetSide;

import java.util.Arrays;
import java.util.EnumSet;

public final class InsetTarget {

	public enum Type {ROOT_INSET, BOTTOM_CONTAINER, FAB, COLLAPSING_APPBAR, SCROLLABLE, SIDED, CUSTOM, LANDSCAPE_SIDES}

	private final int[] viewIds;
	private final View[] views;
	private final Type type;
	private final EnumSet<InsetSide> portraitSides;
	private final EnumSet<InsetsUtils.InsetSide> landscapeSides;
	@InsetsType
	private final int typeMask;

	private final boolean clipToPadding;
	private final boolean adjustHeight;
	private final boolean adjustWidth;
	private final boolean preferMargin;
	private final boolean applyPadding;

	private InsetTarget(InsetTargetBuilder b) {
		this.viewIds = b.viewIds;
		this.views = b.views;
		this.type = b.type;
		this.typeMask = b.typeMask;
		this.applyPadding = b.applyPadding;
		this.portraitSides = b.portraitSides != null ? b.portraitSides : EnumSet.noneOf(InsetsUtils.InsetSide.class);;
		this.landscapeSides = b.landscapeSides != null ? b.landscapeSides : EnumSet.noneOf(InsetsUtils.InsetSide.class);;
		this.clipToPadding = b.clipToPadding;
		this.adjustHeight = b.adjustHeight;
		this.adjustWidth = b.adjustWidth;
		this.preferMargin = b.preferMargin;
	}

	public int[] getViewIds() {
		return viewIds;
	}

	public Type getType() {
		return type;
	}

	public View[] getViews() {
		return views;
	}

	public boolean isPreferMargin() {
		return preferMargin;
	}

	public boolean isAdjustHeight() {
		return adjustHeight;
	}

	public boolean isAdjustWidth() {
		return adjustWidth;
	}

	public boolean isClipToPadding() {
		return clipToPadding;
	}

	public int getTypeMask() {
		return typeMask;
	}

	public boolean isApplyPadding() {
		return applyPadding;
	}

	@Nullable
	public EnumSet<InsetsUtils.InsetSide> getSides(boolean isLandscape) {
		return isLandscape ? landscapeSides : portraitSides;
	}

	public static InsetTargetBuilder createFab(int... ids) {
		return builder(ids)
				.type(Type.FAB)
				.portraitSides(InsetsUtils.InsetSide.BOTTOM)
				.landscapeSides(InsetsUtils.InsetSide.BOTTOM, InsetsUtils.InsetSide.LEFT, InsetsUtils.InsetSide.RIGHT)
				.typeMask(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime() | WindowInsetsCompat.Type.displayCutout())
				.preferMargin(true)
				.clipToPadding(false)
				.adjustHeight(false);
	}

	public static InsetTargetBuilder createScrollable(int... ids) {
		return builder(ids)
				.type(Type.SCROLLABLE)
				.portraitSides(InsetSide.BOTTOM)
				.landscapeSides(InsetSide.BOTTOM, InsetSide.LEFT, InsetSide.RIGHT)
				.typeMask(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime() | WindowInsetsCompat.Type.displayCutout())
				.clipToPadding(true)
				.applyPadding(true);
	}

	public static InsetTargetBuilder createScrollable(View... ids) {
		return builder(ids)
				.type(Type.SCROLLABLE)
				.portraitSides(InsetSide.BOTTOM)
				.landscapeSides(InsetSide.BOTTOM, InsetSide.LEFT, InsetSide.RIGHT)
				.typeMask(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime() | WindowInsetsCompat.Type.displayCutout())
				.clipToPadding(true)
				.applyPadding(true);
	}

	public static InsetTargetBuilder createBottomContainer(int... ids) {
		return builder(ids)
				.type(Type.BOTTOM_CONTAINER)
				.portraitSides(InsetSide.BOTTOM)
				.landscapeSides(InsetSide.BOTTOM, InsetSide.LEFT, InsetSide.RIGHT)
				.clipToPadding(true)
				.applyPadding(true)
				.adjustHeight(true)
				.typeMask(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime() | WindowInsetsCompat.Type.displayCutout());
	}

	public static InsetTargetBuilder createCollapsingAppBar(int... ids) {
		return builder(ids)
				.type(Type.COLLAPSING_APPBAR)
				.portraitSides(InsetsUtils.InsetSide.TOP)
				.landscapeSides(InsetsUtils.InsetSide.TOP);
	}

	public static InsetTargetBuilder createLeftSideContainer(boolean leftSided, int... ids) {
		return createLeftSideContainer(leftSided, false, ids);

	}

	public static InsetTargetBuilder createLeftSideContainer(boolean leftSided, boolean applyPadding, int... ids) {
		return builder(ids)
				.type(Type.SIDED)
				.portraitSides()
				.landscapeSides()
				.typeMask(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime() | WindowInsetsCompat.Type.displayCutout())
				.landscapeLeftSided(leftSided)
				.adjustWidth(true)
				.applyPadding(applyPadding);
	}

	public static InsetTargetBuilder createLeftSideContainer(boolean leftSided, View... ids) {
		return createLeftSideContainer(leftSided, false, ids);
	}

	public static InsetTargetBuilder createLeftSideContainer(boolean leftSided, boolean applyPadding, View... ids) {
		return builder(ids)
				.type(Type.SIDED)
				.portraitSides()
				.landscapeSides()
				.typeMask(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime() | WindowInsetsCompat.Type.displayCutout())
				.landscapeLeftSided(leftSided)
				.adjustWidth(true)
				.applyPadding(applyPadding);
	}

	public static InsetTargetBuilder createHorizontalLandscape(Boolean leftSided, int... ids) {
		return builder(ids)
				.type(Type.LANDSCAPE_SIDES)
				.portraitSides()
				.landscapeSides(InsetSide.LEFT, InsetSide.RIGHT)
				.landscapeLeftSided(leftSided)
				.applyPadding(true)
				.typeMask(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime() | WindowInsetsCompat.Type.displayCutout());
	}

	public static InsetTargetBuilder createHorizontalLandscape(Boolean leftSided, View... ids) {
		return builder(ids)
				.type(Type.LANDSCAPE_SIDES)
				.portraitSides()
				.landscapeSides(InsetSide.LEFT, InsetSide.RIGHT)
				.landscapeLeftSided(leftSided)
				.applyPadding(true)
				.typeMask(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime() | WindowInsetsCompat.Type.displayCutout());
	}

	public static InsetTargetBuilder createHorizontalLandscape(View... ids) {
		return createHorizontalLandscape(null, ids);
	}

	public static InsetTargetBuilder createHorizontalLandscape(int... ids) {
		return createHorizontalLandscape(null, ids);
	}

	public static InsetTargetBuilder createCustomBuilder(int... ids) {
		return builder(ids)
				.type(Type.CUSTOM);
	}

	public static InsetTargetBuilder createCustomBuilder(View... views) {
		return builder(views)
				.type(Type.CUSTOM);
	}

	public static InsetTargetBuilder createRootInset() {
		return builder(0)
				.type(Type.ROOT_INSET)
				.portraitSides(InsetsUtils.InsetSide.TOP)
				.landscapeSides(InsetsUtils.InsetSide.TOP);
	}

	public static InsetTargetBuilder builder(int... viewIds) {
		return new InsetTargetBuilder(viewIds, null);
	}

	public static InsetTargetBuilder builder(View... views) {
		return new InsetTargetBuilder(null, views);
	}

	public static class InsetTargetBuilder {
		private final int[] viewIds;
		private final View[] views;
		private Type type = Type.CUSTOM;
		@InsetsType
		private int typeMask = WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime() | WindowInsetsCompat.Type.displayCutout();
		private EnumSet<InsetsUtils.InsetSide> portraitSides;
		private EnumSet<InsetsUtils.InsetSide> landscapeSides;
		private boolean clipToPadding;
		private boolean adjustHeight;
		private boolean adjustWidth;
		private boolean preferMargin;
		private Boolean landscapeLeftSided = null;
		private boolean applyPadding;

		private InsetTargetBuilder(int[] viewIds, View[] views) {
			this.viewIds = viewIds;
			this.views = views;
		}

		public InsetTargetBuilder type(@NonNull Type type) {
			this.type = type;
			return this;
		}

		public InsetTargetBuilder portraitSides(InsetsUtils.InsetSide... sides) {
			this.portraitSides = sides.length > 0
					? EnumSet.copyOf(Arrays.asList(sides))
					: EnumSet.noneOf(InsetsUtils.InsetSide.class);
			return this;
		}

		public InsetTargetBuilder typeMask(@InsetsType int typeMask) {
			this.typeMask = typeMask;
			return this;
		}

		public InsetTargetBuilder landscapeSides(InsetsUtils.InsetSide... sides) {
			this.landscapeSides = sides.length > 0
					? EnumSet.copyOf(Arrays.asList(sides))
					: EnumSet.noneOf(InsetsUtils.InsetSide.class);
			return this;
		}

		public InsetTargetBuilder clipToPadding(boolean value) {
			this.clipToPadding = value;
			return this;
		}

		public InsetTargetBuilder adjustHeight(boolean value) {
			this.adjustHeight = value;
			return this;
		}

		public InsetTargetBuilder adjustWidth(boolean value) {
			this.adjustWidth = value;
			return this;
		}

		public InsetTargetBuilder preferMargin(boolean value) {
			this.preferMargin = value;
			return this;
		}

		public InsetTargetBuilder landscapeLeftSided(@Nullable Boolean leftSided) {
			this.landscapeLeftSided = leftSided;
			return this;
		}

		public InsetTargetBuilder applyPadding(boolean applyPadding) {
			this.applyPadding = applyPadding;
			return this;
		}

		public InsetTarget build() {
			if (landscapeLeftSided != null) {
				if (landscapeLeftSided) {
					landscapeSides.add(InsetSide.LEFT);
					landscapeSides.remove(InsetSide.RIGHT);
				} else {
					landscapeSides.remove(InsetSide.LEFT);
					landscapeSides.add(InsetSide.RIGHT);
				}
			}
			return new InsetTarget(this);
		}
	}
}
