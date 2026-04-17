package com.smsnew.messenger.dialogs

import android.view.*
import androidx.appcompat.widget.PopupMenu
import com.smsnew.messenger.R
import com.smsnew.messenger.commonsLibCustom.activities.BaseSimpleActivity
import com.smsnew.messenger.commonsLibCustom.adapters.MyRecyclerViewAdapter
import com.smsnew.messenger.commonsLibCustom.extensions.copyToClipboard
import com.smsnew.messenger.commonsLibCustom.extensions.getPopupMenuTheme
import com.smsnew.messenger.commonsLibCustom.extensions.getProperTextColor
import com.smsnew.messenger.commonsLibCustom.extensions.setupViewBackground
import com.smsnew.messenger.commonsLibCustom.interfaces.RefreshRecyclerViewListener
import com.smsnew.messenger.commonsLibCustom.views.MyRecyclerView
import com.smsnew.messenger.databinding.ItemManageBlockedKeywordBinding
import com.smsnew.messenger.extensions.config

class ManageBlockedKeywordsAdapter(
    activity: BaseSimpleActivity, var blockedKeywords: ArrayList<String>, val listener: RefreshRecyclerViewListener?,
    recyclerView: MyRecyclerView, itemClick: (Any) -> Unit
) : MyRecyclerViewAdapter(activity, recyclerView, itemClick) {
    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.cab_blocked_keywords

    override fun prepareActionMode(menu: Menu) {
        menu.apply {
            findItem(R.id.cab_copy_keyword).isVisible = isOneItemSelected()
        }
    }

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_copy_keyword -> copyKeywordToClipboard()
            R.id.cab_delete -> deleteSelection()
        }
    }

    override fun getSelectableItemCount() = blockedKeywords.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = blockedKeywords.getOrNull(position)?.hashCode()

    override fun getItemKeyPosition(key: Int) = blockedKeywords.indexOfFirst { it.hashCode() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemManageBlockedKeywordBinding.inflate(layoutInflater, parent, false)
        return createViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val blockedKeyword = blockedKeywords[position]
        holder.bindView(blockedKeyword, allowSingleClick = true, allowLongClick = true) { itemView, _ ->
            setupView(itemView, blockedKeyword)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = blockedKeywords.size

    private fun getSelectedItems() = blockedKeywords.filter { selectedKeys.contains(it.hashCode()) }

    private fun setupView(view: View, blockedKeyword: String) {
        ItemManageBlockedKeywordBinding.bind(view).apply {
            root.setupViewBackground(activity)
            manageBlockedKeywordHolder.isSelected = selectedKeys.contains(blockedKeyword.hashCode())
            manageBlockedKeywordTitle.apply {
                text = blockedKeyword
                setTextColor(textColor)
            }

            overflowMenuIcon.drawable.apply {
                mutate()
                setTint(activity.getProperTextColor())
            }

            overflowMenuIcon.setOnClickListener {
                showPopupMenu(overflowMenuAnchor, blockedKeyword)
            }
        }
    }

    private fun showPopupMenu(view: View, blockedKeyword: String) {
        finishActMode()
        val theme = activity.getPopupMenuTheme()
        val contextTheme = ContextThemeWrapper(activity, theme)

        PopupMenu(contextTheme, view, Gravity.END).apply {
            inflate(getActionMenuId())
            setOnMenuItemClickListener { item ->
                val blockedKeywordId = blockedKeyword.hashCode()
                when (item.itemId) {
                    R.id.cab_copy_keyword -> {
                        executeItemMenuOperation(blockedKeywordId) {
                            copyKeywordToClipboard()
                        }
                    }

                    R.id.cab_delete -> {
                        executeItemMenuOperation(blockedKeywordId) {
                            deleteSelection()
                        }
                    }
                }
                true
            }
            show()
        }
    }

    private fun executeItemMenuOperation(blockedKeywordId: Int, callback: () -> Unit) {
        selectedKeys.add(blockedKeywordId)
        callback()
        selectedKeys.remove(blockedKeywordId)
    }

    private fun copyKeywordToClipboard() {
        val selectedKeyword = getSelectedItems().firstOrNull() ?: return
        activity.copyToClipboard(selectedKeyword)
        finishActMode()
    }

    private fun deleteSelection() {
        val deleteBlockedKeywords = HashSet<String>(selectedKeys.size)
        val positions = getSelectedItemPositions()

        getSelectedItems().forEach {
            deleteBlockedKeywords.add(it)
            activity.config.removeBlockedKeyword(it)
        }

        blockedKeywords.removeAll(deleteBlockedKeywords)
        removeSelectedItems(positions)
        if (blockedKeywords.isEmpty()) {
            listener?.refreshItems()
        }
    }
}
