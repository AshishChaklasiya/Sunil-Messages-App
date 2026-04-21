package com.smsnew.messenger.commonsLibCustom.extensions

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.UserManager
import android.provider.BaseColumns
import android.provider.BlockedNumberContract.BlockedNumbers
import android.provider.ContactsContract.CommonDataKinds.*
import android.provider.MediaStore.*
import android.provider.OpenableColumns
import android.provider.Settings
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.loader.content.CursorLoader
import com.github.ajalt.reprint.core.Reprint
import com.smsnew.messenger.R
import com.smsnew.messenger.commonsLibCustom.helpers.*
import com.smsnew.messenger.commonsLibCustom.helpers.MyContentProvider.PERMISSION_WRITE_GLOBAL_SETTINGS
import com.smsnew.messenger.commonsLibCustom.models.AlarmSound
import com.smsnew.messenger.commonsLibCustom.models.BlockedNumber
import com.smsnew.messenger.commonsLibCustom.models.contacts.ContactRelation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.PatternSyntaxException
import kotlin.math.roundToInt
import com.smsnew.messenger.R as stringsR

fun Context.getSharedPrefs() = getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)

val Context.isRTLLayout: Boolean get() = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

val Context.areSystemAnimationsEnabled: Boolean get() = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f) > 0f

val Context.appLockManager
    get() = AppLockManager.getInstance(applicationContext as Application)

val Context.isCredentialStorageAvailable: Boolean
    get() = getSystemService(UserManager::class.java)?.isUserUnlocked ?: true

fun Context.toast(id: Int, length: Int = Toast.LENGTH_SHORT) {
    toast(getString(id), length)
}

fun Context.toast(msg: String, length: Int = Toast.LENGTH_SHORT) {
    try {
        if (isOnMainThread()) {
            doToast(this, msg, length)
        } else {
            Handler(Looper.getMainLooper()).post {
                doToast(this, msg, length)
            }
        }
    } catch (_: Exception) {
    }
}

private fun doToast(context: Context, message: String, length: Int) {
    if (context is Activity) {
        if (!context.isFinishing && !context.isDestroyed) {
            Toast.makeText(context, message, length).show()
        }
    } else {
        Toast.makeText(context, message, length).show()
    }
}

fun Context.showErrorToast(msg: String, length: Int = Toast.LENGTH_LONG) {
    toast(String.format(getString(R.string.error), msg), length)
}

fun Context.showErrorToast(exception: Exception, length: Int = Toast.LENGTH_LONG) {
    showErrorToast(exception.toString(), length)
}

val Context.baseConfig: BaseConfig get() = BaseConfig.newInstance(this)
val Context.sdCardPath: String get() = baseConfig.sdCardPath
val Context.internalStoragePath: String get() = baseConfig.internalStoragePath
val Context.otgPath: String get() = baseConfig.OTGPath

fun isFingerPrintSensorAvailable() = Reprint.isHardwarePresent()

fun Context.isBiometricIdAvailable(): Boolean = when (BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
    BiometricManager.BIOMETRIC_SUCCESS, BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> true
    else -> false
}

fun Context.isBiometricAuthSupported(): Boolean {
    return if (isRPlus()) {
        isBiometricIdAvailable()
    } else {
        isFingerPrintSensorAvailable()
    }
}


fun Context.hasPermission(permId: Int) = ContextCompat.checkSelfPermission(this, getPermissionString(permId)) == PERMISSION_GRANTED

@SuppressLint("InlinedApi")
fun getPermissionString(id: Int) = when (id) {
    PERMISSION_READ_STORAGE -> Manifest.permission.READ_EXTERNAL_STORAGE
    PERMISSION_WRITE_STORAGE -> Manifest.permission.WRITE_EXTERNAL_STORAGE
    PERMISSION_CAMERA -> Manifest.permission.CAMERA
    PERMISSION_RECORD_AUDIO -> Manifest.permission.RECORD_AUDIO
    PERMISSION_READ_CONTACTS -> Manifest.permission.READ_CONTACTS
    PERMISSION_WRITE_CONTACTS -> Manifest.permission.WRITE_CONTACTS
    PERMISSION_READ_CALENDAR -> Manifest.permission.READ_CALENDAR
    PERMISSION_WRITE_CALENDAR -> Manifest.permission.WRITE_CALENDAR
    PERMISSION_CALL_PHONE -> Manifest.permission.CALL_PHONE
    PERMISSION_READ_CALL_LOG -> Manifest.permission.READ_CALL_LOG
    PERMISSION_WRITE_CALL_LOG -> Manifest.permission.WRITE_CALL_LOG
    PERMISSION_GET_ACCOUNTS -> Manifest.permission.GET_ACCOUNTS
    PERMISSION_READ_SMS -> Manifest.permission.READ_SMS
    PERMISSION_SEND_SMS -> Manifest.permission.SEND_SMS
    PERMISSION_READ_PHONE_STATE -> Manifest.permission.READ_PHONE_STATE
    PERMISSION_MEDIA_LOCATION -> if (isQPlus()) Manifest.permission.ACCESS_MEDIA_LOCATION else ""
    PERMISSION_POST_NOTIFICATIONS -> Manifest.permission.POST_NOTIFICATIONS
    PERMISSION_READ_MEDIA_IMAGES -> Manifest.permission.READ_MEDIA_IMAGES
    PERMISSION_READ_MEDIA_VIDEO -> Manifest.permission.READ_MEDIA_VIDEO
    PERMISSION_READ_MEDIA_AUDIO -> Manifest.permission.READ_MEDIA_AUDIO
    PERMISSION_ACCESS_COARSE_LOCATION -> Manifest.permission.ACCESS_COARSE_LOCATION
    PERMISSION_ACCESS_FINE_LOCATION -> Manifest.permission.ACCESS_FINE_LOCATION
    PERMISSION_READ_MEDIA_VISUAL_USER_SELECTED -> Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
    PERMISSION_READ_SYNC_SETTINGS -> Manifest.permission.READ_SYNC_SETTINGS
    else -> ""
}

fun Context.launchActivityIntent(intent: Intent) {
    try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        toast(R.string.no_app_found)
    } catch (e: Exception) {
        showErrorToast(e)
    }
}

fun Context.getFilePublicUri(file: File, applicationId: String): Uri {
    // for images/videos/gifs try getting a media content uri first, like content://media/external/images/media/438
    // if media content uri is null, get our custom uri like content://com.goodwy.gallery.provider/external_files/emulated/0/DCIM/IMG_20171104_233915.jpg
    var uri = if (file.isMediaFile()) {
        getMediaContentUri(file.absolutePath)
    } else {
        getMediaContent(file.absolutePath, Files.getContentUri("external"))
    }

    if (uri == null) {
        uri = FileProvider.getUriForFile(this, "$applicationId.provider", file)
    }

    return uri!!
}

fun Context.getMediaContentUri(path: String): Uri? {
    val uri = when {
        path.isImageFast() -> Images.Media.EXTERNAL_CONTENT_URI
        path.isVideoFast() -> Video.Media.EXTERNAL_CONTENT_URI
        else -> Files.getContentUri("external")
    }

    return getMediaContent(path, uri)
}

fun Context.getMediaContent(path: String, uri: Uri): Uri? {
    val projection = arrayOf(Images.Media._ID)
    val selection = Images.Media.DATA + "= ?"
    val selectionArgs = arrayOf(path)
    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                val id = cursor.getIntValue(Images.Media._ID).toString()
                return Uri.withAppendedPath(uri, id)
            }
        }
    } catch (_: Exception) {
    }
    return null
}

fun Context.queryCursor(
    uri: Uri,
    projection: Array<String>,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    sortOrder: String? = null,
    showErrors: Boolean = false,
    callback: (cursor: Cursor) -> Unit
) {
    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
        cursor?.use {
            if (cursor.moveToFirst()) {
                do {
                    callback(cursor)
                } while (cursor.moveToNext())
            }
        }
    } catch (e: Exception) {
        if (showErrors) {
            showErrorToast(e)
        }
    }
}

fun Context.getFilenameFromUri(uri: Uri): String {
    return if (uri.scheme == "file") {
        File(uri.toString()).name
    } else {
        getFilenameFromContentUri(uri) ?: uri.lastPathSegment ?: ""
    }
}


fun Context.getFilenameFromContentUri(uri: Uri): String? {
    val projection = arrayOf(
        OpenableColumns.DISPLAY_NAME
    )

    try {
        val cursor = contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return cursor.getStringValue(OpenableColumns.DISPLAY_NAME)
            }
        }
    } catch (_: Exception) {
    }
    return null
}

fun Context.getSizeFromContentUri(uri: Uri): Long {
    val projection = arrayOf(OpenableColumns.SIZE)
    try {
        val cursor = contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return cursor.getLongValue(OpenableColumns.SIZE)
            }
        }
    } catch (_: Exception) {
    }
    return 0L
}

fun Context.getMyContentProviderCursorLoader() = CursorLoader(this, MyContentProvider.MY_CONTENT_URI, null, null, null, null)

fun Context.getMyContactsCursor(favoritesOnly: Boolean, withPhoneNumbersOnly: Boolean) = try {
    val getFavoritesOnly = if (favoritesOnly) "1" else "0"
    val getWithPhoneNumbersOnly = if (withPhoneNumbersOnly) "1" else "0"
    val args = arrayOf(getFavoritesOnly, getWithPhoneNumbersOnly)
    val uri =  MyContactsContentProvider.CONTACTS_CONTENT_URI
    CursorLoader(this, uri, null, null, args, null).loadInBackground()
} catch (_: Exception) {
    null
}

fun getCurrentFormattedDateTime(): String {
    val simpleDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    return simpleDateFormat.format(Date(System.currentTimeMillis()))
}

fun Context.updateSDCardPath() {
    ensureBackgroundThread {
        val oldPath = baseConfig.sdCardPath
        baseConfig.sdCardPath = getSDCardPath()
        if (oldPath != baseConfig.sdCardPath) {
            baseConfig.sdTreeUri = ""
        }
    }
}


fun Context.canAccessGlobalConfig(): Boolean {
    return isPro() && ContextCompat.checkSelfPermission(this, PERMISSION_WRITE_GLOBAL_SETTINGS) == PERMISSION_GRANTED
}

fun Context.addLockedLabelIfNeeded(stringId: Int, lock: Boolean = false): String {
    return if (lock) {
        getString(stringId)
    } else {
        "${getString(stringId)} (${getString(R.string.feature_locked)})"
    }
}

fun Context.isPackageInstalled(packageName: String?): Boolean {
    val packageManager = packageManager
    val intent = packageManager.getLaunchIntentForPackage(packageName!!) ?: return false
    val list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    return !list.isEmpty()
}

fun Context.getFormattedSeconds(seconds: Int, showBefore: Boolean = true) = when (seconds) {
    -1 -> getString(R.string.no_reminder)
    0 -> getString(R.string.at_start)
    else -> {
        when {
            seconds < 0 && seconds > -60 * 60 * 24 -> {
                val minutes = -seconds / 60
                getString(R.string.during_day_at, minutes / 60, minutes % 60)
            }

            seconds % YEAR_SECONDS == 0 -> {
                val base = if (showBefore) R.plurals.years_before else R.plurals.by_years
                resources.getQuantityString(base, seconds / YEAR_SECONDS, seconds / YEAR_SECONDS)
            }

            seconds % MONTH_SECONDS == 0 -> {
                val base = if (showBefore) R.plurals.months_before else R.plurals.by_months
                resources.getQuantityString(base, seconds / MONTH_SECONDS, seconds / MONTH_SECONDS)
            }

            seconds % WEEK_SECONDS == 0 -> {
                val base = if (showBefore) R.plurals.weeks_before else R.plurals.by_weeks
                resources.getQuantityString(base, seconds / WEEK_SECONDS, seconds / WEEK_SECONDS)
            }

            seconds % DAY_SECONDS == 0 -> {
                val base = if (showBefore) R.plurals.days_before else R.plurals.by_days
                resources.getQuantityString(base, seconds / DAY_SECONDS, seconds / DAY_SECONDS)
            }

            seconds % HOUR_SECONDS == 0 -> {
                val base = if (showBefore) R.plurals.hours_before else R.plurals.by_hours
                resources.getQuantityString(base, seconds / HOUR_SECONDS, seconds / HOUR_SECONDS)
            }

            seconds % MINUTE_SECONDS == 0 -> {
                val base = if (showBefore) R.plurals.minutes_before else R.plurals.by_minutes
                resources.getQuantityString(base, seconds / MINUTE_SECONDS, seconds / MINUTE_SECONDS)
            }

            else -> {
                val base = if (showBefore) R.plurals.seconds_before else R.plurals.by_seconds
                resources.getQuantityString(base, seconds, seconds)
            }
        }
    }
}

fun Context.getDefaultAlarmTitle(type: Int): String {
    val alarmString = getString(R.string.alarm)
    return try {
        RingtoneManager.getRingtone(this, RingtoneManager.getDefaultUri(type))?.getTitle(this) ?: alarmString
    } catch (_: Exception) {
        alarmString
    }
}

fun Context.getDefaultAlarmSound(type: Int) = AlarmSound(0, getDefaultAlarmTitle(type), RingtoneManager.getDefaultUri(type).toString())

fun Context.getTimeFormat() = if (baseConfig.use24HourFormat) TIME_FORMAT_24 else TIME_FORMAT_12

fun Context.getTimeFormatWithSeconds() = if (baseConfig.use24HourFormat) {
    TIME_FORMAT_24_WITH_SECS
} else {
    TIME_FORMAT_12_WITH_SECS
}

fun Context.getResolution(path: String): Point? {
    return if (path.isImageFast() || path.isImageSlow()) {
        getImageResolution(path)
    } else if (path.isVideoFast() || path.isVideoSlow()) {
        getVideoResolution(path)
    } else {
        null
    }
}

fun Context.getImageResolution(path: String): Point? {
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    if (isRestrictedSAFOnlyRoot(path)) {
        BitmapFactory.decodeStream(contentResolver.openInputStream(getAndroidSAFUri(path)), null, options)
    } else {
        BitmapFactory.decodeFile(path, options)
    }

    val width = options.outWidth
    val height = options.outHeight
    return if (width > 0 && height > 0) {
        Point(options.outWidth, options.outHeight)
    } else {
        null
    }
}

fun Context.getVideoResolution(path: String): Point? {
    var point = try {
        val retriever = MediaMetadataRetriever()
        if (isRestrictedSAFOnlyRoot(path)) {
            retriever.setDataSource(this, getAndroidSAFUri(path))
        } else {
            retriever.setDataSource(path)
        }

        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!.toInt()
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!.toInt()
        Point(width, height)
    } catch (_: Exception) {
        null
    }

    if (point == null && path.startsWith("content://", true)) {
        try {
            val fd = contentResolver.openFileDescriptor(path.toUri(), "r")?.fileDescriptor
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(fd)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!.toInt()
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!.toInt()
            point = Point(width, height)
        } catch (_: Exception) {
        }
    }

    return point
}

fun Context.getDuration(path: String): Int? {
    val projection = arrayOf(
        MediaColumns.DURATION
    )

    val uri = getFileUri(path)
    val selection = if (path.startsWith("content://")) "${BaseColumns._ID} = ?" else "${MediaColumns.DATA} = ?"
    val selectionArgs = if (path.startsWith("content://")) arrayOf(path.substringAfterLast("/")) else arrayOf(path)

    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return (cursor.getIntValue(MediaColumns.DURATION) / 1000.toDouble()).roundToInt()
            }
        }
    } catch (_: Exception) {
    }

    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toInt() / 1000f).roundToInt()
    } catch (_: Exception) {
        null
    }
}

fun Context.getTitle(path: String): String? {
    val projection = arrayOf(
        MediaColumns.TITLE
    )

    val uri = getFileUri(path)
    val selection = if (path.startsWith("content://")) "${BaseColumns._ID} = ?" else "${MediaColumns.DATA} = ?"
    val selectionArgs = if (path.startsWith("content://")) arrayOf(path.substringAfterLast("/")) else arrayOf(path)

    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return cursor.getStringValue(MediaColumns.TITLE)
            }
        }
    } catch (_: Exception) {
    }

    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
    } catch (_: Exception) {
        null
    }
}

fun Context.getArtist(path: String): String? {
    val projection = arrayOf(
        Audio.Media.ARTIST
    )

    val uri = getFileUri(path)
    val selection = if (path.startsWith("content://")) "${BaseColumns._ID} = ?" else "${MediaColumns.DATA} = ?"
    val selectionArgs = if (path.startsWith("content://")) arrayOf(path.substringAfterLast("/")) else arrayOf(path)

    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return cursor.getStringValue(Audio.Media.ARTIST)
            }
        }
    } catch (_: Exception) {
    }

    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
    } catch (_: Exception) {
        null
    }
}

fun Context.getAlbum(path: String): String? {
    val projection = arrayOf(
        Audio.Media.ALBUM
    )

    val uri = getFileUri(path)
    val selection = if (path.startsWith("content://")) "${BaseColumns._ID} = ?" else "${MediaColumns.DATA} = ?"
    val selectionArgs = if (path.startsWith("content://")) arrayOf(path.substringAfterLast("/")) else arrayOf(path)

    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return cursor.getStringValue(Audio.Media.ALBUM)
            }
        }
    } catch (_: Exception) {
    }

    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
    } catch (_: Exception) {
        null
    }
}

fun Context.getMediaStoreLastModified(path: String): Long {
    val projection = arrayOf(
        MediaColumns.DATE_MODIFIED
    )

    val uri = getFileUri(path)
    val selection = "${BaseColumns._ID} = ?"
    val selectionArgs = arrayOf(path.substringAfterLast("/"))

    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return cursor.getLongValue(MediaColumns.DATE_MODIFIED) * 1000
            }
        }
    } catch (_: Exception) {
    }
    return 0
}

fun Context.getStringsPackageName() = getString(R.string.package_name)

fun Context.getFontSizeText() = getString(
    when (baseConfig.fontSize) {
        FONT_SIZE_SMALL -> R.string.small
        FONT_SIZE_MEDIUM -> R.string.medium
        FONT_SIZE_LARGE -> R.string.large
        else -> R.string.extra_large
    }
)

fun Context.getTextSize() = when (baseConfig.fontSize) {
    FONT_SIZE_SMALL -> resources.getDimension(R.dimen.normal_text_size)
    FONT_SIZE_MEDIUM -> resources.getDimension(R.dimen.bigger_text_size)
    FONT_SIZE_LARGE -> resources.getDimension(R.dimen.big_text_size)
    else -> resources.getDimension(R.dimen.extra_big_text_size)
}

fun Context.getTextSizeSmall() = when (baseConfig.fontSize) {
    FONT_SIZE_SMALL -> resources.getDimension(R.dimen.small_text_size)
    FONT_SIZE_MEDIUM -> resources.getDimension(R.dimen.smaller_text_size)
    FONT_SIZE_LARGE -> resources.getDimension(R.dimen.bigger_text_size)
    else -> resources.getDimension(R.dimen.big_text_size)
}

val Context.telecomManager: TelecomManager get() = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
val Context.windowManager: WindowManager get() = getSystemService(Context.WINDOW_SERVICE) as WindowManager
val Context.notificationManager: NotificationManager get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


val Context.usableScreenSize: Point
    get() {
        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        return size
    }

val Context.realScreenSize: Point
    get() {
        val size = Point()
        windowManager.defaultDisplay.getRealSize(size)
        return size
    }
fun Context.isDefaultDialer(): Boolean {
    return if (isQPlus()) {
        val roleManager = getSystemService(RoleManager::class.java)
        roleManager != null &&
            roleManager.isRoleAvailable(RoleManager.ROLE_SMS) &&
            roleManager.isRoleHeld(RoleManager.ROLE_SMS)
    } else {
        android.provider.Telephony.Sms.getDefaultSmsPackage(this) == packageName
    }
}

fun Context.getContactsHasMap(withComparableNumbers: Boolean = false, callback: (HashMap<String, String>) -> Unit) {
    ContactsHelper(this).getContacts(showOnlyContactsWithNumbers = true) { contactList ->
        val privateContacts: HashMap<String, String> = HashMap()
        for (contact in contactList) {
            for (phoneNumber in contact.phoneNumbers) {
                var number = PhoneNumberUtils.stripSeparators(phoneNumber.value)
                if (withComparableNumbers) {
                    number = number.trimToComparableNumber()
                }

                privateContacts[number] = contact.name
            }
        }
        callback(privateContacts)
    }
}

fun Context.getBlockedNumbersWithContact(callback: (ArrayList<BlockedNumber>) -> Unit) {
    getContactsHasMap(true) { contacts ->
        val blockedNumbers = ArrayList<BlockedNumber>()
        if (!isDefaultDialer()) {
            callback(blockedNumbers)
            return@getContactsHasMap // Fix 1: without this return, code below runs even when
            // not authorized — causes SecurityException and double callback fire
        }

        val uri = BlockedNumbers.CONTENT_URI
        val projection = arrayOf(
            BlockedNumbers.COLUMN_ID,
            BlockedNumbers.COLUMN_ORIGINAL_NUMBER,
            BlockedNumbers.COLUMN_E164_NUMBER,
        )

        queryCursor(uri, projection) { cursor ->
            val id = cursor.getLongValue(BlockedNumbers.COLUMN_ID)
            val number = cursor.getStringValue(BlockedNumbers.COLUMN_ORIGINAL_NUMBER) ?: ""
            val normalizedNumber = cursor.getStringValue(BlockedNumbers.COLUMN_E164_NUMBER) ?: number
            val comparableNumber = normalizedNumber.trimToComparableNumber()

            val contactName = contacts[comparableNumber]
            val blockedNumber = BlockedNumber(id, number, normalizedNumber, comparableNumber, contactName)
            blockedNumbers.add(blockedNumber)
        }

        val blockedNumbersPair = blockedNumbers.partition { it.contactName != null }
        val blockedNumbersWithNameSorted = blockedNumbersPair.first.sortedBy { it.contactName }
        val blockedNumbersNoNameSorted = blockedNumbersPair.second.sortedBy { it.number }

        callback(ArrayList(blockedNumbersWithNameSorted + blockedNumbersNoNameSorted))
    }
}

fun Context.getBlockedNumbers(): ArrayList<BlockedNumber> {
    val blockedNumbers = ArrayList<BlockedNumber>()
    if (!isDefaultDialer()) {
        return blockedNumbers
    }

    val uri = BlockedNumbers.CONTENT_URI
    val projection = arrayOf(
        BlockedNumbers.COLUMN_ID,
        BlockedNumbers.COLUMN_ORIGINAL_NUMBER,
        BlockedNumbers.COLUMN_E164_NUMBER
    )

    queryCursor(uri, projection) { cursor ->
        val id = cursor.getLongValue(BlockedNumbers.COLUMN_ID)
        val number = cursor.getStringValue(BlockedNumbers.COLUMN_ORIGINAL_NUMBER) ?: ""
        val normalizedNumber = cursor.getStringValue(BlockedNumbers.COLUMN_E164_NUMBER) ?: number
        val comparableNumber = normalizedNumber.trimToComparableNumber()
        val blockedNumber = BlockedNumber(id, number, normalizedNumber, comparableNumber)
        blockedNumbers.add(blockedNumber)
    }

    return blockedNumbers
}

fun Context.addBlockedNumber(number: String): Boolean {
    // Fix 2a: without this guard, insert() throws SecurityException silently
    // when app is not the default SMS app
    if (!isDefaultDialer()) {
        Log.e("BLOCK", "addBlockedNumber: not default SMS app, skipping")
        return false
    }
    // Fix 2b: moved out of .apply{} — return inside apply doesn't return the outer function,
    // so the old code always returned true even when insert failed
    return try {
        val values = ContentValues().apply {
            put(BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number)
            // Only normalize real phone numbers — wildcard patterns like "+91*"
            // must NOT be passed to PhoneNumberUtils as it corrupts them
            if (number.isPhoneNumber()) {
                put(BlockedNumbers.COLUMN_E164_NUMBER, PhoneNumberUtils.normalizeNumber(number))
            }
        }
        contentResolver.insert(BlockedNumbers.CONTENT_URI, values)
        true
    } catch (e: Exception) {
        Log.e("BLOCK", "addBlockedNumber failed: \${e.message}")
        showErrorToast(e)
        false
    }
}

fun Context.deleteBlockedNumber(number: String): Boolean {
    // Fix 3: old code deleted by exact COLUMN_ORIGINAL_NUMBER string match.
    // But stored number may be E164/normalized (e.g. "+911234567890") while
    // caller passes "1234567890" — SQL finds 0 rows, returns false silently.
    // Solution: look up the real row ID first using same matching as isNumberBlocked,
    // then delete by ID which is always exact.
    val blockedNumbers = getBlockedNumbers()
    if (!isNumberBlocked(number, blockedNumbers)) {
        return true // already not blocked, nothing to do
    }

    val numberToCompare = number.trimToComparableNumber()
    val stripped = PhoneNumberUtils.stripSeparators(number)

    val matchedEntry = blockedNumbers.firstOrNull {
        numberToCompare == it.numberToCompare ||
            numberToCompare == it.number ||
            stripped == it.number
    } ?: return false

    val deleteUri = android.net.Uri.withAppendedPath(
        BlockedNumbers.CONTENT_URI, matchedEntry.id.toString()
    )
    val deletedRowCount = contentResolver.delete(deleteUri, null, null)
    return deletedRowCount > 0
}

fun Context.isNumberBlocked(number: String, blockedNumbers: ArrayList<BlockedNumber> = getBlockedNumbers()): Boolean {
    val numberToCompare = number.trimToComparableNumber()

    return blockedNumbers.any {
        numberToCompare == it.numberToCompare ||
            numberToCompare == it.number ||
            PhoneNumberUtils.stripSeparators(number) == it.number
    } || isNumberBlockedByPattern(number, blockedNumbers)
}

fun Context.isNumberBlockedByPattern(number: String, blockedNumbers: ArrayList<BlockedNumber> = getBlockedNumbers()): Boolean {
    for (blockedNumber in blockedNumbers) {
        val num = blockedNumber.number
        if (num.isBlockedNumberPattern()) {
            try {
                // First, we shield all special characters in regular expressions.
                val escapedNum = Regex.escape(num)
                // Then replace the escaped * with .* to support patterns.
                val pattern = escapedNum.replace("\\*", ".*")

                if (number.matches(Regex(pattern))) {
                    return true
                }
            } catch (e: PatternSyntaxException) {
                // We log the error and skip this template.
                android.util.Log.e("BlockedPattern", "Invalid pattern: $num", e)
                baseConfig.lastError = "Context.isNumberBlockedByPattern() PatternSyntaxException: $e"
            }
        }
    }
    return false
}

fun Context.copyToClipboard(text: String) {
    val clip = ClipData.newPlainText(getString(R.string.app_name), text)
    (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
    val toastText = String.format(getString(R.string.value_copied_to_clipboard_show), text)
    toast(toastText)
}

fun Context.getPhoneNumberTypeText(type: Int, label: String): String {
    return if (type == BaseTypes.TYPE_CUSTOM) {
        label
    } else {
        getString(
            when (type) {
                Phone.TYPE_MOBILE -> R.string.mobile
                Phone.TYPE_HOME -> R.string.home
                Phone.TYPE_WORK -> R.string.work
                Phone.TYPE_MAIN -> R.string.main_number
                Phone.TYPE_FAX_WORK -> R.string.work_fax
                Phone.TYPE_FAX_HOME -> R.string.home_fax
                Phone.TYPE_PAGER -> R.string.pager
                else -> R.string.other
            }
        )
    }
}


fun Context.sendEmailIntent(recipient: String) {
    Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.fromParts(KEY_MAILTO, recipient, null)
        launchActivityIntent(this)
    }
}

fun Context.openNotificationSettings() {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
    intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    startActivity(intent)
}

fun Context.getTempFile(folderName: String, filename: String): File? {
    val folder = File(cacheDir, folderName)
    if (!folder.exists()) {
        if (!folder.mkdir()) {
            toast(R.string.unknown_error_occurred)
            return null
        }
    }

    return File(folder, filename)
}

fun Context.openDeviceSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
    }

    try {
        startActivity(intent)
    } catch (e: Exception) {
        showErrorToast(e)
    }
}

@RequiresApi(Build.VERSION_CODES.S)
fun Context.openRequestExactAlarmSettings(appId: String) {
    if (isSPlus()) {
        val uri = Uri.fromParts("package", appId, null)
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        intent.data = uri
        startActivity(intent)
    }
}


fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun Context.applyFontToTextView(
    textView: TextView,
    typeface: Typeface? = null,
    force: Boolean = false
) {
    if (typeface == null && !isCredentialStorageAvailable) return
    val actualTypeface = typeface ?: FontHelper.getTypeface(this)
    if (actualTypeface == Typeface.DEFAULT && !force) return // avoid unnecessary calls and overwrites
    val existingStyle = textView.typeface?.style ?: Typeface.NORMAL
    textView.setTypeface(actualTypeface, existingStyle)
}

fun Context.applyFontToViewRecursively(
    view: View?,
    typeface: Typeface? = null,
    force: Boolean = false
) {
    if (view == null) return
    if (typeface == null && !isCredentialStorageAvailable) return
    val actualTypeface = typeface ?: FontHelper.getTypeface(this)
    if (view is TextView) applyFontToTextView(view, actualTypeface, force)
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            applyFontToViewRecursively(view.getChildAt(i), actualTypeface, force)
        }
    }
}


//Goodwy
fun Context.getEmailTypeText(type: Int, label: String): String {
    return if (type == BaseTypes.TYPE_CUSTOM) {
        label
    } else {
        getString(
            when (type) {
                Email.TYPE_HOME -> R.string.home
                Email.TYPE_WORK -> R.string.work
                Email.TYPE_MOBILE -> R.string.mobile
                else -> R.string.other
            }
        )
    }
}

fun Context.getRelationTypeText(type: Int, label: String): String {
    return if (type == BaseTypes.TYPE_CUSTOM) {
        label
    } else {
        getString(
            when (type) {
                // Relation.TYPE_CUSTOM   -> stringsR.string.custom
                Relation.TYPE_ASSISTANT -> stringsR.string.relation_assistant_g
                Relation.TYPE_BROTHER -> stringsR.string.relation_brother_g
                Relation.TYPE_CHILD -> stringsR.string.relation_child_g
                Relation.TYPE_DOMESTIC_PARTNER -> stringsR.string.relation_domestic_partner_g
                Relation.TYPE_FATHER -> stringsR.string.relation_father_g
                Relation.TYPE_FRIEND -> stringsR.string.relation_friend_g
                Relation.TYPE_MANAGER -> stringsR.string.relation_manager_g
                Relation.TYPE_MOTHER -> stringsR.string.relation_mother_g
                Relation.TYPE_PARENT -> stringsR.string.relation_parent_g
                Relation.TYPE_PARTNER -> stringsR.string.relation_partner_g
                Relation.TYPE_REFERRED_BY -> stringsR.string.relation_referred_by_g
                Relation.TYPE_RELATIVE -> stringsR.string.relation_relative_g
                Relation.TYPE_SISTER -> stringsR.string.relation_sister_g
                Relation.TYPE_SPOUSE -> stringsR.string.relation_spouse_g

                // Relation types defined in vCard 4.0
                ContactRelation.TYPE_CONTACT -> stringsR.string.relation_contact_g
                ContactRelation.TYPE_ACQUAINTANCE -> stringsR.string.relation_acquaintance_g
                // ContactRelation.TYPE_FRIEND -> stringsR.string.relation_friend
                ContactRelation.TYPE_MET -> stringsR.string.relation_met_g
                ContactRelation.TYPE_CO_WORKER -> stringsR.string.relation_co_worker_g
                ContactRelation.TYPE_COLLEAGUE -> stringsR.string.relation_colleague_g
                ContactRelation.TYPE_CO_RESIDENT -> stringsR.string.relation_co_resident_g
                ContactRelation.TYPE_NEIGHBOR -> stringsR.string.relation_neighbor_g
                // ContactRelation.TYPE_CHILD -> stringsR.string.relation_child
                // ContactRelation.TYPE_PARENT -> stringsR.string.relation_parent
                ContactRelation.TYPE_SIBLING -> stringsR.string.relation_sibling_g
                // ContactRelation.TYPE_SPOUSE -> stringsR.string.relation_spouse
                ContactRelation.TYPE_KIN -> stringsR.string.relation_kin_g
                ContactRelation.TYPE_MUSE -> stringsR.string.relation_muse_g
                ContactRelation.TYPE_CRUSH -> stringsR.string.relation_crush_g
                ContactRelation.TYPE_DATE -> stringsR.string.relation_date_g
                ContactRelation.TYPE_SWEETHEART -> stringsR.string.relation_sweetheart_g
                ContactRelation.TYPE_ME -> stringsR.string.relation_me_g
                ContactRelation.TYPE_AGENT -> stringsR.string.relation_agent_g
                ContactRelation.TYPE_EMERGENCY -> stringsR.string.relation_emergency_g

                ContactRelation.TYPE_SUPERIOR -> stringsR.string.relation_superior_g
                ContactRelation.TYPE_SUBORDINATE -> stringsR.string.relation_subordinate_g
                ContactRelation.TYPE_HUSBAND -> stringsR.string.relation_husband_g
                ContactRelation.TYPE_WIFE -> stringsR.string.relation_wife_g
                ContactRelation.TYPE_SON -> stringsR.string.relation_son_g
                ContactRelation.TYPE_DAUGHTER -> stringsR.string.relation_daughter_g
                ContactRelation.TYPE_GRANDPARENT -> stringsR.string.relation_grandparent_g
                ContactRelation.TYPE_GRANDFATHER -> stringsR.string.relation_grandfather_g
                ContactRelation.TYPE_GRANDMOTHER -> stringsR.string.relation_grandmother_g
                ContactRelation.TYPE_GRANDCHILD -> stringsR.string.relation_grandchild_g
                ContactRelation.TYPE_GRANDSON -> stringsR.string.relation_grandson_g
                ContactRelation.TYPE_GRANDDAUGHTER -> stringsR.string.relation_granddaughter_g
                ContactRelation.TYPE_UNCLE -> stringsR.string.relation_uncle_g
                ContactRelation.TYPE_AUNT -> stringsR.string.relation_aunt_g
                ContactRelation.TYPE_NEPHEW -> stringsR.string.relation_nephew_g
                ContactRelation.TYPE_NIECE -> stringsR.string.relation_niece_g
                ContactRelation.TYPE_FATHER_IN_LAW -> stringsR.string.relation_father_in_law_g
                ContactRelation.TYPE_MOTHER_IN_LAW -> stringsR.string.relation_mother_in_law_g
                ContactRelation.TYPE_SON_IN_LAW -> stringsR.string.relation_son_in_law_g
                ContactRelation.TYPE_DAUGHTER_IN_LAW -> stringsR.string.relation_daughter_in_law_g
                ContactRelation.TYPE_BROTHER_IN_LAW -> stringsR.string.relation_brother_in_law_g
                ContactRelation.TYPE_SISTER_IN_LAW -> stringsR.string.relation_sister_in_law_g

                else -> R.string.other
            }
        )
    }
}

fun Context.getEventTypeText(type: Int, label: String): String {
    return if (type == BaseTypes.TYPE_CUSTOM) {
        label
    } else {
        getString(
            when (type) {
                Event.TYPE_ANNIVERSARY -> R.string.anniversary
                Event.TYPE_BIRTHDAY -> R.string.birthday
                CUSTOM_EVENT_TYPE_DEATH -> stringsR.string.death
                else -> R.string.other
            }
        )
    }
}


fun Context.getLetterBackgroundColors(): ArrayList<Long> {
    return when (baseConfig.contactColorList) {
        LBC_ORIGINAL -> letterBackgroundColors
        LBC_IOS -> letterBackgroundColorsIOS
        LBC_ARC -> letterBackgroundColorsArc
        else -> letterBackgroundColorsAndroid
    }
}

suspend fun isMiUi(): Boolean {
    val manufacturer = Build.MANUFACTURER.lowercase(Locale.getDefault())
    val isXiaomi = manufacturer.contains(Regex(pattern = "xiaomi|redmi|poco"))

    if (!isXiaomi) {
        return false
    }

    return !TextUtils.isEmpty(getSystemProperty("ro.miui.ui.version.name"))
}

suspend fun isEmui(): Boolean {
    // Quick check by manufacturer
    val manufacturer = Build.MANUFACTURER.lowercase(Locale.getDefault())
    val isHuawei = manufacturer.contains(Regex(pattern = "huawei|honor"))

    if (!isHuawei) {
        return false
    }

    // Accurate verification via system property
    return !TextUtils.isEmpty(getSystemProperty("ro.build.version.emui"))
}

suspend fun getSystemProperty(propName: String): String? = withContext(Dispatchers.IO) {
    val line: String
    var input: BufferedReader? = null
    try {
        val process = Runtime.getRuntime().exec("getprop $propName")
        // Add a timeout to avoid freezes
        if (!process.waitFor(3, TimeUnit.SECONDS)) {
            process.destroy()
            return@withContext null
        }
        input = BufferedReader(InputStreamReader(process.inputStream), 1024)
        line = input.readLine()
        input.close()
    } catch (_: IOException) {
        return@withContext null
    } finally {
        if (input != null) {
            try {
                input.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
    return@withContext line
}

fun Context.isPro() = baseConfig.isPro

fun Context.appPrefix(): String = if (isNewApp()) "dev." else "com."

fun Context.sysLocale(): Locale? {
    val config = this.resources.configuration
    return getSystemLocale(config)
}

private fun getSystemLocale(config: Configuration) = config.locales.get(0)

fun Context.isNewApp(): Boolean = packageName.startsWith("com.smsnew.messenger.", true)
