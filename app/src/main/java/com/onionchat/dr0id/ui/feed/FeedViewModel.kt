package com.onionchat.dr0id.ui.feed

import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.onionchat.dr0id.database.Conversation
import com.onionchat.dr0id.messaging.IMessage

enum class ListChangeAction {


    ITEMS_INSERTED,
    ITEMS_DELETED,
    ITEMS_CHANGED;
    var positionStart = 0;
    var itemCount = 0;
}



class FeedViewModel : ViewModel() {

    var feed = MutableLiveData<MessageList>()
    var messageEvents = MutableLiveData<ListChangeAction>()

    init {
        feed.value = MessageList()
    }

    inner class MessageList : ArrayList<IMessage>() {
        @MainThread
        override fun addAll(index: Int, elements: Collection<IMessage>): Boolean {
            val action = ListChangeAction.ITEMS_INSERTED
            action.positionStart = index
            action.itemCount = elements.size
            messageEvents.value = action
            return super.addAll(index, elements)
        }

        @MainThread
        override fun addAll(elements: Collection<IMessage>): Boolean {
            val action = ListChangeAction.ITEMS_INSERTED
            action.positionStart = size
            action.itemCount = elements.size
            messageEvents.value = action
            return super.addAll(elements)
        }

        @MainThread
        override fun removeAt(index: Int): IMessage {
            val action = ListChangeAction.ITEMS_DELETED
            action.positionStart = index
            action.itemCount = 1
            messageEvents.value = action
            return super.removeAt(index)
        }

        @MainThread
        override fun add(element: IMessage): Boolean {
            val action = ListChangeAction.ITEMS_INSERTED
            action.positionStart = size
            action.itemCount = 1
            messageEvents.value = action
            return super.add(element)
        }

        @MainThread
        override fun add(index: Int, element: IMessage) {
            val action = ListChangeAction.ITEMS_INSERTED
            action.positionStart = index
            action.itemCount = 1
            messageEvents.value = action
            super.add(index, element)
        }
    }

    @MainThread
    fun notifyItemRangeChanged(startIndex:Int, size:Int) {
        val action = ListChangeAction.ITEMS_CHANGED
        action.positionStart = startIndex
        action.itemCount = size
        messageEvents.value = action
    }
}