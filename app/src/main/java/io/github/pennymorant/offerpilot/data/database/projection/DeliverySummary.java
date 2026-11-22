package io.github.pennymorant.offerpilot.data.database.projection;

public class DeliverySummary {
    public String platform;
    public String serviceKind;
    public String regionKey;
    public String zoneId;
    public int weekday;
    public int localHour;
    public int measurementVersion;
    public long completedCount;
    public long interruptedCount;
    public long measuredCount;
    public long observedMillisSum;
    public long pairedCount;
    public long pairedObservedMillisSum;
    public long pairedQuotedMillisSum;
    public long longerThanQuoteCount;
}
