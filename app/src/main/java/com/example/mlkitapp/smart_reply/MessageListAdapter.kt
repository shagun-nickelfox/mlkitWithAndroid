package com.example.mlkitapp.smart_reply

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.example.mlkitapp.databinding.ItemMessageLocalBinding
import com.example.mlkitapp.databinding.ItemMessageRemoteBinding

class MessageListAdapter: RecyclerView.Adapter<MessageListAdapter.MessageViewHolder>() {
    var messageList = ArrayList<Message>()

    var emulatingRemoteUser = false
        set(emulatingRemoteUser) {
            field = emulatingRemoteUser
            notifyDataSetChanged()
        }

    class MessageViewHolder(private val binding: ViewBinding):RecyclerView.ViewHolder(binding.root){
        fun bind(message:Message){
            when(binding){
                is ItemMessageLocalBinding-> {binding.messageText.text = message.text}
                is ItemMessageRemoteBinding ->{binding.messageText.text = message.text}
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
      val binding =  when(viewType){
          1-> ItemMessageLocalBinding.inflate(LayoutInflater.from(parent.context),parent,false)
          else ->ItemMessageRemoteBinding.inflate(LayoutInflater.from(parent.context),parent,false)
      }
        return MessageViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messageList[position]
        holder.bind(message)
    }

    override fun getItemViewType(position: Int): Int {
        return if (
            messageList[position].isLocalUser && !emulatingRemoteUser ||
            !messageList[position].isLocalUser && emulatingRemoteUser
        ) 1 else 0
    }

    fun setMessages(messages: List<Message>) {
        messageList.clear()
        messageList.addAll(messages)
        notifyDataSetChanged()
    }
}