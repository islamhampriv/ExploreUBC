package com.exploreUBC.cpen321application.data.users

enum class TransportationMode(val apiValue: String, val displayName: String) {
    WALKING("walking", "Walking"),
    CYCLING("cycling", "Cycling"),
    TRANSIT("transit", "Transit"),
    DRIVING("driving", "Driving"),
    OTHER("other", "Other");

    companion object {
        fun fromApiValue(value: String): TransportationMode =
            entries.firstOrNull { it.apiValue == value }
                ?: throw IllegalArgumentException("Unknown transportation mode: $value")
    }
}

enum class BudgetPreference(val apiValue: String, val displayName: String) {
    ECONOMICAL("economical", "Economical"),
    BALANCED("balanced", "Balanced"),
    PREMIUM("premium", "Premium");

    companion object {
        fun fromApiValue(value: String): BudgetPreference =
            entries.firstOrNull { it.apiValue == value }
                ?: throw IllegalArgumentException("Unknown budget preference: $value")
    }
}

enum class InterestPreference(val apiValue: String, val displayName: String) {
    FOOD("food", "Food"),
    ART("art", "Art"),
    OUTDOORS("outdoors", "Outdoors"),
    MARKETS("markets", "Markets"),
    LIVE_MUSIC("live_music", "Live music"),
    GAMING("gaming", "Gaming");

    companion object {
        fun fromApiValue(value: String): InterestPreference =
            entries.firstOrNull { it.apiValue == value }
                ?: throw IllegalArgumentException("Unknown interest preference: $value")
    }
}

data class UserPreferences(
    val transportation: TransportationPreference,
    val budget: BudgetPreference,
    val interest: InterestPreference,
)

data class TransportationPreference(
    val mode: TransportationMode,
)

data class PreferencesUpdate(
    val transportation: TransportationPreference? = null,
    val budget: BudgetPreference? = null,
    val interest: InterestPreference? = null,
)

data class User(
    val googleSubject: String,
    val email: String,
    val displayName: String,
    val start_location: String,
    val preferences: UserPreferences,
    val create_at: String,
    val updated_at: String,
)
