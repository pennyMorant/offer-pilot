package com.overud.data.preferences

import android.content.Context
import com.overud.model.Platform
import org.json.JSONObject

data class TrackerCheckpoint(val identity: String, val token: String, val firstAt: Long, val lastAt: Long)

/** At most one fingerprint and timestamps per platform, without prices or addresses. */
class TrackerCheckpointStore(context: Context) {
    private val preferences = context.getSharedPreferences("capture_checkpoint", Context.MODE_PRIVATE)

    fun read(platform: Platform): TrackerCheckpoint? = runCatching {
        val value = JSONObject(preferences.getString(platform.name, null) ?: return null)
        TrackerCheckpoint(value.getString("identity"), value.getString("token"), value.getLong("first"), value.getLong("last"))
    }.getOrNull()

    fun write(platform: Platform, checkpoint: TrackerCheckpoint) {
        val value = JSONObject().put("identity", checkpoint.identity).put("token", checkpoint.token)
            .put("first", checkpoint.firstAt).put("last", checkpoint.lastAt)
        preferences.edit().putString(platform.name, value.toString()).commit()
    }

    fun clear(platform: Platform) { preferences.edit().remove(platform.name).commit() }
}
