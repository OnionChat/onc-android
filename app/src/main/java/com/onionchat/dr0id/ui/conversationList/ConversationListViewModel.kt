package com.onionchat.dr0id.ui.conversationList

import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.onionchat.dr0id.database.Conversation

enum class ListChangeAction {


    ITEMS_INSERTED,
    ITEMS_DELETED,
    ITEMS_CHANGED;
    var positionStart = 0;
    var itemCount = 0;
}



class ConversationListViewModel : ViewModel() {

    var conversations = MutableLiveData<ConversationList>()
    var conversationEvents = MutableLiveData<ListChangeAction>()

    init {
        conversations.value = ConversationList()
    }

    inner class ConversationList : ArrayList<Conversation>() {
        @MainThread
        override fun addAll(index: Int, elements: Collection<Conversation>): Boolean {
            val action = ListChangeAction.ITEMS_INSERTED
            action.positionStart = index
            action.itemCount = elements.size
            conversationEvents.value = action
            return super.addAll(index, elements)
        }

        @MainThread
        override fun addAll(elements: Collection<Conversation>): Boolean {
            val action = ListChangeAction.ITEMS_INSERTED
            action.positionStart = size
            action.itemCount = elements.size
            conversationEvents.value = action
            return super.addAll(elements)
        }

        @MainThread
        override fun removeAt(index: Int): Conversation {
            val action = ListChangeAction.ITEMS_DELETED
            action.positionStart = index
            action.itemCount = 1
            conversationEvents.value = action
            return super.removeAt(index)
        }

        @MainThread
        override fun add(element: Conversation): Boolean {
            val action = ListChangeAction.ITEMS_INSERTED
            action.positionStart = size
            action.itemCount = 1
            conversationEvents.value = action
            return super.add(element)
        }

        @MainThread
        override fun add(index: Int, element: Conversation) {
            val action = ListChangeAction.ITEMS_INSERTED
            action.positionStart = index
            action.itemCount = 1
            conversationEvents.value = action
            super.add(index, element)
        }
    }

    @MainThread
    fun notifyItemRangeChanged(startIndex:Int, size:Int) {
        val action = ListChangeAction.ITEMS_CHANGED
        action.positionStart = startIndex
        action.itemCount = size
        conversationEvents.value = action
    }
}