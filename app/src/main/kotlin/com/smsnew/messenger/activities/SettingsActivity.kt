package com.smsnew.messenger.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import com.behaviorule.arturdumchev.library.pixels
import com.mikhaellopez.rxanimation.RxAnimation
import com.mikhaellopez.rxanimation.shake
import com.smsnew.messenger.R
import com.smsnew.messenger.commonsLibCustom.activities.ManageBlockedNumbersActivity
import com.smsnew.messenger.commonsLibCustom.dialogs.*
import com.smsnew.messenger.commonsLibCustom.extensions.*
import com.smsnew.messenger.commonsLibCustom.helpers.*
import com.smsnew.messenger.commonsLibCustom.models.RadioItem
import com.smsnew.messenger.databinding.ActivitySettingsBinding
import com.smsnew.messenger.dialogs.ExportMessagesDialog
import com.smsnew.messenger.dialogs.MessageBubbleSettingDialog
import com.smsnew.messenger.extensions.*
import com.smsnew.messenger.helpers.*
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.system.exitProcess

class SettingsActivity : SimpleActivity() {
    private var blockedNumbersAtPause = -1
    private var recycleBinMessages = 0
    private val messagesFileType = "application/json"
    private val messageImportFileTypes = buildList {
        add("application/json")
        add("application/xml")
        add("text/xml")
        if (!isQPlus()) {
            add("application/octet-stream")
        }
    }

    private val getContent =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                MessagesImporter(this).importMessages(uri)
            }
        }

    private var exportMessagesDialog: ExportMessagesDialog? = null

    private val saveDocument =
        registerForActivityResult(ActivityResultContracts.CreateDocument(messagesFileType)) { uri ->
            if (uri != null) {
                toast(R.string.exporting)
                exportMessagesDialog?.exportMessages(uri)
            }
        }

    private val binding by viewBinding(ActivitySettingsBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupEdgeToEdge(padBottomImeAndSystem = listOf(binding.settingsNestedScrollview))


    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.settingsAppbar, NavigationIcon.Arrow)

        setupPurchaseThankYou()

        setupCustomizeColors()
        setupOverflowIcon()
        setupFloatingButtonStyle()
        setupUseColoredContacts()
        setupContactsColorList()
        setupColorSimIcons()
        setupSimCardColorList()

        setupManageBlockedNumbers()
        setupManageBlockedKeywords()
        setupUseSpeechToText()
        setupFontSize()
        setupChangeDateTimeFormat()
        setupUseEnglish()
        setupLanguage()

        setupUseSwipeToAction()
        setupSwipeWidth()
        setupSwipeVibration()
        setupSwipeRipple()
        setupSwipeRightAction()
        setupSwipeLeftAction()
        setupArchiveConfirmation()
        setupDeleteConfirmation()

        setupCustomizeNotifications()
        setupLockScreenVisibility()
        setupCopyNumberAndDelete()
        setupNotifyTurnsOnScreen()

        setupThreadTopStyle()
        setupShowNumberInTitle()
        setupMessageBubble()
        setupTextAlignmentMessage()
        setupFontSizeMessage()
        setupActionOnMessageClick()

        setupSendOnEnter()
        setupSoundOnOutGoingMessages()
        setupShowSimSelectionDialog()
        setupEnableDeliveryReports()
        setupShowCharacterCounter()
        setupUseSimpleCharacters()
        setupSendLongMessageAsMMS()
        setupGroupMessageAsMMS()
        setupMMSFileSizeLimit()

        setupShowSearchBar()
        setupHideTopBarWhenScroll()

        setupShowDividers()
        setupShowContactThumbnails()
        setupContactThumbnailsSize()
        setupEllipsizeMode()
        setupUseRelativeDate()
        setupUnreadAtTop()
        setupLinesCount()
        setupUnreadIndicatorPosition()
        setupChangeColourTopBarWhenScroll()

        setupKeepConversationsArchived()

        setupUseRecycleBin()
        setupEmptyRecycleBin()

        setupAppPasswordProtection()

        setupMessagesExport()
        setupMessagesImport()

        setupTipJar()
        updateTextColors(binding.settingsNestedScrollview)

        if (blockedNumbersAtPause != -1 && blockedNumbersAtPause != getBlockedNumbers().hashCode()) {
            refreshConversations()
        }

        binding.apply {
            val properPrimaryColor = getProperPrimaryColor()
            arrayOf(
                settingsAppearanceLabel,
                settingsGeneralLabel,
                settingsNotificationsLabel,
                settingsMessagesLabel,
                settingsOutgoingMessagesLabel,
                settingsTopAppBarLabel,
                settingsListViewLabel,
                settingsSwipeGesturesLabel,
                settingsArchivedMessagesLabel,
                settingsRecycleBinLabel,
                settingsSecurityLabel,
                settingsBackupsLabel,
                settingsOtherLabel
            ).forEach {
                it.setTextColor(properPrimaryColor)
            }

            val surfaceColor = getSurfaceColor()
            arrayOf(
                settingsColorCustomizationHolder,
                settingsGeneralHolder,
                settingsNotificationsHolder,
                settingsMessagesHolder,
                settingsOutgoingMessagesHolder,
                settingsTopAppBarHolder,
                settingsListViewHolder,
                settingsSwipeGesturesHolder,
                settingsRecycleBinHolder,
                settingsArchivedMessagesHolder,
                settingsSecurityHolder,
                settingsBackupsHolder,
                settingsOtherHolder
            ).forEach {
                it.setCardBackgroundColor(surfaceColor)
            }

            val properTextColor = getProperTextColor()
            arrayOf(
                settingsCustomizeColorsChevron,
                settingsManageBlockedNumbersChevron,
                settingsManageBlockedKeywordsChevron,
                settingsCustomizeNotificationsChevron,
                settingsImportMessagesChevron,
                settingsExportMessagesChevron,
                settingsTipJarChevron,
                settingsAboutChevron
            ).forEach {
                it.applyColorFilter(properTextColor)
            }
        }
    }

    private fun setupMessagesExport() {
        binding.settingsExportMessagesHolder.setOnClickListener {
            exportMessagesDialog = ExportMessagesDialog(this) { fileName ->
                saveDocument.launch("$fileName.json")
            }
        }
    }

    private fun setupMessagesImport() {
        binding.settingsImportMessagesHolder.setOnClickListener {
            getContent.launch(messageImportFileTypes.toTypedArray())
        }
    }

    override fun onPause() {
        super.onPause()
        blockedNumbersAtPause = getBlockedNumbers().hashCode()
    }

    private fun setupPurchaseThankYou() = binding.apply {
        settingsPurchaseThankYouHolder.beGoneIf(isPro())
    }

    private fun setupCustomizeColors() = binding.apply {
        settingsCustomizeColorsHolder.setOnClickListener {
            startCustomizationActivity(
                showAccentColor = true,
                showAppIconColor = true
            )
        }
    }

    private fun setupCustomizeNotifications() = binding.apply {
        if (settingsCustomizeNotificationsHolder.isGone()) {
            settingsLockScreenVisibilityHolder.background =
                AppCompatResources.getDrawable(this@SettingsActivity, R.drawable.ripple_all_corners)
        }

        settingsCustomizeNotificationsHolder.setOnClickListener {
            launchCustomizeNotificationsIntent()
        }
    }

    private fun setupOverflowIcon() {
        binding.apply {
            settingsOverflowIcon.applyColorFilter(getProperTextColor())
            settingsOverflowIcon.setImageResource(getOverflowIcon(baseConfig.overflowIcon))
            settingsOverflowIconHolder.setOnClickListener {
                val items = arrayListOf(
                    R.drawable.ic_more_horiz,
                    R.drawable.ic_three_dots_vector,
                    R.drawable.ic_more_horiz_round
                )

                IconListDialog(
                    activity = this@SettingsActivity,
                    items = items,
                    checkedItemId = baseConfig.overflowIcon + 1,
                    defaultItemId = OVERFLOW_ICON_HORIZONTAL + 1,
                    titleId = R.string.overflow_icon,
                    size = pixels(R.dimen.normal_icon_size).toInt(),
                    color = getProperTextColor()
                ) { wasPositivePressed, newValue ->
                    if (wasPositivePressed) {
                        if (baseConfig.overflowIcon != newValue - 1) {
                            baseConfig.overflowIcon = newValue - 1
                            settingsOverflowIcon.setImageResource(getOverflowIcon(baseConfig.overflowIcon))
                        }
                    }
                }
            }
        }
    }

    private fun setupFloatingButtonStyle() {
        binding.apply {
            settingsFloatingButtonStyle.applyColorFilter(getProperTextColor())
            settingsFloatingButtonStyle.setImageResource(
                if (baseConfig.materialDesign3) R.drawable.squircle_bg else R.drawable.ic_circle_filled
            )
            settingsFloatingButtonStyleHolder.setOnClickListener {
                val items = arrayListOf(
                    R.drawable.ic_circle_filled,
                    R.drawable.squircle_bg
                )

                IconListDialog(
                    activity = this@SettingsActivity,
                    items = items,
                    checkedItemId = if (baseConfig.materialDesign3) 2 else 1,
                    defaultItemId = 1,
                    titleId = R.string.floating_button_style,
                    size = pixels(R.dimen.normal_icon_size).toInt(),
                    color = getProperTextColor()
                ) { wasPositivePressed, newValue ->
                    if (wasPositivePressed) {
                        if (newValue != if (baseConfig.materialDesign3) 2 else 1) {
                            baseConfig.materialDesign3 = newValue == 2
                            settingsFloatingButtonStyle.setImageResource(
                                if (newValue == 2) R.drawable.squircle_bg
                                else R.drawable.ic_circle_filled
                            )
                            config.needRestart = true
                        }
                    }
                }
            }
        }
    }

    private fun setupThreadTopStyle() = binding.apply {
        settingsThreadTopStyle.text = getThreadTopStyleText()
        settingsThreadTopStyleHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(THREAD_TOP_COMPACT, getString(R.string.small)),
                RadioItem(THREAD_TOP_LARGE, getString(R.string.large))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.threadTopStyle, R.string.chat_title_style_g) {
                config.threadTopStyle = it as Int
                settingsThreadTopStyle.text = getThreadTopStyleText()
            }
        }
    }

    private fun setupShowNumberInTitle() = binding.apply {
        settingsShowNumberInTitle.isChecked = config.showNumberInTitle
        settingsShowNumberInTitleHolder.setOnClickListener {
            settingsShowNumberInTitle.toggle()
            config.showNumberInTitle = settingsShowNumberInTitle.isChecked
        }
    }

    private fun getThreadTopStyleText() = getString(
        when (config.threadTopStyle) {
            THREAD_TOP_COMPACT -> R.string.small
            THREAD_TOP_LARGE -> R.string.large
            else -> R.string.large
        }
    )

    private fun setupMessageBubble() = binding.apply {
        val primaryColor = getProperPrimaryColor()
        settingsMessageBubbleIcon.background = resources.getColoredDrawableWithColor(getMessageBubbleResource(config.bubbleStyle), primaryColor)
        settingsMessageBubbleIcon.setTextColor(primaryColor.getContrastColor())
        settingsMessageBubbleIcon.setPaddingBubble(this@SettingsActivity, config.bubbleStyle)
        settingsMessageBubbleHolder.setOnClickListener {
            MessageBubbleSettingDialog(this@SettingsActivity, isPro()) {
                settingsMessageBubbleIcon.background = resources.getColoredDrawableWithColor(
                    getMessageBubbleResource(it),
                    primaryColor
                )
                settingsMessageBubbleIcon.setPaddingBubble(this@SettingsActivity, it)
            }
        }
    }

    private fun getMessageBubbleResource(bubbleStyle: Int): Int {
        return when (bubbleStyle) {
            BUBBLE_STYLE_ROUNDED -> R.drawable.item_received_rounded_background
            BUBBLE_STYLE_IOS_NEW -> R.drawable.item_received_ios_new_background
            BUBBLE_STYLE_IOS -> R.drawable.item_received_ios_background
            else -> R.drawable.item_received_background
        }
    }

    private fun setupTextAlignmentMessage() = binding.apply {
        settingsTextAlignmentMessage.text = getTextAlignmentMessageText()
        settingsTextAlignmentMessageHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(TEXT_ALIGNMENT_START, getString(R.string.start)),
                RadioItem(TEXT_ALIGNMENT_ALONG_EDGES, getString(R.string.text_alignment_along_edges))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.textAlignment, R.string.text_alignment) {
                config.textAlignment = it as Int
                settingsTextAlignmentMessage.text = getTextAlignmentMessageText()
            }
        }
    }

    private fun getTextAlignmentMessageText() = getString(
        when (config.textAlignment) {
            TEXT_ALIGNMENT_START -> R.string.start
            else -> R.string.text_alignment_along_edges
        }
    )

    private fun setupFontSizeMessage() = binding.apply {
        settingsFontSizeMessage.text = getFontSizeMessageText()
        settingsFontSizeMessageHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(FONT_SIZE_SMALL, getString(R.string.small)),
                RadioItem(FONT_SIZE_MEDIUM, getString(R.string.medium)),
                RadioItem(FONT_SIZE_LARGE, getString(R.string.large)),
                RadioItem(FONT_SIZE_EXTRA_LARGE, getString(R.string.extra_large))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.fontSizeMessage, R.string.font_size) {
                config.fontSizeMessage = it as Int
                settingsFontSizeMessage.text = getFontSizeMessageText()
            }
        }
    }

    private fun getFontSizeMessageText() = getString(
        when (config.fontSizeMessage) {
            FONT_SIZE_SMALL -> R.string.small
            FONT_SIZE_MEDIUM -> R.string.medium
            FONT_SIZE_LARGE -> R.string.large
            else -> R.string.extra_large
        }
    )

    private fun setupUseEnglish() = binding.apply {
        settingsUseEnglishHolder.beVisibleIf(
            (config.wasUseEnglishToggled || Locale.getDefault().language != "en") && !isTiramisuPlus()
        )
        settingsUseEnglish.isChecked = config.useEnglish
        settingsUseEnglishHolder.setOnClickListener {
            settingsUseEnglish.toggle()
            config.useEnglish = settingsUseEnglish.isChecked
            exitProcess(0)
        }
    }

    private fun setupLanguage() = binding.apply {
        settingsLanguage.text = Locale.getDefault().displayLanguage
        if (isTiramisuPlus()) {
            settingsLanguageHolder.beVisible()
            settingsLanguageHolder.setOnClickListener {
                launchChangeAppLanguageIntent()
            }
        } else {
            settingsLanguageHolder.beGone()
        }
    }

    private fun setupManageBlockedNumbers() = binding.apply {
        @SuppressLint("SetTextI18n")
        settingsManageBlockedNumbersCount.text = getBlockedNumbers().size.toString()

        val getProperTextColor = getProperTextColor()
        val red = resources.getColor(R.color.red_missed, theme)
        val colorUnknown = if (baseConfig.blockUnknownNumbers) red else getProperTextColor
        val alphaUnknown = if (baseConfig.blockUnknownNumbers) 1f else 0.6f
        settingsManageBlockedNumbersIconUnknown.apply {
            applyColorFilter(colorUnknown)
            alpha = alphaUnknown
        }

        settingsManageBlockedNumbersHolder.setOnClickListener {
            Intent(this@SettingsActivity, ManageBlockedNumbersActivity::class.java).apply {
                startActivity(this)
            }
        }
    }

    private fun setupManageBlockedKeywords() = binding.apply {
        @SuppressLint("SetTextI18n")
        settingsManageBlockedKeywordsCount.text = config.blockedKeywords.size.toString()
        settingsManageBlockedKeywordsHolder.setOnClickListener {
            Intent(this@SettingsActivity, ManageBlockedKeywordsActivity::class.java).apply {
                startActivity(this)
            }
        }
    }

    private fun setupUseSpeechToText() = binding.apply {
        settingsUseSpeechToText.isChecked = config.useSpeechToText
        settingsUseSpeechToTextHolder.setOnClickListener {
            settingsUseSpeechToText.toggle()
            config.useSpeechToText = settingsUseSpeechToText.isChecked
            config.needRestart = true
        }
    }

    private fun setupChangeDateTimeFormat() = binding.apply {
        updateDateTimeFormat()
        settingsChangeDateTimeFormatHolder.setOnClickListener {
            ChangeDateTimeFormatDialog(this@SettingsActivity, true) {
                updateDateTimeFormat()
                refreshConversations()
//                config.needRestart = true
            }
        }
    }

    private fun updateDateTimeFormat() {
        val cal = Calendar.getInstance(Locale.ENGLISH).timeInMillis
        val formatDate = cal.formatDate(this@SettingsActivity)
        binding.settingsChangeDateTimeFormat.text = formatDate
    }

    private fun setupFontSize() = binding.apply {
        settingsFontSize.text = getFontSizeText()
        settingsFontSizeHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(FONT_SIZE_SMALL, getString(R.string.small)),
                RadioItem(FONT_SIZE_MEDIUM, getString(R.string.medium)),
                RadioItem(FONT_SIZE_LARGE, getString(R.string.large)),
                RadioItem(FONT_SIZE_EXTRA_LARGE, getString(R.string.extra_large))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.fontSize, R.string.font_size) {
                config.fontSize = it as Int
                settingsFontSize.text = getFontSizeText()
            }
        }
    }

    private fun setupShowCharacterCounter() = binding.apply {
        settingsShowCharacterCounter.isChecked = config.showCharacterCounter
        settingsShowCharacterCounterHolder.setOnClickListener {
            settingsShowCharacterCounter.toggle()
            config.showCharacterCounter = settingsShowCharacterCounter.isChecked
        }
    }

    @SuppressLint("PrivateResource")
    private fun setupActionOnMessageClick() = binding.apply {
        settingsActionOnMessageClick.text = getActionOnMessageClickText()
        settingsActionOnMessageClickHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(ACTION_COPY_CODE, getString(R.string.copy_code), icon = R.drawable.ic_copy_vector),
                RadioItem(ACTION_COPY_MESSAGE, getString(R.string.copy_to_clipboard), icon = R.drawable.ic_copy_vector),
                RadioItem(ACTION_SELECT_TEXT, getString(R.string.select_text), icon = R.drawable.ic_text_select),
                RadioItem(
                    ACTION_NOTHING,
                    getString(com.google.android.material.R.string.exposed_dropdown_menu_content_description),
                    icon = R.drawable.ic_menu_open
                ),
            )

            RadioGroupIconDialog(this@SettingsActivity, items, config.actionOnMessageClickSetting, R.string.action_on_message_click) {
                config.actionOnMessageClickSetting = it as Int
                settingsActionOnMessageClick.text = getActionOnMessageClickText()
            }
        }
    }

    @SuppressLint("PrivateResource")
    private fun getActionOnMessageClickText() = getString(
        when (config.actionOnMessageClickSetting) {
            ACTION_COPY_CODE -> R.string.copy_code
            ACTION_COPY_MESSAGE -> R.string.copy_to_clipboard
            ACTION_SELECT_TEXT -> R.string.select_text
            else -> com.google.android.material.R.string.exposed_dropdown_menu_content_description
        }
    )

    private fun setupUseSimpleCharacters() = binding.apply {
        settingsUseSimpleCharacters.isChecked = config.useSimpleCharacters
        settingsUseSimpleCharactersHolder.setOnClickListener {
            settingsUseSimpleCharacters.toggle()
            config.useSimpleCharacters = settingsUseSimpleCharacters.isChecked
        }
    }

    private fun setupSendOnEnter() = binding.apply {
        settingsSendOnEnter.isChecked = config.sendOnEnter
        settingsSendOnEnterHolder.setOnClickListener {
            settingsSendOnEnter.toggle()
            config.sendOnEnter = settingsSendOnEnter.isChecked
        }
    }

    private fun setupSoundOnOutGoingMessages() = binding.apply {
        settingsSoundOnOutGoingMessages.isChecked = config.soundOnOutGoingMessages
        settingsSoundOnOutGoingMessagesHolder.setOnClickListener {
            settingsSoundOnOutGoingMessages.toggle()
            config.soundOnOutGoingMessages = settingsSoundOnOutGoingMessages.isChecked
        }
    }

    private fun setupShowSimSelectionDialog() = binding.apply {
        settingsShowSimSelectionDialogHolder.beVisibleIf(areMultipleSIMsAvailable())
        settingsShowSimSelectionDialog.isChecked = config.showSimSelectionDialog
        settingsShowSimSelectionDialogHolder.setOnClickListener {
            settingsShowSimSelectionDialog.toggle()
            config.showSimSelectionDialog = settingsShowSimSelectionDialog.isChecked
        }
    }

    private fun setupEnableDeliveryReports() = binding.apply {
        settingsEnableDeliveryReports.isChecked = config.enableDeliveryReports
        settingsEnableDeliveryReportsHolder.setOnClickListener {
            settingsEnableDeliveryReports.toggle()
            config.enableDeliveryReports = settingsEnableDeliveryReports.isChecked
        }
    }

    private fun setupSendLongMessageAsMMS() = binding.apply {
        settingsSendLongMessageMms.isChecked = config.sendLongMessageMMS
        settingsSendLongMessageMmsHolder.setOnClickListener {
            settingsSendLongMessageMms.toggle()
            config.sendLongMessageMMS = settingsSendLongMessageMms.isChecked
        }
    }

    private fun setupGroupMessageAsMMS() = binding.apply {
        settingsSendGroupMessageMms.isChecked = config.sendGroupMessageMMS
        settingsSendGroupMessageMmsHolder.setOnClickListener {
            settingsSendGroupMessageMms.toggle()
            config.sendGroupMessageMMS = settingsSendGroupMessageMms.isChecked
        }
    }

    private fun setupKeepConversationsArchived() = binding.apply {
        settingsKeepConversationsArchivedHolder.beVisibleIf(config.isArchiveAvailable)
        settingsKeepConversationsArchived.isChecked = config.keepConversationsArchived
        settingsKeepConversationsArchivedHolder.setOnClickListener {
            settingsKeepConversationsArchived.toggle()
            config.keepConversationsArchived = settingsKeepConversationsArchived.isChecked
        }
    }

    private fun setupLockScreenVisibility() = binding.apply {
        settingsLockScreenVisibility.text = getLockScreenVisibilityText()
        settingsLockScreenVisibilityHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(LOCK_SCREEN_SENDER_MESSAGE, getString(R.string.sender_and_message)),
                RadioItem(LOCK_SCREEN_SENDER, getString(R.string.sender_only)),
                RadioItem(LOCK_SCREEN_NOTHING, getString(R.string.nothing)),
            )

            RadioGroupDialog(this@SettingsActivity, items, config.lockScreenVisibilitySetting, R.string.lock_screen_visibility) {
                config.lockScreenVisibilitySetting = it as Int
                settingsLockScreenVisibility.text = getLockScreenVisibilityText()
            }
        }
    }

    private fun setupCopyNumberAndDelete() = binding.apply {
        settingsCopyNumberAndDelete.isChecked = config.copyNumberAndDelete
        settingsCopyNumberAndDeleteHolder.setOnClickListener {
            settingsCopyNumberAndDelete.toggle()
            config.copyNumberAndDelete = settingsCopyNumberAndDelete.isChecked
        }
    }

    private fun setupNotifyTurnsOnScreen() = binding.apply {
        settingsNotifyTurnsOnScreen.isChecked = config.notifyTurnsOnScreen
        settingsNotifyTurnsOnScreenHolder.setOnClickListener {
            settingsNotifyTurnsOnScreen.toggle()
            config.notifyTurnsOnScreen = settingsNotifyTurnsOnScreen.isChecked
        }
    }

    private fun getLockScreenVisibilityText() = getString(
        when (config.lockScreenVisibilitySetting) {
            LOCK_SCREEN_SENDER_MESSAGE -> R.string.sender_and_message
            LOCK_SCREEN_SENDER -> R.string.sender_only
            else -> R.string.nothing
        }
    )

    private fun setupMMSFileSizeLimit() = binding.apply {
        settingsMmsFileSizeLimit.text = getMMSFileLimitText()
        settingsMmsFileSizeLimitHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(7, getString(R.string.mms_file_size_limit_none), FILE_SIZE_NONE),
                RadioItem(6, getString(R.string.mms_file_size_limit_2mb), FILE_SIZE_2_MB),
                RadioItem(5, getString(R.string.mms_file_size_limit_1mb), FILE_SIZE_1_MB),
                RadioItem(4, getString(R.string.mms_file_size_limit_600kb), FILE_SIZE_600_KB),
                RadioItem(3, getString(R.string.mms_file_size_limit_300kb), FILE_SIZE_300_KB),
                RadioItem(2, getString(R.string.mms_file_size_limit_200kb), FILE_SIZE_200_KB),
                RadioItem(1, getString(R.string.mms_file_size_limit_100kb), FILE_SIZE_100_KB),
            )

            val checkedItemId = items.find { it.value == config.mmsFileSizeLimit }?.id ?: 7
            RadioGroupDialog(this@SettingsActivity, items, checkedItemId, R.string.mms_file_size_limit) {
                config.mmsFileSizeLimit = it as Long
                settingsMmsFileSizeLimit.text = getMMSFileLimitText()
            }
        }
    }

    private fun setupUseSwipeToAction() {
        updateSwipeToActionVisible()
        binding.apply {
            settingsUseSwipeToAction.isChecked = config.useSwipeToAction
            settingsUseSwipeToActionHolder.setOnClickListener {
                settingsUseSwipeToAction.toggle()
                config.useSwipeToAction = settingsUseSwipeToAction.isChecked
                config.needRestart = true
                updateSwipeToActionVisible()
            }
        }
    }

    private fun setupSwipeWidth() = binding.apply {
        settingsSwipeWidthHolder.beVisibleIf(config.useSwipeToAction)
        settingsSwipeWidth.text = getSwipeWidthText(config.swipeToActionWidth)
        settingsSwipeWidthHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(2, "1/2"),
                RadioItem(3, "1/3"),
                RadioItem(4, "1/4"),
                RadioItem(5, "1/5"),
            )

            RadioGroupIconDialog(
                this@SettingsActivity,
                items,
                config.swipeToActionWidth,
                R.string.swipe_width,
                defaultItemId = 2
            ) {
                config.swipeToActionWidth = it as Int
                config.needRestart = true
                settingsSwipeWidth.text = getSwipeWidthText(config.swipeToActionWidth)
            }
        }
    }

    private fun getSwipeWidthText(swipeWidth: Int): String {
        return when (swipeWidth) {
            3 -> "1/3"
            4 -> "1/4"
            5 -> "1/5"
            else -> "1/2"
        }
    }

    private fun updateSwipeToActionVisible() {
        binding.apply {
            settingsSwipeWidthHolder.beVisibleIf(config.useSwipeToAction)
            settingsSwipeVibrationHolder.beVisibleIf(config.useSwipeToAction)
            settingsSwipeRippleHolder.beVisibleIf(config.useSwipeToAction)
            settingsSwipeRightActionHolder.beVisibleIf(config.useSwipeToAction)
            settingsSwipeLeftActionHolder.beVisibleIf(config.useSwipeToAction)
            settingsSkipArchiveConfirmationHolder.beVisibleIf(
                (config.swipeLeftAction == SWIPE_ACTION_ARCHIVE || config.swipeRightAction == SWIPE_ACTION_ARCHIVE)
                    && config.isArchiveAvailable && config.useSwipeToAction
            )
            settingsSkipDeleteConfirmationHolder.beVisibleIf(config.useSwipeToAction && (config.swipeLeftAction == SWIPE_ACTION_DELETE || config.swipeRightAction == SWIPE_ACTION_DELETE))
        }
    }

    private fun setupSwipeVibration() {
        binding.apply {
            settingsSwipeVibration.isChecked = config.swipeVibration
            settingsSwipeVibrationHolder.setOnClickListener {
                settingsSwipeVibration.toggle()
                config.swipeVibration = settingsSwipeVibration.isChecked
                config.needRestart = true
            }
        }
    }

    private fun setupSwipeRipple() {
        binding.apply {
            settingsSwipeRipple.isChecked = config.swipeRipple
            settingsSwipeRippleHolder.setOnClickListener {
                settingsSwipeRipple.toggle()
                config.swipeRipple = settingsSwipeRipple.isChecked
                config.needRestart = true
            }
        }
    }

    private fun setupSwipeRightAction() = binding.apply {
        if (isRTLLayout) settingsSwipeRightActionLabel.text = getString(R.string.swipe_left_action)
        settingsSwipeRightAction.text = getSwipeActionText(false)
        settingsSwipeRightActionHolder.setOnClickListener {
            val items = if (config.isArchiveAvailable) arrayListOf(
                RadioItem(SWIPE_ACTION_MARK_READ, getString(R.string.mark_as_read), icon = R.drawable.ic_mark_read),
                RadioItem(SWIPE_ACTION_DELETE, getString(R.string.delete), icon = R.drawable.ic_delete_outline),
                RadioItem(SWIPE_ACTION_ARCHIVE, getString(R.string.archive), icon = R.drawable.ic_archive_vector),
                RadioItem(SWIPE_ACTION_BLOCK, getString(R.string.block_number), icon = R.drawable.ic_block_vector),
                RadioItem(SWIPE_ACTION_CALL, getString(R.string.call), icon = R.drawable.ic_phone_vector),
                RadioItem(SWIPE_ACTION_MESSAGE, getString(R.string.send_sms), icon = R.drawable.ic_messages),
                RadioItem(SWIPE_ACTION_NONE, getString(R.string.nothing)),
            ) else arrayListOf(
                RadioItem(SWIPE_ACTION_MARK_READ, getString(R.string.mark_as_read), icon = R.drawable.ic_mark_read),
                RadioItem(SWIPE_ACTION_DELETE, getString(R.string.delete), icon = R.drawable.ic_delete_outline),
                RadioItem(SWIPE_ACTION_BLOCK, getString(R.string.block_number), icon = R.drawable.ic_block_vector),
                RadioItem(SWIPE_ACTION_CALL, getString(R.string.call), icon = R.drawable.ic_phone_vector),
                RadioItem(SWIPE_ACTION_MESSAGE, getString(R.string.send_sms), icon = R.drawable.ic_messages),
                RadioItem(SWIPE_ACTION_NONE, getString(R.string.nothing)),
            )

            val title =
                if (isRTLLayout) R.string.swipe_left_action else R.string.swipe_right_action
            RadioGroupIconDialog(this@SettingsActivity, items, config.swipeRightAction, title) {
                config.swipeRightAction = it as Int
                config.needRestart = true
                settingsSwipeRightAction.text = getSwipeActionText(false)
                settingsSkipArchiveConfirmationHolder.beVisibleIf(
                    (config.swipeLeftAction == SWIPE_ACTION_ARCHIVE || config.swipeRightAction == SWIPE_ACTION_ARCHIVE)
                        && config.isArchiveAvailable
                )
                settingsSkipDeleteConfirmationHolder.beVisibleIf(
                    (config.swipeLeftAction == SWIPE_ACTION_DELETE || config.swipeRightAction == SWIPE_ACTION_DELETE)
                        && config.isArchiveAvailable
                )
            }
        }
    }

    private fun setupSwipeLeftAction() = binding.apply {
        val pro = isPro()
        settingsSwipeLeftActionHolder.alpha = if (pro) 1f else 0.4f
        val stringId =
            if (isRTLLayout) R.string.swipe_right_action else R.string.swipe_left_action
        settingsSwipeLeftActionLabel.text = addLockedLabelIfNeeded(stringId, pro)
        settingsSwipeLeftAction.text = getSwipeActionText(true)
        settingsSwipeLeftActionHolder.setOnClickListener {
            if (pro) {
                val items = if (config.isArchiveAvailable) arrayListOf(
                    RadioItem(SWIPE_ACTION_MARK_READ, getString(R.string.mark_as_read), icon = R.drawable.ic_mark_read),
                    RadioItem(SWIPE_ACTION_DELETE, getString(R.string.delete), icon = R.drawable.ic_delete_outline),
                    RadioItem(SWIPE_ACTION_ARCHIVE, getString(R.string.archive), icon = R.drawable.ic_archive_vector),
                    RadioItem(SWIPE_ACTION_BLOCK, getString(R.string.block_number), icon = R.drawable.ic_block_vector),
                    RadioItem(SWIPE_ACTION_CALL, getString(R.string.call), icon = R.drawable.ic_phone_vector),
                    RadioItem(SWIPE_ACTION_MESSAGE, getString(R.string.send_sms), icon = R.drawable.ic_messages),
                    RadioItem(SWIPE_ACTION_NONE, getString(R.string.nothing)),
                ) else arrayListOf(
                    RadioItem(SWIPE_ACTION_MARK_READ, getString(R.string.mark_as_read), icon = R.drawable.ic_mark_read),
                    RadioItem(SWIPE_ACTION_DELETE, getString(R.string.delete), icon = R.drawable.ic_delete_outline),
                    RadioItem(SWIPE_ACTION_BLOCK, getString(R.string.block_number), icon = R.drawable.ic_block_vector),
                    RadioItem(SWIPE_ACTION_CALL, getString(R.string.call), icon = R.drawable.ic_phone_vector),
                    RadioItem(SWIPE_ACTION_MESSAGE, getString(R.string.send_sms), icon = R.drawable.ic_messages),
                    RadioItem(SWIPE_ACTION_NONE, getString(R.string.nothing)),
                )

                val title =
                    if (isRTLLayout) R.string.swipe_right_action else R.string.swipe_left_action
                RadioGroupIconDialog(this@SettingsActivity, items, config.swipeLeftAction, title) {
                    config.swipeLeftAction = it as Int
                    config.needRestart = true
                    settingsSwipeLeftAction.text = getSwipeActionText(true)
                    settingsSkipArchiveConfirmationHolder.beVisibleIf(
                        (config.swipeLeftAction == SWIPE_ACTION_ARCHIVE || config.swipeRightAction == SWIPE_ACTION_ARCHIVE)
                            && config.isArchiveAvailable
                    )
                    settingsSkipDeleteConfirmationHolder.beVisibleIf(
                        (config.swipeLeftAction == SWIPE_ACTION_DELETE || config.swipeRightAction == SWIPE_ACTION_DELETE)
                            && config.isArchiveAvailable
                    )
                }
            } else {
                RxAnimation.from(settingsSwipeLeftActionHolder)
                    .shake(shakeTranslation = 2f)
                    .subscribe()

                showSnackbar(binding.root)
            }
        }
    }

    private fun getSwipeActionText(left: Boolean) = getString(
        when (if (left) config.swipeLeftAction else config.swipeRightAction) {
            SWIPE_ACTION_MARK_READ -> R.string.mark_as_read
            SWIPE_ACTION_DELETE -> R.string.delete
            SWIPE_ACTION_ARCHIVE -> R.string.archive
            SWIPE_ACTION_BLOCK -> R.string.block_number
            SWIPE_ACTION_CALL -> R.string.call
            SWIPE_ACTION_MESSAGE -> R.string.send_sms
            else -> R.string.nothing
        }
    )

    private fun setupArchiveConfirmation() {
        binding.apply {
            //settingsSkipArchiveConfirmationHolder.beVisibleIf(config.swipeLeftAction == SWIPE_ACTION_ARCHIVE || config.swipeRightAction == SWIPE_ACTION_ARCHIVE)
            settingsSkipArchiveConfirmation.isChecked = config.skipArchiveConfirmation
            settingsSkipArchiveConfirmationHolder.setOnClickListener {
                settingsSkipArchiveConfirmation.toggle()
                config.skipArchiveConfirmation = settingsSkipArchiveConfirmation.isChecked
            }
        }
    }

    private fun setupDeleteConfirmation() {
        binding.apply {
            //settingsSkipDeleteConfirmationHolder.beVisibleIf(config.swipeLeftAction == SWIPE_ACTION_DELETE || config.swipeRightAction == SWIPE_ACTION_DELETE)
            settingsSkipDeleteConfirmation.isChecked = config.skipDeleteConfirmation
            settingsSkipDeleteConfirmationHolder.setOnClickListener {
                settingsSkipDeleteConfirmation.toggle()
                config.skipDeleteConfirmation = settingsSkipDeleteConfirmation.isChecked
            }
        }
    }

    private fun setupUseRecycleBin() = binding.apply {
        updateRecycleBinButtons()
        settingsUseRecycleBin.isChecked = config.useRecycleBin
        settingsUseRecycleBinHolder.setOnClickListener {
            settingsUseRecycleBin.toggle()
            config.useRecycleBin = settingsUseRecycleBin.isChecked
            updateRecycleBinButtons()
        }
    }

    private fun updateRecycleBinButtons() = binding.apply {
        settingsEmptyRecycleBinHolder.beVisibleIf(config.useRecycleBin)
    }

    private fun setupEmptyRecycleBin() = binding.apply {
        ensureBackgroundThread {
            recycleBinMessages = messagesDB.getArchivedCount()
            runOnUiThread {
                settingsEmptyRecycleBinSize.text =
                    resources.getQuantityString(R.plurals.delete_messages, recycleBinMessages, recycleBinMessages)
            }
        }

        settingsEmptyRecycleBinHolder.setOnClickListener {
            if (recycleBinMessages == 0) {
                toast(R.string.recycle_bin_empty)
            } else {
                ConfirmationDialog(
                    activity = this@SettingsActivity,
                    message = "",
                    messageId = R.string.empty_recycle_bin_messages_confirmation,
                    positive = R.string.yes,
                    negative = R.string.no
                ) {
                    ensureBackgroundThread {
                        emptyMessagesRecycleBin()
                    }
                    recycleBinMessages = 0
                    settingsEmptyRecycleBinSize.text =
                        resources.getQuantityString(R.plurals.delete_messages, recycleBinMessages, recycleBinMessages)
                }
            }
        }
    }

    private fun setupAppPasswordProtection() = binding.apply {
        settingsAppPasswordProtection.isChecked = config.isAppPasswordProtectionOn
        settingsAppPasswordProtectionHolder.setOnClickListener {
            val tabToShow = if (config.isAppPasswordProtectionOn) config.appProtectionType else SHOW_ALL_TABS
            SecurityDialog(
                activity = this@SettingsActivity,
                requiredHash = config.appPasswordHash,
                showTabIndex = tabToShow
            ) { hash, type, success ->
                if (success) {
                    val hasPasswordProtection = config.isAppPasswordProtectionOn
                    settingsAppPasswordProtection.isChecked = !hasPasswordProtection
                    config.isAppPasswordProtectionOn = !hasPasswordProtection
                    config.appPasswordHash = if (hasPasswordProtection) "" else hash
                    config.appProtectionType = type

                    if (config.isAppPasswordProtectionOn) {
                        val confirmationTextId =
                            if (config.appProtectionType == PROTECTION_FINGERPRINT) {
                                R.string.fingerprint_setup_successfully
                            } else {
                                R.string.protection_setup_successfully
                            }

                        ConfirmationDialog(
                            activity = this@SettingsActivity,
                            message = "",
                            messageId = confirmationTextId,
                            positive = R.string.ok,
                            negative = 0
                        ) { }
                    }
                }
            }
        }
    }

    private fun getMMSFileLimitText() = getString(
        when (config.mmsFileSizeLimit) {
            FILE_SIZE_100_KB -> R.string.mms_file_size_limit_100kb
            FILE_SIZE_200_KB -> R.string.mms_file_size_limit_200kb
            FILE_SIZE_300_KB -> R.string.mms_file_size_limit_300kb
            FILE_SIZE_600_KB -> R.string.mms_file_size_limit_600kb
            FILE_SIZE_1_MB -> R.string.mms_file_size_limit_1mb
            FILE_SIZE_2_MB -> R.string.mms_file_size_limit_2mb
            else -> R.string.mms_file_size_limit_none
        }
    )

    private fun setupShowDividers() = binding.apply {
        settingsShowDividers.isChecked = config.useDividers
        settingsShowDividersHolder.setOnClickListener {
            settingsShowDividers.toggle()
            config.useDividers = settingsShowDividers.isChecked
            config.needRestart = true
        }
    }

    private fun setupShowContactThumbnails() = binding.apply {
        settingsShowContactThumbnails.isChecked = config.showContactThumbnails
        settingsShowContactThumbnailsHolder.setOnClickListener {
            settingsShowContactThumbnails.toggle()
            config.showContactThumbnails = settingsShowContactThumbnails.isChecked
            settingsContactThumbnailsSizeHolder.beVisibleIf(config.showContactThumbnails)
            config.needRestart = true
        }
    }

    private fun setupContactThumbnailsSize() = binding.apply {
        val pro = isPro()
        settingsContactThumbnailsSizeHolder.beVisibleIf(config.showContactThumbnails)
        settingsContactThumbnailsSizeHolder.alpha = if (pro) 1f else 0.4f
        settingsContactThumbnailsSizeLabel.text = addLockedLabelIfNeeded(R.string.contact_thumbnails_size, pro)
        settingsContactThumbnailsSize.text = getContactThumbnailsSizeText()
        settingsContactThumbnailsSizeHolder.setOnClickListener {
            if (pro) {
                val items = arrayListOf(
                    RadioItem(FONT_SIZE_SMALL, getString(R.string.small), CONTACT_THUMBNAILS_SIZE_SMALL),
                    RadioItem(FONT_SIZE_MEDIUM, getString(R.string.medium), CONTACT_THUMBNAILS_SIZE_MEDIUM),
                    RadioItem(FONT_SIZE_LARGE, getString(R.string.large), CONTACT_THUMBNAILS_SIZE_LARGE),
                    RadioItem(FONT_SIZE_EXTRA_LARGE, getString(R.string.extra_large), CONTACT_THUMBNAILS_SIZE_EXTRA_LARGE)
                )

                RadioGroupDialog(this@SettingsActivity, items, config.contactThumbnailsSize, R.string.contact_thumbnails_size) {
                    config.contactThumbnailsSize = it as Int
                    settingsContactThumbnailsSize.text = getContactThumbnailsSizeText()
                    config.needRestart = true
                }
            } else {
                RxAnimation.from(settingsContactThumbnailsSizeHolder)
                    .shake(shakeTranslation = 2f)
                    .subscribe()

                showSnackbar(binding.root)
            }
        }
    }

    private fun getContactThumbnailsSizeText() = getString(
        when (baseConfig.contactThumbnailsSize) {
            CONTACT_THUMBNAILS_SIZE_SMALL -> R.string.small
            CONTACT_THUMBNAILS_SIZE_MEDIUM -> R.string.medium
            CONTACT_THUMBNAILS_SIZE_LARGE -> R.string.large
            else -> R.string.extra_large
        }
    )

    private fun setupEllipsizeMode() = binding.apply {
        settingsEllipsizeMode.text = getEllipsizeModeText()
        settingsEllipsizeModeHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(ELLIPSIZE_MODE_END, getString(R.string.cut_off)),
                RadioItem(ELLIPSIZE_MODE_MARQUEE, getString(R.string.auto_scroll)),
            )

            RadioGroupDialog(
                this@SettingsActivity,
                items,
                config.ellipsizeMode,
                R.string.long_text_behavior,
                defaultItemId = ELLIPSIZE_MODE_END
            ) {
                config.ellipsizeMode = it as Int
                settingsEllipsizeMode.text = getEllipsizeModeText()
//                config.needRestart = true
            }
        }
    }

    private fun getEllipsizeModeText() = getString(
        when (baseConfig.ellipsizeMode) {
            ELLIPSIZE_MODE_END -> R.string.cut_off
            else -> R.string.auto_scroll
        }
    )

    private fun setupUseRelativeDate() = binding.apply {
        settingsRelativeDate.isChecked = config.useRelativeDate
        settingsRelativeDateHolder.setOnClickListener {
            settingsRelativeDate.toggle()
            config.useRelativeDate = settingsRelativeDate.isChecked
            config.needRestart = true
        }
    }

    private fun setupUnreadAtTop() = binding.apply {
        settingsUnreadAtTop.isChecked = config.unreadAtTop
        settingsUnreadAtTopHolder.setOnClickListener {
            settingsUnreadAtTop.toggle()
            config.unreadAtTop = settingsUnreadAtTop.isChecked
            config.needRestart = true
        }
    }

    private fun setupLinesCount() = binding.apply {
        @SuppressLint("SetTextI18n")
        settingsLinesCount.text = config.linesCount.toString()
        settingsLinesCountHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(1, "1", icon = R.drawable.ic_lines_count_1),
                RadioItem(2, "2", icon = R.drawable.ic_lines_count_2),
                RadioItem(3, "3", icon = R.drawable.ic_lines_count_3),
                RadioItem(4, "4", icon = R.drawable.ic_lines_count_4)
            )

            RadioGroupIconDialog(this@SettingsActivity, items, config.linesCount, R.string.lines_count) {
                config.linesCount = it as Int
                settingsLinesCount.text = it.toString()
                config.needRestart = true
            }
        }
    }

    private fun setupUnreadIndicatorPosition() = binding.apply {
        settingsUnreadIndicatorPosition.text = getUnreadIndicatorPositionText()
        settingsUnreadIndicatorPositionHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(UNREAD_INDICATOR_START, getString(R.string.start), icon = R.drawable.ic_unread_start),
                RadioItem(UNREAD_INDICATOR_END, getString(R.string.end), icon = R.drawable.ic_unread_end)
            )

            RadioGroupIconDialog(this@SettingsActivity, items, config.unreadIndicatorPosition, R.string.unread_indicator_position) {
                config.unreadIndicatorPosition = it as Int
                settingsUnreadIndicatorPosition.text = getUnreadIndicatorPositionText()
                config.needRestart = true
            }
        }
    }

    private fun getUnreadIndicatorPositionText() = getString(
        when (config.unreadIndicatorPosition) {
            UNREAD_INDICATOR_START -> R.string.start
            else -> R.string.end
        }
    )

    private fun setupShowSearchBar() = binding.apply {
        settingsShowSearchBar.isChecked = config.showSearchBar
        settingsShowSearchBarHolder.setOnClickListener {
            settingsShowSearchBar.toggle()
            config.showSearchBar = settingsShowSearchBar.isChecked
            config.needRestart = true
        }
    }

    private fun setupHideTopBarWhenScroll() = binding.apply {
        settingsHideBarWhenScroll.isChecked = config.hideTopBarWhenScroll
        settingsHideBarWhenScrollHolder.setOnClickListener {
            settingsHideBarWhenScroll.toggle()
            config.hideTopBarWhenScroll = settingsHideBarWhenScroll.isChecked
            config.needRestart = true
        }
    }

    private fun setupChangeColourTopBarWhenScroll() = binding.apply {
        settingsChangeColourTopBar.isChecked = config.changeColourTopBar
        settingsChangeColourTopBarHolder.setOnClickListener {
            settingsChangeColourTopBar.toggle()
            config.changeColourTopBar = settingsChangeColourTopBar.isChecked
            config.needRestart = true
        }
    }

    private fun setupUseColoredContacts() = binding.apply {
        settingsColoredContacts.isChecked = config.useColoredContacts
        settingsColoredContactsHolder.setOnClickListener {
            settingsColoredContacts.toggle()
            config.useColoredContacts = settingsColoredContacts.isChecked
            settingsContactColorListHolder.beVisibleIf(config.useColoredContacts)
            config.needRestart = true
        }
    }

    private fun setupContactsColorList() = binding.apply {
        settingsContactColorListHolder.beVisibleIf(config.useColoredContacts)
        settingsContactColorListIcon.setImageResource(getContactsColorListIcon(config.contactColorList))
        settingsContactColorListHolder.setOnClickListener {
            val items = arrayListOf(
                R.drawable.ic_color_list,
                R.drawable.ic_color_list_android,
                R.drawable.ic_color_list_ios,
                R.drawable.ic_color_list_arc
            )

            IconListDialog(
                activity = this@SettingsActivity,
                items = items,
                checkedItemId = config.contactColorList,
                defaultItemId = LBC_ANDROID,
                titleId = R.string.overflow_icon
            ) { wasPositivePressed, newValue ->
                if (wasPositivePressed) {
                    if (config.contactColorList != newValue) {
                        config.contactColorList = newValue
                        settingsContactColorListIcon.setImageResource(getContactsColorListIcon(config.contactColorList))
                        config.needRestart = true
                    }
                }
            }
        }
    }

    private fun setupColorSimIcons() = binding.apply {
        settingsColorSimCardIconsHolder.beGoneIf(!areMultipleSIMsAvailable())
        settingsColorSimCardIcons.isChecked = config.colorSimIcons
        settingsColorSimCardIconsHolder.setOnClickListener {
            settingsColorSimCardIcons.toggle()
            config.colorSimIcons = settingsColorSimCardIcons.isChecked
            settingsSimCardColorListHolder.beVisibleIf(config.colorSimIcons)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupSimCardColorList() = binding.apply {
        settingsSimCardColorListHolder.beVisibleIf(config.colorSimIcons && areMultipleSIMsAvailable())
        settingsSimCardColorListIcon1.setColorFilter(config.simIconsColors[1])
        settingsSimCardColorListIcon2.setColorFilter(config.simIconsColors[2])
        if (isPro()) {
            settingsSimCardColorListIcon1.setOnClickListener {
                ColorPickerDialog(
                    this@SettingsActivity,
                    config.simIconsColors[1],
                    addDefaultColorButton = true,
                    colorDefault = resources.getColor(R.color.ic_dialer, theme),
                    title = resources.getString(R.string.color_sim_card_icons)
                ) { wasPositivePressed, color, wasDefaultPressed ->
                    if (wasPositivePressed || wasDefaultPressed) {
                        if (hasColorChanged(config.simIconsColors[1], color)) {
                            addSimCardColor(1, color)
                            settingsSimCardColorListIcon1.setColorFilter(color)
                        }
                    }
                }
            }
            settingsSimCardColorListIcon2.setOnClickListener {
                ColorPickerDialog(
                    this@SettingsActivity,
                    config.simIconsColors[2],
                    addDefaultColorButton = true,
                    colorDefault = resources.getColor(R.color.color_primary, theme),
                    title = resources.getString(R.string.color_sim_card_icons)
                ) { wasPositivePressed, color, wasDefaultPressed ->
                    if (wasPositivePressed || wasDefaultPressed) {
                        if (hasColorChanged(config.simIconsColors[2], color)) {
                            addSimCardColor(2, color)
                            settingsSimCardColorListIcon2.setColorFilter(color)
                        }
                    }
                }
            }
        } else {
            settingsSimCardColorListLabel.text =
                "${getString(R.string.change_color)} (${getString(R.string.feature_locked)})"
            arrayOf(
                settingsSimCardColorListIcon1,
                settingsSimCardColorListIcon2
            ).forEach { view ->
                view.setOnClickListener {
                    RxAnimation.from(view)
                        .shake(shakeTranslation = 2f)
                        .subscribe()

                    showSnackbar(binding.root)
                }
            }
        }
    }

    private fun addSimCardColor(index: Int, color: Int) {
        val recentColors = config.simIconsColors

        recentColors.removeAt(index)
        recentColors.add(index, color)

        baseConfig.simIconsColors = recentColors
    }

    private fun hasColorChanged(old: Int, new: Int) = abs(old - new) > 1

    private fun setupTipJar() = binding.apply {
        settingsTipJarHolder.apply {
            beVisibleIf(isPro())
            background.applyColorFilter(getColoredMaterialStatusBarColor().lightenColor(4))

        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        updateMenuItemColors(menu)
        return super.onCreateOptionsMenu(menu)
    }
}
