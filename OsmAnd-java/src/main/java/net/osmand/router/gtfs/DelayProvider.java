package net.osmand.router.gtfs;

/**
 * Hook for real-time delays (GTFS-RT, later). Stop positions are 0-based indices along the trip as
 * in {@link GtfsIndex#getTripStop(int, int)}. The default provider reports no delay.
 */
public interface DelayProvider {

	DelayProvider NONE = new DelayProvider() {
		@Override
		public int departureDelaySec(int tripIdx, int stopSeq) {
			return 0;
		}

		@Override
		public int arrivalDelaySec(int tripIdx, int stopSeq) {
			return 0;
		}
	};

	int departureDelaySec(int tripIdx, int stopSeq);

	int arrivalDelaySec(int tripIdx, int stopSeq);
}
