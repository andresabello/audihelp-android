package com.audihelp.app.domain.enums

enum class EarSide(val value: String, val label: String) {
    LEFT("left", "Left"),
    RIGHT("right", "Right"),
    BOTH("both", "Both"),
}

enum class TrialResponse(val value: String, val label: String) {
    HEAR_CLEARLY("hear_clearly", "Hear Clearly"),
    BARELY_HEAR("barely_hear", "Barely Hear"),
    FEEL("feel", "Feel"),
    NOTHING("nothing", "Nothing"),
}

enum class PatternType(val value: String, val label: String) {
    TONE("tone", "Tone"),
    PULSE("pulse", "Pulse"),
    AM("am", "AM"),
}

enum class SessionStatus(val value: String) {
    PENDING("pending"),
    IN_PROGRESS("in_progress"),
    COMPLETED("completed"),
    ABANDONED("abandoned"),
}

enum class EqFilterType(val value: String, val label: String) {
    PEAK("peak", "Peak"),
    LOW_SHELF("lowshelf", "Lo Shelf"),
    HIGH_SHELF("highshelf", "Hi Shelf"),
    LOW_PASS("lowpass", "Lo Pass"),
    HIGH_PASS("highpass", "Hi Pass"),
    NOTCH("notch", "Notch"),
}
