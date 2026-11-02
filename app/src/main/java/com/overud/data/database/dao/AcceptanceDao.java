package com.overud.data.database.dao;

import androidx.room.*;
import com.overud.data.database.entity.*;

@Dao
public interface AcceptanceDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) long reserve(AttemptRecord attempt);
    @Query("UPDATE acceptance_attempts SET state = :state, detail = :detail WHERE offerId = :id")
    void updateAttempt(String id, String state, String detail);
    @Query("SELECT COUNT(*) FROM acceptance_attempts WHERE state IN ('dispatching', 'outcome_unknown', 'accepted')") int unresolvedCount();
    @Query("UPDATE acceptance_attempts SET state = 'user_resolved' WHERE state IN ('dispatching', 'outcome_unknown', 'accepted')") void resolve();
}
