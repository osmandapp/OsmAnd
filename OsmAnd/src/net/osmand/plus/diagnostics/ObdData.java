package net.osmand.plus.diagnostics;

// TODO(natashaj): change to data types rather than strings
public class ObdData {

    private String speed;
    private String fuelLevel;
    private String engineCoolantTemp;
    private String fuelEconomy;
    
    public String getSpeed() {
        return this.speed;
    }
    
    public void setSpeed(String speed) {
        this.speed = speed;
    }
    
    public String getFuelLevel() {
        return this.fuelLevel;
    }
    
    public void setFuelLevel(String fuelLevel) {
        this.fuelLevel = fuelLevel;
    }
    
    public String getEngineCoolantTemp() {
        return this.engineCoolantTemp;
    }
    
    public void setEngineCoolantTemp(String engineCoolantTemp) {
        this.engineCoolantTemp = engineCoolantTemp;
    }
    
    public String getFuelEconomy() {
        return this.fuelEconomy;
    }
    
    public void setFuelEconomy(String fuelEconomy) {
        this.fuelEconomy = fuelEconomy;
    }
}
