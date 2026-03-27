package net.osmand.router;

public class FastRoutingState {
    public enum Status {
        READY,

        // MissingMapsCalculator
        MIXED_MAPS_INTERMEDIATES,
        MISSING_MAPS_INTERMEDIATES,
        MIXED_MAPS_AT_START_OR_END,
        MISSING_MAPS_AT_START_OR_END,

        // HHRoutePlanner
        FAILED_WITH_MIXED_MAPS,
        FAILED_WITH_MISSING_MAPS,
        FAILED_NO_HH_ROUTING_DATA, // pedestrian profile, ancient maps, etc
        FAILED_WITHOUT_MAP_ISSUES, // unsupported parameters, unusual geometry (Roma to Barcelona), etc

        CANCELLED,
        SUCCESS
    }

    public static boolean isSuccessStatus(Status status) {
        return status == Status.SUCCESS;
    }

    public static boolean isCancelledStatus(Status status) {
        return status == Status.CANCELLED;
    }

    public static boolean isFailedStatus(Status status) {
        return status == Status.FAILED_WITH_MIXED_MAPS
                || status == Status.FAILED_WITH_MISSING_MAPS
                || status == Status.FAILED_NO_HH_ROUTING_DATA
                || status == Status.FAILED_WITHOUT_MAP_ISSUES;
    }

    protected static Status get(int ordinal) {
        return Status.values()[ordinal];
    }

    protected static int reset() {
        return Status.READY.ordinal();
    }

    protected static int raise(int old, Status status) {
        return Math.max(status.ordinal(), old);
    }

    protected static int fail(int old) {
        if (isMixedMaps(old)) {
            return raise(old, Status.FAILED_WITH_MIXED_MAPS);
        } else if (isMissingMaps(old)) {
            return raise(old, Status.FAILED_WITH_MISSING_MAPS);
        } else {
            return raise(old, Status.FAILED_WITHOUT_MAP_ISSUES);
        }
    }

    protected static boolean isMixedOrMissingMaps(int ordinal) {
        return isMixedMaps(ordinal) || isMissingMaps(ordinal);
    }

    protected static boolean isSlowRoutingActive(int ordinal) {
        return isFailedStatus(get(ordinal));
    }

    private static boolean isMixedMaps(int ordinal) {
        return ordinal == Status.FAILED_WITH_MIXED_MAPS.ordinal()
                || ordinal == Status.MIXED_MAPS_INTERMEDIATES.ordinal()
                || ordinal == Status.MIXED_MAPS_AT_START_OR_END.ordinal();
    }

    private static boolean isMissingMaps(int ordinal) {
        return ordinal == Status.FAILED_WITH_MISSING_MAPS.ordinal()
                || ordinal == Status.MISSING_MAPS_INTERMEDIATES.ordinal()
                || ordinal == Status.MISSING_MAPS_AT_START_OR_END.ordinal();
    }
}
