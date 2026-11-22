package io.github.pennymorant.offerpilot.data.database.projection;

public class WaitingSummary {
    public String platform;
    public String regionKey;
    public String zoneId;
    public int weekday;
    public int localHour;
    public int measurementVersion;
    public long completedCount;
    public long interruptedCount;
    public long completedMillisSum;
}
