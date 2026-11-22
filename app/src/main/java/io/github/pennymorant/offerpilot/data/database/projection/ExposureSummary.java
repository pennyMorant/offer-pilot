package io.github.pennymorant.offerpilot.data.database.projection;

import androidx.annotation.NonNull;

public class ExposureSummary {
    @NonNull public String platform = "";
    @NonNull public String regionKey = "";
    public int localWeekday;
    public int localHour;
    public long bucketStart;
    public long exposureMs;
}
