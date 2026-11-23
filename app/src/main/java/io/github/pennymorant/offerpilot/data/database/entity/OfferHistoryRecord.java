package io.github.pennymorant.offerpilot.data.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Structured offer push log for daily volume and quality-ratio analytics; never stores screenshots or raw OCR text. */
@Entity(
    tableName = "offer_history",
    indices = {
        @Index(value = {"localDate", "platform"}),
        @Index(value = {"observedAt"})
    }
)
public class OfferHistoryRecord {
    @PrimaryKey @NonNull public String offerId = "";
    @NonNull public String platform = "";
    public long observedAt;
    @NonNull public String localDate = "";
    public int localHour;
    @NonNull public String serviceKind = "";
    @NonNull public String channel = "";
    public boolean preferredTag;
    public boolean fakePreferred;
    public int itemCount;
    public long amountCents;
    public long distanceMeters;
    public long durationSeconds;
    public long centsPerMile;
    public long grossCentsPerHour;
    public int valueScore;
    @NonNull public String quality = "";
    public boolean eligible;
    public boolean accepted;
    @NonNull public String outcome = "PENDING";
    @NonNull public String merchant = "";
    @NonNull public String destinationArea = "";
}
