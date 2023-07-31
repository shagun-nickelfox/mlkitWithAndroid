package com.example.mlkitapp.smart_reply

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.nl.smartreply.SmartReply
import com.google.mlkit.nl.smartreply.SmartReplySuggestion
import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult
import com.google.mlkit.nl.smartreply.TextMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.lang.Exception
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SmartReplyViewModel @Inject constructor():ViewModel(){

    private val remoteUserId = UUID.randomUUID().toString()

    private val suggestions =  MediatorLiveData<List<SmartReplySuggestion>>()
    private val emulatingRemoteUser = MutableLiveData<Boolean>()
    private val messageList = MutableLiveData<MutableList<Message>?>()
    private val smartReply = SmartReply.getClient()

    val messages: MutableLiveData<MutableList<Message>?>
        get() = messageList

    init {
        initSuggestionGenerator()
        emulatingRemoteUser.postValue(false)

    }
    fun getSuggestions(): LiveData<List<SmartReplySuggestion>> {
        return suggestions
    }

    fun getEmulatingRemoteUser(): LiveData<Boolean> {
        return emulatingRemoteUser
    }

    internal fun setMessages(messages: MutableList<Message>) {
        clearSuggestions()
        messageList.postValue(messages)
    }

    private fun initSuggestionGenerator() {
        suggestions.addSource(
            emulatingRemoteUser,
            Observer { isEmulatingRemoteUser ->
                val list = messageList.value
                if(list.isNullOrEmpty())
                    return@Observer

                generateReplies(list,isEmulatingRemoteUser).addOnSuccessListener { result ->
                    suggestions.postValue(result)
                }
            }
        )
        suggestions.addSource(messageList, Observer {list->
            val isEmulatingRemoteUser = emulatingRemoteUser.value
            if (isEmulatingRemoteUser == null || list!!.isEmpty()) {
                return@Observer
            }
            generateReplies(list, isEmulatingRemoteUser).addOnSuccessListener { result ->
                suggestions.postValue(result)
            }
        })
    }

    private fun generateReplies(
        messages:List<Message>,
        isEmulatingRemoteUser:Boolean): Task<List<SmartReplySuggestion>> {

        val lastMessage = messages[messages.size-1]

        // If the last message in the chat thread is not sent by the "other" user, don't generate
        // smart replies.
        if(lastMessage.isLocalUser != isEmulatingRemoteUser)
            return Tasks.forException(Exception("Not running smart reply!!"))

        val chatHistory = ArrayList<TextMessage>()
        for(message in messages) {
            if (lastMessage.isLocalUser != isEmulatingRemoteUser)
                chatHistory.add(TextMessage.createForLocalUser(message.text,message.timestamp))
            else
                chatHistory.add(TextMessage.createForRemoteUser(message.text,message.timestamp,remoteUserId))
        }
        return smartReply
            .suggestReplies(chatHistory)
            .continueWith {task ->
                val result = task.result
                when(result.status){
                    SmartReplySuggestionResult.STATUS_NOT_SUPPORTED_LANGUAGE-> {
                        // This error happens when the detected language is not English, as that is the
                        // only supported language in Smart Reply.
                      /*  Toast.makeText(, R.string.error_not_supported_language,
                            Toast.LENGTH_SHORT).show()*/
                    }
                    SmartReplySuggestionResult.STATUS_NO_REPLY->{
                        // This error happens when the inference completed successfully, but no replies
                        // were returned.
                       // Toast.makeText(application, R.string.error_no_reply, Toast.LENGTH_SHORT).show()
                    }
                    else->{}
                }
                result.suggestions
            }.addOnFailureListener {e->
               /* Toast.makeText(
                    application, "Smart reply error" + "\nError: " + e.localizedMessage +
                            "\nCause: " + e.cause,
                    Toast.LENGTH_LONG
                ).show()*/
            }
    }
    internal fun switchUser() {
        clearSuggestions()
        val value = emulatingRemoteUser.value!!
        emulatingRemoteUser.postValue(!value)
    }

    private fun clearSuggestions() {
        suggestions.postValue(ArrayList())
    }

    internal fun addMessage(message: String) {
        var list: MutableList<Message>? = messageList.value
        if (list == null) {
            list = ArrayList()
        }
        val value = emulatingRemoteUser.value!!
        list.add(Message(message, !value, System.currentTimeMillis()))
        clearSuggestions()
        messageList.postValue(list)
    }
}