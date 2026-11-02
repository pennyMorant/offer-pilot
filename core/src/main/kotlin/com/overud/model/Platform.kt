package com.overud.model

enum class Platform(val label: String, val packageName: String) {
    UBER_EATS("Uber Eats", "com.ubercab.driver"),
    DOORDASH("DoorDash", "com.doordash.driverapp");
    companion object { fun fromPackage(value: String) = entries.find { it.packageName == value } }
}
