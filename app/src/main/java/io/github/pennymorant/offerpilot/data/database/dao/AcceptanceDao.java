package io.github.pennymorant.offerpilot.data.database.dao;

import androidx.room.*;
import io.github.pennymorant.offerpilot.data.database.entity.AttemptRecord;

@Dao
public abstract class AcceptanceDao {
    @Query("SELECT * FROM acceptance_state WHERE slot = 'active'")
    public abstract AttemptRecord active();
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void save(AttemptRecord attempt);
    @Transaction
    public long reserve(AttemptRecord attempt) {
        AttemptRecord previous = active();
        if (previous != null && (previous.offerId.equals(attempt.offerId) ||
            previous.state.equals("dispatching") || previous.state.equals("outcome_unknown") ||
            previous.state.equals("accepted"))) return -1;
        save(attempt);
        return 1;
    }
    @Query("UPDATE acceptance_state SET state = :state, detail = :detail WHERE offerId = :id")
    public abstract void updateAttempt(String id, String state, String detail);
    @Query("SELECT COUNT(*) FROM acceptance_state WHERE state IN ('dispatching', 'outcome_unknown', 'accepted')")
    public abstract int unresolvedCount();
    @Query("UPDATE acceptance_state SET state = 'user_resolved' WHERE state IN ('dispatching', 'outcome_unknown', 'accepted')")
    public abstract void resolve();
}
