package com.onionchat.common


// TODO add tests

enum class MessageStatus(val status: Int) {
    NOT_SENT(0), // default
    SENT(2),
    FORWARDED(4),
    RECEIVED(8),
    READ(16),
    NOT_FORWARDED(32);


    companion object {
        fun hasFlag(status: Int, toCheck: MessageStatus): Boolean {
            if (status and toCheck.ordinal == toCheck.ordinal) {
                return true
            }
            return false
        }

        fun addFlag(currentStatus: Int, toBeAdded: MessageStatus): Int {
            return currentStatus or toBeAdded.ordinal
        }

        fun setFlags(vararg status: MessageStatus): Int {
            var intStatus = 0
            status.forEach {
                intStatus = intStatus or it.status
            }
            return intStatus
        }

        fun dumpState(status: Int): String {
            var state = "{"
            values().forEach {
                if (status and it.ordinal == it.ordinal) {
                    state += it.name + ", "
                }
            }
            return "$state} ($status)";
        }
    }


}