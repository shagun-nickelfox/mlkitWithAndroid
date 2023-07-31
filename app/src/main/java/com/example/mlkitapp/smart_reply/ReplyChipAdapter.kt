package com.example.mlkitapp.smart_reply

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mlkitapp.databinding.SmartReplyChipBinding
import com.google.mlkit.nl.smartreply.SmartReplySuggestion

class ReplyChipAdapter(private val listener: ChipClickListener):RecyclerView.Adapter<ReplyChipAdapter.ReplyViewHolder>() {

    private val suggestions = ArrayList<SmartReplySuggestion>()

    inner class ReplyViewHolder(private val binding: SmartReplyChipBinding):RecyclerView.ViewHolder(binding.root){
        fun bind(suggestion: SmartReplySuggestion){
            binding.apply {
               binding.smartReplyText.text = suggestion.text
                itemView.setOnClickListener {
                    listener.onClick(suggestion.text)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReplyViewHolder {
        return ReplyViewHolder(SmartReplyChipBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun getItemCount(): Int {
        return suggestions.size
    }

    override fun onBindViewHolder(holder: ReplyViewHolder, position: Int) {
        val suggestion = suggestions[position]
        holder.bind(suggestion)
    }

    fun setSuggestions(suggestions: List<SmartReplySuggestion>) {
        this.suggestions.clear()
        this.suggestions.addAll(suggestions)
        notifyDataSetChanged()
    }

    interface ChipClickListener{
        fun onClick(chipText:String){}
    }
}