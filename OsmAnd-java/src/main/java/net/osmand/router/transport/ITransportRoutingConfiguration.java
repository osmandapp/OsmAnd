package net.osmand.router.transport;

public interface ITransportRoutingConfiguration {
    float getSpeedByRouteType(String routeType);

    int getChangeTime();

    int getBoardingTime();

    float getDefaultTravelSpeed();

    double getWalkSpeed();

    boolean getUseSchedule();

    int getMaxRouteTime();

    int getFinishTimeSeconds();

    double getMaxRouteDistance();

    double getMaxRouteIncreaseSpeed();

    int getStopTime();

    int getMaxNumberOfChanges();

    int getScheduleTimeOfDay();

    int getWalkRadius();
}
