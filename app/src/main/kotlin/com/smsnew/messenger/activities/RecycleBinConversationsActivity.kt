package com.smsnew.messenger.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import com.smsnew.messenger.R
import com.smsnew.messenger.adapters.RecycleBinConversationsAdapter
import com.smsnew.messenger.commonsLibCustom.dialogs.ConfirmationDialog
import com.smsnew.messenger.commonsLibCustom.extensions.*
import com.smsnew.messenger.commonsLibCustom.helpers.NavigationIcon
import com.smsnew.messenger.commonsLibCustom.helpers.ensureBackgroundThread
import com.smsnew.messenger.databinding.ActivityRecycleBinConversationsBinding
import com.smsnew.messenger.extensions.config
import com.smsnew.messenger.extensions.conversationsDB
import com.smsnew.messenger.extensions.emptyMessagesRecycleBin
import com.smsnew.messenger.helpers.IS_RECYCLE_BIN
import com.smsnew.messenger.helpers.THREAD_ID
import com.smsnew.messenger.helpers.THREAD_TITLE
import com.smsnew.messenger.models.Conversation
import com.smsnew.messenger.models.Events
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class RecycleBinConversationsActivity : SimpleActivity() {
    private var bus: EventBus? = null
    private val binding by viewBinding(ActivityRecycleBinConversationsBinding::inflate)

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupOptionsMenu()

        setupEdgeToEdge(padBottomImeAndSystem = listOf(binding.conversationsList))
        setupMaterialScrollListener(
            scrollingView = binding.conversationsList,
            topAppBar = binding.recycleBinAppbar
        )

        loadRecycleBinConversations()
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.recycleBinAppbar, NavigationIcon.Arrow)
        loadRecycleBinConversations()
        binding.conversationsFastscroller.updateColors(getProperAccentColor())
    }

    override fun onDestroy() {
        super.onDestroy()
        bus?.unregister(this)
    }

    private fun setupOptionsMenu() {
        binding.recycleBinToolbar.inflateMenu(R.menu.recycle_bin_menu)
        binding.recycleBinToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.empty_recycle_bin -> removeAll()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun updateOptionsMenu(conversations: ArrayList<Conversation>) {
        binding.recycleBinToolbar.menu.apply {
            findItem(R.id.empty_recycle_bin).isVisible = conversations.isNotEmpty()
        }
    }

    private fun loadRecycleBinConversations() {
        ensureBackgroundThread {
            val conversations = try {
                conversationsDB.getAllWithMessagesInRecycleBin()
                    .toMutableList() as ArrayList<Conversation>
            } catch (_: Exception) {
                ArrayList()
            }

            runOnUiThread {
                setupConversations(conversations)
            }
        }

        bus = EventBus.getDefault()
        try {
            bus!!.register(this)
        } catch (_: Exception) {
        }
    }

    private fun removeAll() {
        ConfirmationDialog(
            activity = this,
            message = "",
            messageId = R.string.empty_recycle_bin_messages_confirmation,
            positive = R.string.yes,
            negative = R.string.no
        ) {
            ensureBackgroundThread {
                emptyMessagesRecycleBin()
                loadRecycleBinConversations()
            }
        }
    }

    private fun getOrCreateConversationsAdapter(): RecycleBinConversationsAdapter {
        if (isDynamicTheme() && !isSystemInDarkMode()) {
            binding.conversationsList.setBackgroundColor(getSurfaceColor())
        }

        var currAdapter = binding.conversationsList.adapter
        if (currAdapter == null) {
            hideKeyboard()
            currAdapter = RecycleBinConversationsAdapter(
                activity = this,
                recyclerView = binding.conversationsList,
                onRefresh = { notifyDatasetChanged() },
                itemClick = { handleConversationClick(it) }
            )

            binding.conversationsList.adapter = currAdapter
            if (areSystemAnimationsEnabled) {
                binding.conversationsList.scheduleLayoutAnimation()
            }
        } else {
            (currAdapter as RecycleBinConversationsAdapter).addBottomPadding(64)
        }
        return currAdapter as RecycleBinConversationsAdapter
    }

    private fun setupConversations(conversations: ArrayList<Conversation>) {
        val sortedConversations = conversations.sortedWith(
            compareByDescending<Conversation> { config.pinnedConversations.contains(it.threadId.toString()) }
                .thenByDescending { it.date }
        ).toMutableList() as ArrayList<Conversation>

        showOrHidePlaceholder(conversations.isEmpty())
        updateOptionsMenu(conversations)

        try {
            getOrCreateConversationsAdapter().apply {
                updateConversations(sortedConversations)
            }
        } catch (_: Exception) {
        }
    }

    private fun showOrHidePlaceholder(show: Boolean) {
        binding.conversationsFastscroller.beGoneIf(show)
        binding.noConversationsPlaceholder.beVisibleIf(show)
        binding.noConversationsPlaceholder.text = getString(R.string.no_conversations_found)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun notifyDatasetChanged() {
        getOrCreateConversationsAdapter().notifyDataSetChanged()
    }

    private fun handleConversationClick(any: Any) {
        Intent(this, ThreadActivity::class.java).apply {
            val conversation = any as Conversation
            putExtra(THREAD_ID, conversation.threadId)
            putExtra(THREAD_TITLE, conversation.title)
            putExtra(IS_RECYCLE_BIN, true)
            startActivity(this)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refreshConversations(@Suppress("unused") event: Events.RefreshConversations) {
        loadRecycleBinConversations()
    }
}
