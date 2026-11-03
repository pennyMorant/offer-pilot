package com.overud.ui.components

data class OpportunityCardState(
    val id: String,
    val title: String,
    val subtitle: String,
    val score: Int?,
    val goodRate: Int?,
    val offersPerHour: Double?,
    val averageMile: String,
    val averageHour: String,
    val samples: String,
    val confidence: String,
    val advice: String,
    val knownRegion: Boolean,
    val proximity: String,
    val distanceMiles: Double?,
)
