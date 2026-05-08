package com.smsnew.messenger.activities

import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.content.res.ColorStateList
import android.graphics.drawable.Icon
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.provider.Telephony
import android.speech.RecognizerIntent
import android.text.TextUtils
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.smsnew.messenger.BuildConfig
import com.smsnew.messenger.R
import com.smsnew.messenger.adapters.ConversationsAdapter
import com.smsnew.messenger.adapters.SearchResultsAdapter
import com.smsnew.messenger.commonsLibCustom.activities.ManageBlockedNumbersActivity
import com.smsnew.messenger.commonsLibCustom.dialogs.ConfirmationAdvancedDialog
import com.smsnew.messenger.commonsLibCustom.dialogs.PermissionRequiredDialog
import com.smsnew.messenger.commonsLibCustom.extensions.*
import com.smsnew.messenger.commonsLibCustom.helpers.*
import com.smsnew.messenger.commonsLibCustom.models.BlockedNumber
import com.smsnew.messenger.commonsLibCustom.views.MyMaterialSwitch
import com.smsnew.messenger.databinding.ActivityMainBinding
import com.smsnew.messenger.extensions.*
import com.smsnew.messenger.helpers.SEARCHED_MESSAGE_ID
import com.smsnew.messenger.helpers.THREAD_ID
import com.smsnew.messenger.helpers.THREAD_TITLE
import com.smsnew.messenger.models.Conversation
import com.smsnew.messenger.models.Events
import com.smsnew.messenger.models.Message
import com.smsnew.messenger.models.SearchResult
import com.smsnew.messenger.workers.OtpAutoDeleteWorker
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Objects

class MainActivity : SimpleActivity() {
    companion object {
        @Volatile private var processLastTelephonySyncMs: Long = 0L
        @Volatile private var processIsLoadInFlight: Boolean = false
    }
    override var isSearchBarEnabled = true

    private val MAKE_DEFAULT_APP_REQUEST = 1

    private var storedPrimaryColor = 0
    private var storedTextColor = 0
    private var storedBackgroundColor = 0
    private var storedFontSize = 0
    private var storedEllipsizeMode = ELLIPSIZE_MODE_END
    private var lastSearchedText = ""
    private var bus: EventBus? = null
    private var isSpeechToTextAvailable = false

    private val binding by viewBinding(ActivityMainBinding::inflate)

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        appLaunched(BuildConfig.APPLICATION_ID)
        Log.e("=====","===="+baseConfig.useColoredContacts);
        setupOptionsMenu()
        setupNavigationDrawer()
        refreshMenuItems()

        binding.mainMenu.updateTitle(getString(R.string.messages))
        binding.mainMenu.searchBeVisibleIf(config.showSearchBar)
        setupEdgeToEdge(padBottomImeAndSystem = listOf(binding.conversationsList, binding.searchResultsList))

        if (config.changeColourTopBar) {
            val useSurfaceColor = isDynamicTheme() && !isSystemInDarkMode()
            setupSearchMenuScrollListener(
                scrollingView = binding.conversationsList,
                searchMenu = binding.mainMenu,
                surfaceColor = useSurfaceColor
            )
        }

        storeStateVariables()

        checkAndDeleteOldRecycleBinMessages()
        if (config.deleteOtpAfter24Hours) {
            OtpAutoDeleteWorker.schedule(applicationContext)
        }
        clearAllMessagesIfNeeded {
            loadMessages()
        }
    }

    @SuppressLint("UnsafeIntentLaunch")
    override fun onResume() {
        super.onResume()

        if (config.needRestart || storedBackgroundColor != getProperBackgroundColor()) {
            finish()
            startActivity(intent)
            return
        }

        updateMenuColors()
        refreshMenuItems()

        getOrCreateConversationsAdapter().apply {
            if (storedPrimaryColor != getProperPrimaryColor()) {
                updatePrimaryColor()
            }

            if (storedTextColor != getProperTextColor()) {
                updateTextColor(getProperTextColor())
            }

            if (storedBackgroundColor != getProperBackgroundColor()) {
                updateBackgroundColor(getProperBackgroundColor())
            }

            if (storedFontSize != config.fontSize) {
                updateFontSize()
            }

            if (storedEllipsizeMode != config.ellipsizeMode) {
                updateEllipsizeMode()
            }

            updateDrafts()
        }

        updateTextColors(binding.mainCoordinator)
        binding.searchHolder.setBackgroundColor(getProperBackgroundColor())

        val properPrimaryColor = getProperPrimaryColor()
        binding.noConversationsPlaceholder2.setTextColor(properPrimaryColor)
        binding.noConversationsPlaceholder2.underlineText()
        binding.conversationsFastscroller.updateColors(getProperAccentColor())
        binding.conversationsProgressBar.setIndicatorColor(properPrimaryColor)
        binding.conversationsProgressBar.trackColor = properPrimaryColor.adjustAlpha(LOWER_ALPHA)
        checkShortcut()

        binding.conversationsList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                hideKeyboard()
            }
        })

        binding.searchResultsList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                hideKeyboard()
            }
        })

        val params = binding.mainMenu.layoutParams as AppBarLayout.LayoutParams
        params.scrollFlags = if (config.hideTopBarWhenScroll) {
            AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
                AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
        } else 0
        binding.mainMenu.layoutParams = params

        // ============ NEW: Cache se silent refresh - NO loading bar ============
        // Conversation se wapas aane par updates dikhane ke liye
        // Sirf tab jab adapter pehle se ready ho (matlab pehli baar nahi)
        if (binding.conversationsList.adapter != null) {
            refreshFromCacheOnly()
        }
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
    }

    override fun onDestroy() {
        super.onDestroy()
        config.needRestart = false
        bus?.unregister(this)
    }

    override fun onBackPressedCompat(): Boolean {
        return when {
            binding.mainDrawerLayout.isDrawerOpen(GravityCompat.START) -> {
                binding.mainDrawerLayout.closeDrawer(GravityCompat.START)
                true
            }
            binding.mainMenu.isSearchOpen -> {
                binding.mainMenu.closeSearch()
                true
            }
            else -> {
                appLockManager.lock()
                false
            }
        }
    }

    private fun setupOptionsMenu() {
        binding.apply {
            mainMenu.requireToolbar().inflateMenu(R.menu.menu_main)
//            mainMenu.toggleHideOnScroll(config.hideTopBarWhenScroll)

            if (baseConfig.useSpeechToText) {
                isSpeechToTextAvailable = isSpeechToTextAvailable()
                mainMenu.showSpeechToText = isSpeechToTextAvailable
            }
            mainMenu.setupMenu()

            mainMenu.onSpeechToTextClickListener = {
                speechToText()
            }

            mainMenu.onSearchClosedListener = {
                fadeOutSearch()
            }

            mainMenu.onSearchTextChangedListener = { text ->
                if (text.isNotEmpty()) {
                    if (binding.searchHolder.alpha < 1f) {
                        binding.searchHolder.fadeIn()
                    }
                } else {
                    fadeOutSearch()
                }
                searchTextChanged(text)
                mainMenu.clearSearch()
            }

            mainMenu.requireToolbar().setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.show_recycle_bin -> launchRecycleBin()
                    R.id.show_archived -> launchArchivedConversations()
                    R.id.show_blocked_numbers -> showBlockedNumbers()
                    R.id.settings -> launchSettings()
                    else -> return@setOnMenuItemClickListener false
                }
                return@setOnMenuItemClickListener true
            }

            mainMenu.clearSearch()
        }
    }

    private fun refreshMenuItems() {
        binding.mainMenu.requireToolbar().menu.apply {
            findItem(R.id.show_recycle_bin).isVisible = config.useRecycleBin
            findItem(R.id.show_archived).isVisible = config.isArchiveAvailable
            findItem(R.id.show_blocked_numbers).title =
                if (config.showBlockedNumbers) getString(R.string.hide_blocked_numbers)
                else getString(R.string.show_blocked_numbers)
        }

        // Mirror to drawer menu (same item IDs where applicable)
        binding.mainNavigationView.menu.apply {
            findItem(R.id.show_recycle_bin)?.isVisible = config.useRecycleBin
            findItem(R.id.show_archived)?.isVisible = config.isArchiveAvailable

            // Drawer toggle: title stays fixed. Sync the action-layout switch
            // to current config value WITHOUT firing the listener.
            findItem(R.id.show_blocked_numbers)?.let { item ->
                item.title = getString(R.string.show_blocked_numbers)
                setDrawerBlockedSwitchSilently(config.showBlockedNumbers)
            }
        }
    }

    /** Returns the MyMaterialSwitch attached as actionView on the drawer's
     *  show_blocked_numbers item, or null if drawer hasn't inflated yet. */
    private fun getDrawerBlockedSwitch(): MyMaterialSwitch? {
        val item = binding.mainNavigationView.menu.findItem(R.id.show_blocked_numbers)
            ?: return null
        return item.actionView?.findViewById(R.id.drawer_blocked_switch)
    }

    /** Sets the drawer switch's checked state without triggering its
     *  setOnCheckedChangeListener — used for programmatic syncs (e.g. when
     *  refreshing from config). */
    private fun setDrawerBlockedSwitchSilently(checked: Boolean) {
        val sw = getDrawerBlockedSwitch() ?: return
        sw.setOnCheckedChangeListener(null)
        sw.isChecked = checked
        attachBlockedSwitchListener(sw)
    }

    /** Attach the standard checked-change listener that drives showBlockedNumbers. */
    private fun attachBlockedSwitchListener(sw: MyMaterialSwitch) {
        sw.setOnCheckedChangeListener { _, isChecked ->
            // Only act if state actually differs from config — guards against
            // the listener firing during silent syncs and re-entrancy.
            if (config.showBlockedNumbers != isChecked) {
                showBlockedNumbers(isChecked)
            }
        }
    }

    // ============================================================================
    //  Navigation drawer
    // ============================================================================

    // Guard against rapid double-clicks on drawer items while the close animation
    // is running — without this, two activities can stack up.
    private var isDrawerNavigating = false

    private fun setupNavigationDrawer() {
        // Hamburger icon on the toolbar
        val toolbar = binding.mainMenu.requireToolbar()
        toolbar.setNavigationIcon(R.drawable.ic_menu_hamburger)
        toolbar.contentDescription = getString(R.string.app_name)
        toolbar.setNavigationOnClickListener {
            if (binding.mainDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.mainDrawerLayout.closeDrawer(GravityCompat.START)
            } else {
                binding.mainDrawerLayout.openDrawer(GravityCompat.START)
            }
        }

        // Reset the navigation guard whenever the drawer fully closes — handles the
        // case where user opened drawer, navigated, came back: ready for next click.
        binding.mainDrawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerClosed(drawerView: android.view.View) {
                isDrawerNavigating = false
            }
        })

        // Drawer item clicks — IDs match menu_main.xml so same handlers apply
        binding.mainNavigationView.setNavigationItemSelectedListener { menuItem ->
            // Toggle item: tapping the row toggles the switch. The switch's
            // setOnCheckedChangeListener will then drive showBlockedNumbers().
            // Drawer stays open for clear feedback that this is a setting.
            if (menuItem.itemId == R.id.show_blocked_numbers) {
                getDrawerBlockedSwitch()?.toggle()
                return@setNavigationItemSelectedListener true
            }

            // For navigation items: guard against rapid double-clicks while close
            // animation is running (without this, two activities can stack up).
            if (isDrawerNavigating) return@setNavigationItemSelectedListener false
            isDrawerNavigating = true

            // Close drawer for snappy UI
            binding.mainDrawerLayout.closeDrawer(GravityCompat.START)

            // Defer the launch slightly so the drawer close animation can run
            // smoothly without contending with the activity transition.
            binding.mainDrawerLayout.postDelayed({
                when (menuItem.itemId) {
                    R.id.show_recycle_bin -> launchRecycleBin()
                    R.id.show_archived -> launchArchivedConversations()
                    R.id.manage_blocked_numbers -> launchManageBlockedNumbers()
                    R.id.settings -> launchSettings()
                    R.id.about -> {
                        // No AboutActivity in project — kept for parity with
                        // menu_main.xml. Wire to your About screen here when added.
                    }
                }
            }, 220L)

            true
        }

        // Header — set text color and binding manually so it follows the theme
        applyDrawerColors()
    }

    /** Re-tints the drawer (background, header, item text + icons, switch) to
     *  match the current app theme, and ensures the toggle switch is wired up.
     *  Called from setupNavigationDrawer() and from onResume() via
     *  updateMenuColors() so it follows theme changes. */
    private fun applyDrawerColors() {
        val backgroundColor = getProperBackgroundColor()
        val textColor = getProperTextColor()
        val primaryColor = getProperPrimaryColor()

        binding.mainNavigationView.apply {
            setBackgroundColor(backgroundColor)

            // Item text colors: normal + selected (highlight uses primary)
            itemTextColor = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf()
                ),
                intArrayOf(primaryColor, textColor)
            )

            // Icon tints: same scheme
            itemIconTintList = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf()
                ),
                intArrayOf(primaryColor, textColor)
            )

            // Selection background ripple — light tint of primary
            itemBackground = null  // let the system draw default ripple

            // Header color (if header view exists)
            getHeaderView(0)?.apply {
                setBackgroundColor(getSurfaceColor())
                findViewById<android.widget.TextView>(R.id.nav_header_title)?.setTextColor(textColor)
            }
        }

        // Tint the toggle switch + ensure its listener is attached + initial state
        // is in sync with config. This runs on every onResume so theme changes from
        // SettingsActivity flow through here automatically.
        getDrawerBlockedSwitch()?.let { sw ->
            // Theme-aware track + thumb colors
            val checkedTrack = primaryColor
            val uncheckedTrack = textColor.adjustAlpha(0.25f)
            sw.trackTintList = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf()
                ),
                intArrayOf(checkedTrack, uncheckedTrack)
            )
            val checkedThumb = primaryColor.getContrastColor()
            val uncheckedThumb = textColor
            sw.thumbTintList = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf()
                ),
                intArrayOf(checkedThumb, uncheckedThumb)
            )

            // Re-attach listener (idempotent — clears any previous listener first
            // via setDrawerBlockedSwitchSilently → attachBlockedSwitchListener).
            setDrawerBlockedSwitchSilently(config.showBlockedNumbers)
        }

        // Hamburger icon tint should match toolbar icon color (text color)
        binding.mainMenu.requireToolbar().navigationIcon?.setTint(textColor)
    }
    // ============================================================================

    private fun showBlockedNumbers(newState: Boolean? = null) {
        val target = newState ?: !config.showBlockedNumbers
        if (config.showBlockedNumbers == target && newState != null) {
            return
        }
        config.showBlockedNumbers = target
        binding.mainMenu.requireToolbar().menu.findItem(R.id.show_blocked_numbers).title =
            if (target) getString(R.string.hide_blocked_numbers)
            else getString(R.string.show_blocked_numbers)
        getDrawerBlockedSwitch()?.isChecked = target
        val adapter = binding.conversationsList.adapter as? ConversationsAdapter
        if (adapter == null) {
            refreshFromCacheOnly()
            return
        }
        ensureBackgroundThread {
            try {
                val allCached = conversationsDB.getNonArchived()
                    .sortedByDescending { it.date }

                // Block status sync karo
                val currentBlockedNumbers = try {
                    getBlockedNumbers()
                } catch (_: Exception) {
                    emptyList()
                }

                val updatedList = allCached.map { conv ->
                    val nowBlocked = try {
                        isNumberBlocked(conv.phoneNumber, currentBlockedNumbers as ArrayList<BlockedNumber>)
                    } catch (_: Exception) {
                        conv.isBlocked
                    }
                    if (conv.isBlocked != nowBlocked) {
                        val updatedConv = conv.copy(isBlocked = nowBlocked)
                        try {
                            conversationsDB.insertOrUpdate(updatedConv)
                        } catch (_: Exception) {
                        }
                        updatedConv
                    } else conv
                }

                val filtered = if (config.showBlockedNumbers) {
                    updatedList
                } else {
                    updatedList.filter { !it.isBlocked }
                }

                val finalList = filtered.toMutableList() as ArrayList<Conversation>

                synchronized(visibleConversations) {
                    visibleConversations.clear()
                    visibleConversations.addAll(finalList)
                }

                runOnUiThread {
                    setupConversations(finalList, cached = true)
                }
            } catch (_: Exception) {
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == MAKE_DEFAULT_APP_REQUEST) {
            if (resultCode == RESULT_OK) {
                askPermissions()
            } else {
                finish()
            }
        } else if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK) {
            if (resultData != null) {
                val res: ArrayList<String> =
                    resultData.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) as ArrayList<String>

                val speechToText = Objects.requireNonNull(res)[0]
                if (speechToText.isNotEmpty()) {
                    binding.mainMenu.setText(speechToText)
                }
            }
        }
    }

    private fun storeStateVariables() {
        storedPrimaryColor = getProperPrimaryColor()
        storedTextColor = getProperTextColor()
        storedBackgroundColor = getProperBackgroundColor()
        storedFontSize = config.fontSize
        storedEllipsizeMode = config.ellipsizeMode
        config.needRestart = false
    }

    private fun updateMenuColors() {
        val useSurfaceColor = isDynamicTheme() && !isSystemInDarkMode()
        val backgroundColor = if (useSurfaceColor) getSurfaceColor() else getProperBackgroundColor()
        val statusBarColor = if (config.changeColourTopBar) getRequiredStatusBarColor(useSurfaceColor) else backgroundColor
        binding.mainMenu.updateColors(statusBarColor, scrollingView?.computeVerticalScrollOffset() ?: 0)
        applyDrawerColors()
    }

    private fun loadMessages() {
//        if (!config.wasReminderWarningShown) {
//            ConfirmationAdvancedDialog(
//                activity = this,
//                messageId = R.string.warning_disclosure,
//                fromHtml = true,
//                positive = R.string.agree,
//                negative = R.string.disagree
//            ) {
//                if (it) {
//                    config.wasReminderWarningShown = true
//
//                    if (isQPlus()) {
//                        val roleManager = getSystemService(RoleManager::class.java)
//                        if (roleManager!!.isRoleAvailable(RoleManager.ROLE_SMS)) {
//                            if (roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
//                                askPermissions()
//                            } else {
//                                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
//                                startActivityForResult(intent, MAKE_DEFAULT_APP_REQUEST)
//                            }
//                        } else {
//                            toast(R.string.unknown_error_occurred)
//                            finish()
//                        }
//                    } else {
//                        if (Telephony.Sms.getDefaultSmsPackage(this) == packageName) {
//                            askPermissions()
//                        } else {
//                            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
//                            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
//                            startActivityForResult(intent, MAKE_DEFAULT_APP_REQUEST)
//                        }
//                    }
//                } else {
//                    finish()
//                }
//            }
//        } else {
            if (isQPlus()) {
                val roleManager = getSystemService(RoleManager::class.java)
                if (roleManager!!.isRoleAvailable(RoleManager.ROLE_SMS)) {
                    if (roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                        askPermissions()
                    } else {
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                        startActivityForResult(intent, MAKE_DEFAULT_APP_REQUEST)
                    }
                } else {
                    toast(R.string.unknown_error_occurred)
                    finish()
                }
            } else {
                if (Telephony.Sms.getDefaultSmsPackage(this) == packageName) {
                    askPermissions()
                } else {
                    val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                    intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
                    startActivityForResult(intent, MAKE_DEFAULT_APP_REQUEST)
                }
            }
//        }
    }

    // while SEND_SMS and READ_SMS permissions are mandatory, READ_CONTACTS is optional.
    // If we don't have it, we just won't be able to show the contact name in some cases
    private fun askPermissions() {
        handlePermission(PERMISSION_READ_SMS) {
            if (it) {
                handlePermission(PERMISSION_SEND_SMS) {
                    if (it) {
                        handlePermission(PERMISSION_READ_CONTACTS) {
                            handleNotificationPermission { granted ->
                                if (!granted) {
                                    PermissionRequiredDialog(
                                        activity = this,
                                        textId = R.string.allow_notifications_incoming_messages,
                                        positiveActionCallback = { openNotificationSettings() })
                                }
                            }

                            initMessenger()
                            bus = EventBus.getDefault()
                            try {
                                bus!!.register(this)
                            } catch (_: Exception) {
                            }
                        }
                    } else {
                        finish()
                    }
                }
            } else {
                finish()
            }
        }
    }

    private fun initMessenger(forceTelephonySync: Boolean = false) {
        getCachedConversations(forceTelephonySync = forceTelephonySync)
        binding.noConversationsPlaceholder2.setOnClickListener {
            launchNewConversation()
        }

        binding.conversationsFab.setOnClickListener {
            launchNewConversation()
        }
    }

    // Holds the currently visible conversation list — used by the streaming loader to
    // add/update/remove items one-by-one without re-reading the whole list every time.
    private val visibleConversations = ArrayList<Conversation>()

    // True while background sync (cache expansion + telephony sync) is running.
    // When true, showOrHideProgress keeps the progress bar visible even if
    // setupConversations is called mid-stream with intermediate snapshots.
    @Volatile
    private var isBackgroundSyncRunning = false

    // Guards against concurrent loads. If a load is already in flight, subsequent
    // triggers (EventBus refresh, menu toggles, etc.) won't start a second one on top.
    @Volatile
    private var isLoadInFlight = false

    // Timestamp of the last full telephony rescan. Used to throttle — we skip the
    // expensive telephony scan if it ran very recently, since nothing will have changed.
    private var lastTelephonySyncMs = 0L
    // Minimum time between full telephony rescans. Chosen to cover typical "navigate away
    // and come back" patterns (open a conversation, read, return) without re-syncing.
    // Incoming SMS/MMS will still refresh the list via the EventBus path regardless.
    private val TELEPHONY_SYNC_MIN_INTERVAL_MS = 60_000L

    // Throttle UI updates so AsyncListDiffer has time to process each diff before the next one.
    // AsyncListDiffer cancels in-flight diffs when a new submitList arrives — that's why rapid
    // updates appeared to do nothing. 150ms is enough for diff to complete on modest lists.
    private var lastUiPostTime = 0L
    private val UI_POST_THROTTLE_MS = 150L
    private val UI_POST_FORCE_EVERY = 10   // force an update every 10 items regardless of time
    private var pendingItemsSinceLastPost = 0

    /**
     * Main entry point for loading conversations.
     *
     * @param forceTelephonySync  If true, ALWAYS does a full telephony rescan. If false,
     *                            respects the throttle — cache-only refresh when a telephony
     *                            scan ran recently. Use true only for explicit user actions
     *                            like toggling blocked numbers.
     */
    private fun getCachedConversations(forceTelephonySync: Boolean = false) {
        // BUG FIX: Agar load already chal raha hai, skip karo
        // Lekin ye flag KABHI clear hi nahi hua to permanent block ho jayega
        if (processIsLoadInFlight) {
            return
        }

        val nowBeforeLoad = System.currentTimeMillis()
        val recentlySynced = (nowBeforeLoad - processLastTelephonySyncMs) < TELEPHONY_SYNC_MIN_INTERVAL_MS
        val needsFullSync = forceTelephonySync || !recentlySynced

        processIsLoadInFlight = true
        isLoadInFlight = true

        ensureBackgroundThread {
            try {
                val allCached = try {
                    conversationsDB.getNonArchived().sortedByDescending { it.date }
                } catch (_: Exception) {
                    emptyList()
                }

                val mustSyncBecauseCacheEmpty = allCached.isEmpty()
                val shouldSyncTelephony = needsFullSync || mustSyncBecauseCacheEmpty

                // SILENT PATH: cache-only refresh, NO progress bar
                if (!shouldSyncTelephony) {
                    synchronized(visibleConversations) {
                        visibleConversations.clear()
                        visibleConversations.addAll(allCached)
                    }
                    runOnUiThread {
                        setupConversations(
                            ArrayList(allCached) as ArrayList<Conversation>,
                            cached = true
                        )
                    }
                    return@ensureBackgroundThread
                }

                // FULL SYNC PATH
                val firstBatch = allCached.take(100).toMutableList() as ArrayList<Conversation>
                synchronized(visibleConversations) {
                    visibleConversations.clear()
                    visibleConversations.addAll(firstBatch)
                }
                runOnUiThread {
                    setupConversations(ArrayList(firstBatch), cached = true)
                }

                showBackgroundSyncProgress(true)

                if (allCached.size > 100) {
                    val remaining = allCached.drop(100)
                    remaining.chunked(25).forEach { chunk ->
                        synchronized(visibleConversations) {
                            visibleConversations.addAll(chunk)
                        }
                        postVisibleConversationsToUi(cached = true, force = true)
                        try {
                            Thread.sleep(200)
                        } catch (_: InterruptedException) {
                        }
                    }
                }

                val archived = try {
                    conversationsDB.getAllArchived()
                } catch (_: Exception) {
                    listOf()
                }

                streamNewConversations(
                    (allCached + archived).toMutableList() as ArrayList<Conversation>
                )

                val syncCompleteTime = System.currentTimeMillis()
                lastTelephonySyncMs = syncCompleteTime
                processLastTelephonySyncMs = syncCompleteTime

                allCached.forEach {
                    clearExpiredScheduledMessages(it.threadId)
                }

                showBackgroundSyncProgress(false)
            } catch (e: Exception) {
            } finally {
                // CRITICAL: Flag ko HAMESHA clear karo, even on exception
                isLoadInFlight = false
                processIsLoadInFlight = false
            }
        }
    }

    private fun showBackgroundSyncProgress(show: Boolean) {
        isBackgroundSyncRunning = show
        runOnUiThread {
            if (show) {
                binding.conversationsProgressBar.show()
            } else {
                binding.conversationsProgressBar.hide()
            }
        }
    }

    private fun postVisibleConversationsToUi(cached: Boolean = false, force: Boolean = false) {
        pendingItemsSinceLastPost++
        val now = System.currentTimeMillis()
        val enoughTimePassed = now - lastUiPostTime >= UI_POST_THROTTLE_MS
        val enoughItems = pendingItemsSinceLastPost >= UI_POST_FORCE_EVERY

        if (!force && !enoughTimePassed && !enoughItems) {
            return
        }

        lastUiPostTime = now
        pendingItemsSinceLastPost = 0

        val snapshot: ArrayList<Conversation>
        synchronized(visibleConversations) {
            snapshot = ArrayList(visibleConversations)
        }
        runOnUiThread {
            setupConversations(snapshot, cached = cached)
        }
    }

    // Kept for external callers (like EventBus refresh) — delegates to streaming implementation
    private fun getNewConversations(cachedConversations: ArrayList<Conversation>) {
        ensureBackgroundThread {
            streamNewConversations(cachedConversations)
        }
    }

    /**
     * MUST be called from a background thread.
     * Streams conversations to the UI one by one:
     *   insert to DB → add to visible list → refresh UI → next conversation
     * This gives the user a "live loading" feel instead of a long wait followed by a dump.
     */
    private fun streamNewConversations(cachedConversations: ArrayList<Conversation>) {
        val privateCursor = getMyContactsCursor(favoritesOnly = false, withPhoneNumbersOnly = true)
        val privateContacts = MyContactsContentProvider.getSimpleContacts(this, privateCursor)

        // Build cached lookup ONCE up front — used inside the streaming callback
        val cachedThreadIds = cachedConversations.map { it.threadId }.toHashSet()

        // Buffer for batching streamed items — we flush every N items to the UI so
        // AsyncListDiffer has time to process each batch.
        val pendingBatch = mutableListOf<Conversation>()
        val STREAM_BATCH_SIZE = 10

        // STEP 1 (streaming): As each conversation comes off the cursor inside
        // getConversations(), insert it to DB immediately and flush batches to the UI.
        // This is what actually makes conversations appear on screen WHILE loading.
        val conversations = getConversations(
            privateContacts = privateContacts,
            onConversation = { conv ->
                if (conv.threadId !in cachedThreadIds) {
                    conversationsDB.insertOrUpdate(conv)
                    cachedConversations.add(conv)
                    pendingBatch.add(conv)

                    // When batch reaches the threshold, flush to UI
                    if (pendingBatch.size >= STREAM_BATCH_SIZE) {
                        synchronized(visibleConversations) {
                            pendingBatch.forEach { c ->
                                if (visibleConversations.none { it.threadId == c.threadId }) {
                                    visibleConversations.add(c)
                                }
                            }
                        }
                        pendingBatch.clear()
                        postVisibleConversationsToUi(force = true)
                    }
                }
            }
        )

        // Flush any remaining items in the final partial batch
        if (pendingBatch.isNotEmpty()) {
            synchronized(visibleConversations) {
                pendingBatch.forEach { c ->
                    if (visibleConversations.none { it.threadId == c.threadId }) {
                        visibleConversations.add(c)
                    }
                }
            }
            pendingBatch.clear()
            postVisibleConversationsToUi(force = true)
        }

        // Build remaining lookup structures now that we have the full list
        val newThreadIds = conversations.map { it.threadId }.toHashSet()
        val newConvByPhone = conversations.associateBy { it.phoneNumber }
        val newConvByThreadId = conversations.associateBy { it.threadId }

        // STEP 2: Handle deletions and temporary-thread migration (DB-only work,
        // no intermediate UI updates — final refresh happens in STEP 4).
        cachedConversations.toList().forEach { cachedConversation ->
            val threadId = cachedConversation.threadId
            val isTemporaryThread = cachedConversation.isScheduled
            val isConversationDeleted = threadId !in newThreadIds

            if (isConversationDeleted && !isTemporaryThread) {
                conversationsDB.deleteThreadId(threadId)
                synchronized(visibleConversations) {
                    visibleConversations.removeAll { it.threadId == threadId }
                }
            }

            val newConversation = newConvByPhone[cachedConversation.phoneNumber]
            if (isTemporaryThread && newConversation != null) {
                // delete the original temporary thread and move any scheduled messages
                // to the new thread
                conversationsDB.deleteThreadId(threadId)
                messagesDB.getScheduledThreadMessages(threadId)
                    .forEach { message ->
                        messagesDB.insertOrUpdate(
                            message.copy(threadId = newConversation.threadId)
                        )
                    }
                insertOrUpdateConversation(newConversation, cachedConversation)

                synchronized(visibleConversations) {
                    visibleConversations.removeAll { it.threadId == threadId }
                    if (visibleConversations.none { it.threadId == newConversation.threadId }) {
                        visibleConversations.add(newConversation)
                    }
                }
            }
        }

        // STEP 3: Update changed conversations (DB-only work, UI refreshed in STEP 4)
        cachedConversations.forEach { cachedConv ->
            val conv = newConvByThreadId[cachedConv.threadId]
            if (conv != null && !Conversation.areContentsTheSame(old = cachedConv, new = conv)) {
                // FIXME: Scheduled message date is being reset here. Conversations with
                //  scheduled messages will have their original date.
                insertOrUpdateConversation(conv)

                synchronized(visibleConversations) {
                    val idx = visibleConversations.indexOfFirst { it.threadId == conv.threadId }
                    if (idx >= 0) {
                        visibleConversations[idx] = conv
                    } else {
                        visibleConversations.add(conv)
                    }
                }
            }
        }

        // STEP 4: Final sync with DB truth to cover any edge cases.
        // Use force=true to bypass the throttle so the final authoritative list ALWAYS renders.
        val allConversations = conversationsDB.getNonArchived() as ArrayList<Conversation>
        synchronized(visibleConversations) {
            visibleConversations.clear()
            visibleConversations.addAll(allConversations)
        }
        postVisibleConversationsToUi(force = true)


        if (config.appRunCount == 1) {
            conversations
                .sortedByDescending { it.date }
                .take(30)
                .map { it.threadId }
                .forEach { threadId ->
                    val messages = getMessages(threadId, includeScheduledMessages = false)
                    messages.chunked(30).forEach { currentMessages ->
                        messagesDB.insertMessages(*currentMessages.toTypedArray())
                    }
                }
        }
    }

    private fun getOrCreateConversationsAdapter(): ConversationsAdapter {
        if (isDynamicTheme() && !isSystemInDarkMode()) {
            binding.conversationsList.setBackgroundColor(getSurfaceColor())
        }

        var currAdapter = binding.conversationsList.adapter
        if (currAdapter == null) {
            hideKeyboard()
            currAdapter = ConversationsAdapter(
                activity = this,
                recyclerView = binding.conversationsList,
                onRefresh = { notifyDatasetChanged() },
                itemClick = { handleConversationClick(it) }
            )

            binding.conversationsList.adapter = currAdapter
            if (areSystemAnimationsEnabled) {
                binding.conversationsList.scheduleLayoutAnimation()
            }
        }
        return currAdapter as ConversationsAdapter
    }

    private fun setupConversations(
        conversations: ArrayList<Conversation>,
        cached: Boolean = false,
    ) {
        val sortedConversations = if (config.unreadAtTop) {
            conversations.sortedWith(
                compareByDescending<Conversation> {
                    config.pinnedConversations.contains(it.threadId.toString())
                }
                    .thenBy { it.read }
                    .thenByDescending { it.date }
            ).toMutableList() as ArrayList<Conversation>
        } else {
            conversations.sortedWith(
                compareByDescending<Conversation> {
                    config.pinnedConversations.contains(it.threadId.toString())
                }
                    .thenByDescending { it.date }
                    .thenByDescending { it.isGroupConversation }
            ).toMutableList() as ArrayList<Conversation>
        }

        // Sirf first run + empty list pe progress dikhao
        if (cached && config.appRunCount == 1 && conversations.isEmpty()) {
            showOrHideProgress(true)
        } else {
            showOrHideProgress(false)
            if (!cached) {
                showOrHidePlaceholder(conversations.isEmpty())
            }
        }

        try {
            getOrCreateConversationsAdapter().apply {
                updateConversations(sortedConversations) {
                    if (!cached) {
                        showOrHidePlaceholder(currentList.isEmpty())
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun showOrHideProgress(show: Boolean) {
        if (show) {
            binding.conversationsProgressBar.show()
            binding.noConversationsPlaceholder.beVisible()
            binding.noConversationsPlaceholder.text = getString(R.string.loading_messages)
        } else {
            // Don't hide the progress bar if background sync is still running —
            // it's still doing useful work behind the scenes and the user should see that.
            if (!isBackgroundSyncRunning) {
                binding.conversationsProgressBar.hide()
            }
            binding.noConversationsPlaceholder.beGone()
        }
    }

    private fun showOrHidePlaceholder(show: Boolean) {
        binding.conversationsFastscroller.beGoneIf(show)
        binding.noConversationsPlaceholder.beVisibleIf(show)
        binding.noConversationsPlaceholder.text = getString(R.string.no_conversations_found)
        binding.noConversationsPlaceholder2.beVisibleIf(show)
    }

    private fun fadeOutSearch() {
        binding.searchHolder.animate()
            .alpha(0f)
            .setDuration(SHORT_ANIMATION_DURATION)
            .withEndAction {
                binding.searchHolder.beGone()
                searchTextChanged("", true)
            }.start()
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
            startActivity(this)
        }
    }

    private fun launchNewConversation() {
        hideKeyboard()
        Intent(this, NewConversationActivity::class.java).apply {
            startActivity(this)
        }
    }

    private fun checkShortcut() {
        val iconColor = getProperPrimaryColor()
        if (config.lastHandledShortcutColor != iconColor) {
            val newConversation = getCreateNewContactShortcut(iconColor)

            val manager = getSystemService(ShortcutManager::class.java)
            try {
                manager.dynamicShortcuts = listOf(newConversation)
                config.lastHandledShortcutColor = iconColor
            } catch (_: Exception) {
            }
        }
    }

    @SuppressLint("NewApi")
    private fun getCreateNewContactShortcut(iconColor: Int): ShortcutInfo {
        val newEvent = getString(R.string.new_conversation)
        val drawable =
            AppCompatResources.getDrawable(this, R.drawable.shortcut_plus)

        (drawable as LayerDrawable).findDrawableByLayerId(R.id.shortcut_plus_background)
            .applyColorFilter(iconColor)
        val bmp = drawable.convertToBitmap()

        val intent = Intent(this, NewConversationActivity::class.java)
        intent.action = Intent.ACTION_VIEW
        return ShortcutInfo.Builder(this, "new_conversation")
            .setShortLabel(newEvent)
            .setLongLabel(newEvent)
            .setIcon(Icon.createWithBitmap(bmp))
            .setIntent(intent)
            .setRank(0)
            .build()
    }

    private fun searchTextChanged(text: String, forceUpdate: Boolean = false) {
        if (!binding.mainMenu.isSearchOpen && !forceUpdate) {
            return
        }

        lastSearchedText = text
        binding.searchPlaceholder2.beGoneIf(text.length >= 2)
        if (text.length >= 2) {
            ensureBackgroundThread {
                val searchQuery = "%$text%"
                val messages = messagesDB.getMessagesWithText(searchQuery)
                val conversations = conversationsDB.getConversationsWithText(searchQuery)
                if (text == lastSearchedText) {
                    showSearchResults(messages, conversations, text)
                }
            }
        } else {
            binding.searchPlaceholder.beVisible()
            binding.searchResultsList.beGone()
        }
        binding.mainMenu.clearSearch()
    }

    private fun showSearchResults(
        messages: List<Message>,
        conversations: List<Conversation>,
        searchedText: String,
    ) {
        val searchResults = ArrayList<SearchResult>()
        conversations.forEach { conversation ->
            val date = (conversation.date * 1000L).formatDateOrTime(
                context = this,
                hideTimeOnOtherDays = true,
                showCurrentYear = true
            )

            val searchResult = SearchResult(
                messageId = -1,
                title = conversation.title,
                phoneNumber = conversation.phoneNumber,
                snippet = conversation.phoneNumber,
                date = date,
                threadId = conversation.threadId,
                photoUri = conversation.photoUri,
                isCompany = conversation.isCompany,
                isBlocked = conversation.isBlocked
            )
            searchResults.add(searchResult)
        }

        messages.sortedByDescending { it.id }.forEach { message ->
            var recipient = message.senderName
            if (recipient.isEmpty() && message.participants.isNotEmpty()) {
                val participantNames = message.participants.map { it.name }
                recipient = TextUtils.join(", ", participantNames)
            }

            val phoneNumber = message.participants.firstOrNull()!!.phoneNumbers.firstOrNull()!!.normalizedNumber
            val date = (message.date * 1000L).formatDateOrTime(
                context = this,
                hideTimeOnOtherDays = true,
                showCurrentYear = true
            )
            val isCompany =
                if (message.participants.size == 1) message.participants.first().isABusinessContact() else false

            val searchResult = SearchResult(
                messageId = message.id,
                title = recipient,
                phoneNumber = phoneNumber,
                snippet = message.body,
                date = date,
                threadId = message.threadId,
                photoUri = message.senderPhotoUri,
                isCompany = isCompany
            )
            searchResults.add(searchResult)
        }

        runOnUiThread {
            binding.searchResultsList.beVisibleIf(searchResults.isNotEmpty())
            binding.searchPlaceholder.beVisibleIf(searchResults.isEmpty())

            val currAdapter = binding.searchResultsList.adapter
            if (currAdapter == null) {
                SearchResultsAdapter(this, searchResults, binding.searchResultsList, searchedText) {
                hideKeyboard()
                    Intent(this, ThreadActivity::class.java).apply {
                        putExtra(THREAD_ID, (it as SearchResult).threadId)
                        putExtra(THREAD_TITLE, it.title)
                        putExtra(SEARCHED_MESSAGE_ID, it.messageId)
                        startActivity(this)
                    }
                }.apply {
                    binding.searchResultsList.adapter = this
                }
            } else {
                (currAdapter as SearchResultsAdapter).updateItems(searchResults, searchedText)
            }
        }
    }

    private fun launchRecycleBin() {
        hideKeyboard()
        val intent = Intent(applicationContext, RecycleBinConversationsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    private fun launchArchivedConversations() {
        hideKeyboard()
        val intent = Intent(applicationContext, ArchivedConversationsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    private fun launchSettings() {
        hideKeyboard()
        val intent = Intent(applicationContext, SettingsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    private fun launchManageBlockedNumbers() {
        hideKeyboard()
        val intent = Intent(applicationContext, ManageBlockedNumbersActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refreshConversations(@Suppress("unused") event: Events.RefreshConversations) {
        refreshFromCacheOnly()
    }


    private fun refreshFromCacheOnly() {
        ensureBackgroundThread {
            try {
                val allCached = conversationsDB.getNonArchived()
                    .sortedByDescending { it.date }

                // System se latest blocked numbers lao
                val currentBlockedNumbers = try {
                    getBlockedNumbers()
                } catch (_: Exception) {
                    emptyList()
                }

                // Har conversation ka block status LIVE check karo aur DB update karo
                val updatedList = allCached.map { conv ->
                    val nowBlocked = try {
                        isNumberBlocked(conv.phoneNumber, currentBlockedNumbers as ArrayList<BlockedNumber>)
                    } catch (_: Exception) {
                        conv.isBlocked
                    }

                    if (conv.isBlocked != nowBlocked) {
                        val updatedConv = conv.copy(isBlocked = nowBlocked)
                        try {
                            conversationsDB.insertOrUpdate(updatedConv)
                        } catch (_: Exception) {
                        }
                        updatedConv
                    } else {
                        conv
                    }
                }

                // Filter based on settings
                val filtered = if (config.showBlockedNumbers) {
                    updatedList
                } else {
                    updatedList.filter { !it.isBlocked }
                }

                val finalList = filtered.toMutableList() as ArrayList<Conversation>

                synchronized(visibleConversations) {
                    visibleConversations.clear()
                    visibleConversations.addAll(finalList)
                }

                runOnUiThread {
                    setupConversations(finalList, cached = true)
                }
            } catch (_: Exception) {
            }
        }
    }
}
