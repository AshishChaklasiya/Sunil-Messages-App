package com.smsnew.messenger.adapters

import android.util.TypedValue
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.smsnew.messenger.activities.SimpleActivity
import com.smsnew.messenger.commonsLibCustom.adapters.MyRecyclerViewAdapter
import com.smsnew.messenger.commonsLibCustom.extensions.*
import com.smsnew.messenger.commonsLibCustom.helpers.SimpleContactsHelper
import com.smsnew.messenger.commonsLibCustom.views.MyRecyclerView
import com.smsnew.messenger.databinding.ItemSearchResultBinding
import com.smsnew.messenger.extensions.config
import com.smsnew.messenger.models.SearchResult

class SearchResultsAdapter(
    activity: SimpleActivity, var searchResults: ArrayList<SearchResult>, recyclerView: MyRecyclerView, highlightText: String, itemClick: (Any) -> Unit
) : MyRecyclerViewAdapter(activity, recyclerView, itemClick) {

    private var fontSize = activity.getTextSize()
    private var textToHighlight = highlightText

    init {
        addBottomPadding(64)
    }

    override fun getActionMenuId() = 0

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {}

    override fun getSelectableItemCount() = searchResults.size

    override fun getIsItemSelectable(position: Int) = false

    override fun getItemSelectionKey(position: Int) = searchResults.getOrNull(position)?.hashCode()

    override fun getItemKeyPosition(key: Int) = searchResults.indexOfFirst { it.hashCode() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSearchResultBinding.inflate(layoutInflater, parent, false)
        return createViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val searchResult = searchResults[position]
        holder.bindView(searchResult, allowSingleClick = true, allowLongClick = false) { itemView, _ ->
            setupView(itemView, searchResult)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = searchResults.size

    fun updateItems(newItems: ArrayList<SearchResult>, highlightText: String = "") {
        if (newItems.hashCode() != searchResults.hashCode()) {
            searchResults = newItems.clone() as ArrayList<SearchResult>
            textToHighlight = highlightText
            notifyDataSetChanged()
        } else if (textToHighlight != highlightText) {
            textToHighlight = highlightText
            notifyDataSetChanged()
        }
    }

    private fun setupView(view: View, searchResult: SearchResult) {
        ItemSearchResultBinding.bind(view).apply {

            searchResultChevron.setColorFilter(textColor)
            divider.setBackgroundColor(textColor)
            if (searchResults.last() == searchResult || !activity.config.useDividers) divider.beInvisible() else divider.beVisible()

            searchResultTitle.apply {
                text = searchResult.title.highlightTextPart(textToHighlight, properPrimaryColor)
                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 1.2f)
            }

            searchResultSnippet.apply {
                text = searchResult.snippet.highlightTextPart(textToHighlight, properPrimaryColor)
                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 0.9f)
            }

            searchResultDate.apply {
                text = searchResult.date
                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 0.8f)
            }

            searchResultImage.beGoneIf(!activity.config.showContactThumbnails)
            if ((searchResult.title == searchResult.phoneNumber || searchResult.isCompany) && searchResult.photoUri == "") {
                val drawable =
                    if (searchResult.isCompany) SimpleContactsHelper(activity).getColoredCompanyIcon(searchResult.run { title })
                    else SimpleContactsHelper(activity).getColoredContactIcon(searchResult.run { title })
                searchResultImage.setImageDrawable(drawable)
            } else {
                SimpleContactsHelper(activity).loadContactImage(searchResult.photoUri, searchResultImage, searchResult.title)
            }
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        if (!activity.isDestroyed && !activity.isFinishing) {
            val binding = ItemSearchResultBinding.bind(holder.itemView)
            Glide.with(activity).clear(binding.searchResultImage)
        }
    }
}
