package net.osmand.plus.base;

public abstract class BottomSheetDialogFragment extends OsmAndSheetDialogFragment {
	
	@Override
	protected SheetDialogType getSheetDialogType()  {
		return SheetDialogType.BOTTOM;
	}
}
