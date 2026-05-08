package com.smsnew.messenger.workers

import android.content.Context
import android.provider.Telephony
import androidx.work.*
import com.smsnew.messenger.extensions.config
import com.smsnew.messenger.extensions.deleteMessage
import com.smsnew.messenger.extensions.moveMessageToRecycleBin
import com.smsnew.messenger.helpers.OtpDetector
import java.util.concurrent.TimeUnit

class OtpAutoDeleteWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            if (!applicationContext.config.deleteOtpAfter24Hours) {
                return Result.success()
            }
            val cutoffMillis = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)
            deleteOldOtps(cutoffMillis)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun deleteOldOtps(cutoffMillis: Long) {
        val ctx = applicationContext
        val resolver = ctx.contentResolver
        val useRecycleBin = ctx.config.useRecycleBin

        // Note: SMS table mein date milliseconds mein hota hai
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )
        val selection = "${Telephony.Sms.DATE} < ?"
        val args = arrayOf(cutoffMillis.toString())

        resolver.query(Telephony.Sms.CONTENT_URI, projection, selection, args, null)?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(Telephony.Sms._ID)
            val bodyIdx = c.getColumnIndexOrThrow(Telephony.Sms.BODY)

            while (c.moveToNext()) {
                val body = c.getString(bodyIdx)
                if (!OtpDetector.isOtpMessage(body)) continue
                val id = c.getLong(idIdx)
                try {
                    if (useRecycleBin) {

                        ctx.moveMessageToRecycleBin(id)
                    } else {
                        ctx.deleteMessage(id, isMMS = false)
                    }
                } catch (_: Exception) { }
            }
        }
    }

    companion object {
        private const val WORK_NAME = "otp_auto_delete_periodic"
        private const val ONE_TIME_NAME = "otp_auto_delete_oneshot"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<OtpAutoDeleteWorker>(
                6, TimeUnit.HOURS
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun runOnce(context: Context) {
            val request = OneTimeWorkRequestBuilder<OtpAutoDeleteWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
