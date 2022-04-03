package com.onionchat.common

// !!!!!!!!!!!!! ONLY ADD VALUES DOWNWARDS !!!!!!!!!!!!!!
enum class MessageTypes {
    // key exchange
    REQUEST_PUB_MESSAGE,
    RESPONSE_PUB_MESSAGE,

    // asymmetric encrypted messages
    SYM_KEY_MESSAGE,

    // symmetric encrypted messages
    TEXT_MESSAGE,
    BROADCAST_TEXT_MESSAGE,
    MESSAGE_READ_MESSAGE,
    INVALID_MESSAGE,
    ATTACHMENT_MESSAGE,

    // avatar
    REQUEST_CONTACT_DETAILS_MESSAGE,
    PROVIDE_CONTACT_DETAILS_MESSAGE,

    // requestable keys
    REQUEST_SYN_KEY_MESSAGE,
    PROVIDE_SYM_KEY_MESSAGE
    ;

    companion object {
        fun renderableMessages(): List<Int> {
            return listOf(SYM_KEY_MESSAGE.ordinal, TEXT_MESSAGE.ordinal, ATTACHMENT_MESSAGE.ordinal)
        }

        fun contentMessages(): List<Int> {
            return listOf(TEXT_MESSAGE.ordinal, ATTACHMENT_MESSAGE.ordinal)
        }

        fun shouldShowPush(type: Int): Boolean {
            return contentMessages().contains(type)
        }

        fun shouldBeShownInChat(type: Int): Boolean {
            return renderableMessages().contains(type)
        }
    }

}