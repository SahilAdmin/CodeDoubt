package com.sahil_admin.codedoubt.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import com.getstream.sdk.chat.viewmodel.MessageInputViewModel
import com.getstream.sdk.chat.viewmodel.messages.MessageListViewModel
import com.sahil_admin.codedoubt.R
import com.sahil_admin.codedoubt.Utility
import com.sahil_admin.codedoubt.Utility.makeToast
import com.sahil_admin.codedoubt.databinding.ActivityChatBinding
import com.sahil_admin.codedoubt.objects.Doubt
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.ui.message.input.viewmodel.bindView
import io.getstream.chat.android.ui.message.list.MessageListView
import io.getstream.chat.android.ui.message.list.header.viewmodel.MessageListHeaderViewModel
import io.getstream.chat.android.ui.message.list.header.viewmodel.bindView
import io.getstream.chat.android.ui.message.list.viewmodel.bindView
import io.getstream.chat.android.ui.message.list.viewmodel.factory.MessageListViewModelFactory
import javax.inject.Inject

private const val TAG = "De_ChatActivity"

@AndroidEntryPoint
class ChatActivity: AppCompatActivity() {

    private val binding by lazy {ActivityChatBinding.inflate(layoutInflater)}
    private val doubt by lazy {intent.getParcelableExtra<Doubt>(Utility.DOUBT_CODE)!!}
    @Inject lateinit var client: ChatClient

    private val factory by lazy { MessageListViewModelFactory(doubt.doubtChannelId!!) }
    private var onThread = false
    private val messageListHeaderViewModel by viewModels<MessageListHeaderViewModel> { factory }
    private val messageListViewModel by viewModels<MessageListViewModel> { factory }
    private val messageInputViewModel by viewModels<MessageInputViewModel> { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        messageListHeaderViewModel.bindView(binding.messageListHeaderView, this)
        messageListViewModel.bindView(binding.messageListView, this)
        messageInputViewModel.bindView(binding.messageInputView, this)

        // for private thread
        messageListViewModel.mode.observe(this) { mode ->
            when(mode) {
                is MessageListViewModel.Mode.Thread -> {
                    messageListHeaderViewModel.setActiveThread(mode.parentMessage)
                    messageInputViewModel.setActiveThread(mode.parentMessage)
                    onThread = true
                }
                is MessageListViewModel.Mode.Normal -> {
                    messageListHeaderViewModel.resetThread()
                    messageInputViewModel.resetThread()
                    onThread = false
                }
            }
        }

        binding.messageListView.setMessageEditHandler(messageInputViewModel::postMessageToEdit)

        binding.messageListHeaderView.setBackButtonClickListener {
            messageListViewModel.bindView(binding.messageListView, this)
        }

        binding.messageListHeaderView.setBackButtonClickListener {
            onBackPressed()
        }

//        binding.messageListHeaderView.setAvatarClickListener {
//            AlertDialog.Builder(this).setMessage(R.string.doubt_solved_alert)
//                .setPositiveButton(R.string.yes) { _, _ ->
//                    solvedPressed()
//                }
//                .setNegativeButton(R.string.cancel) { _, _ ->
//                }
//                .create()
//                .show()
//        }

        binding.messageListView.setMessageFlagHandler(MessageListView.MessageFlagHandler {
            client.flagMessage(it.id).enqueue() {result ->
                if(result.isSuccess) {
                    makeToast("Message flagged Successfully")
                } else {
                    Log.d(TAG, result.error().toString());
                }
            }
        })
    }

    private fun solvedPressed() {
//        client!!.channel(doubt.doubtChannelId!!).updatePartial(
//            set = mapOf("image" to "https://img.icons8.com/emoji/512/green-circle-emoji.png")
//        )
    }

    override fun onBackPressed() {
        if(onThread) messageListViewModel.onEvent(MessageListViewModel.Event.BackButtonPressed)
        else super.onBackPressed()
    }
}