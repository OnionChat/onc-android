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
    INVALID_MESSAGE
}