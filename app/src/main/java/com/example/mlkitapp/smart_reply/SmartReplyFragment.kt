package com.example.mlkitapp.smart_reply

import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.mlkitapp.R
import com.example.mlkitapp.databinding.FragmentSmartReplyBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SmartReplyFragment : Fragment() {
    private lateinit var binding: FragmentSmartReplyBinding
    private lateinit var viewModel: SmartReplyViewModel
    private lateinit var chatAdapter: MessageListAdapter
    private lateinit var chipAdapter: ReplyChipAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSmartReplyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[SmartReplyViewModel::class.java]
        initRecyclerView()
        initListeners()
        initObservers()
    }

    private fun initObservers() {
        binding.apply {
            viewModel.getSuggestions().observe(
                viewLifecycleOwner
            ) { suggestions -> chipAdapter.setSuggestions(suggestions!!) }

            viewModel.messages.observe(
                viewLifecycleOwner
            ) { messages ->
                chatAdapter.setMessages(messages!!)
                if (chatAdapter.itemCount > 0) {
                    chatHistory.smoothScrollToPosition(chatAdapter.itemCount - 1)
                }
            }

            viewModel.getEmulatingRemoteUser().observe(
                viewLifecycleOwner
            ) { isEmulatingRemoteUser ->
                if (isEmulatingRemoteUser!!) {
                    switchText.setText(R.string.chatting_as_red)
                    switchText.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            android.R.color.holo_red_dark
                        )
                    )
                } else {
                    switchText.setText(R.string.chatting_as_blue)
                    switchText.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            android.R.color.holo_blue_dark
                        )
                    )
                }
            }

            // Only set initial message for the new ViewModel instance.
            if (viewModel.messages.value == null) {
                val messageList = ArrayList<Message>()
                messageList.add(Message("Hello. How are you?", false, System.currentTimeMillis()))
                viewModel.setMessages(messageList)
            }
        }
    }

    private fun initListeners() {
        binding.apply {
            switchEmulatedUser.setOnClickListener {
                chatAdapter.emulatingRemoteUser = !chatAdapter.emulatingRemoteUser
                viewModel.switchUser()
            }
            button.setOnClickListener {
                val input = inputText.text.toString()
                if (TextUtils.isEmpty(input))
                    return@setOnClickListener

                viewModel.addMessage(input)
                inputText.text = null
            }
        }
    }

    private fun initRecyclerView() {
        binding.apply {
            chatAdapter = MessageListAdapter()
            chipAdapter = ReplyChipAdapter(object : ReplyChipAdapter.ChipClickListener {
                override fun onClick(chipText: String) {
                    inputText.setText(chipText)
                }
            })
            chatHistory.adapter = chatAdapter
            smartRepliesRecycler.adapter = chipAdapter
        }
    }
}