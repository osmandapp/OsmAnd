package net.osmand.plus.mapmarkers;

import android.content.Context;

import net.osmand.plus.R;

public class CoordinateInputFormats {

	public enum Format {

		DD_MM_MMM(R.string.dd_mm_mmm_format, true, 2, 3, "°", "."),
		DD_MM_MMMM(R.string.dd_mm_mmmm_format, true, 2, 4, "°", "."),
		DD_DDDDD(R.string.dd_ddddd_format, false, 5, 0, ".", "°"),
		DD_DDDDDD(R.string.dd_dddddd_format, false, 6, 0, ".", "°"),
		DD_MM_SS(R.string.dd_mm_ss_format, true, 2, 2, "°", "′");

		private final int humanStringId;
		private final boolean containsThirdPart;
		private final int secondPartSymbolsCount;
		private final int thirdPartSymbolsCount;
		private final String firstSeparator;
		private final String secondSeparator;

		Format(int humanStringId,
			   boolean containsThirdPart,
			   int secondPartSymbolsCount,
			   int thirdPartSymbolsCount,
			   String firstSeparator,
			   String secondSeparator) {
			this.humanStringId = humanStringId;
			this.containsThirdPart = containsThirdPart;
			this.secondPartSymbolsCount = secondPartSymbolsCount;
			this.thirdPartSymbolsCount = thirdPartSymbolsCount;
			this.firstSeparator = firstSeparator;
			this.secondSeparator = secondSeparator;
		}

		public String toHumanString(Context context) {
			return context.getString(humanStringId);
		}

		public boolean isContainsThirdPart() {
			return containsThirdPart;
		}

		public int getFirstPartSymbolsCount(boolean latitude, boolean twoDigitsLongitude) {
			if (latitude) {
				return 2;
			}
			return twoDigitsLongitude ? 2 : 3;
		}

		public int getSecondPartSymbolsCount() {
			return secondPartSymbolsCount;
		}

		public int getThirdPartSymbolsCount() {
			return thirdPartSymbolsCount;
		}

		public String getFirstSeparator() {
			return firstSeparator;
		}

		public String getSecondSeparator() {
			return secondSeparator;
		}
	}

	public static DDM ddToDdm(double decimalDegrees) {
		DDM res = new DDM();
		res.degrees = (int) decimalDegrees;
		res.decimalMinutes = (decimalDegrees - res.degrees) * 60;
		return res;
	}

	public static DMS ddToDms(double decimalDegrees) {
		DDM ddm = ddToDdm(decimalDegrees);
		DMS res = new DMS();
		res.degrees = ddm.degrees;
		res.minutes = (int) ddm.decimalMinutes;
		res.seconds = (ddm.decimalMinutes - res.minutes) * 60;
		return res;
	}

	public static class DDM {
		public int degrees;
		public double decimalMinutes;
	}

	public static class DMS {
		public int degrees;
		public int minutes;
		public double seconds;
	}
}
