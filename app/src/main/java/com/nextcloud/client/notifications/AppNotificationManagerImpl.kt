/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020-2021 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.nextcloud.client.account.User
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.ui.preview.PreviewImageActivity
import com.owncloud.android.ui.preview.PreviewImageFragment
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class AppNotificationManagerImpl @Inject constructor(
    private val context: Context,
    private val resources: Resources,
    private val platformNotificationsManager: NotificationManager,
    private val viewThemeUtils: ViewThemeUtils
) : AppNotificationManager {

    companion object {
        const val PROGRESS_PERCENTAGE_MAX = 100
        const val PROGRESS_PERCENTAGE_MIN = 0
    }

    private fun builder(channelId: String): NotificationCompat.Builder {
        val builder =
            NotificationCompat.Builder(context, channelId)
        viewThemeUtils.androidx.themeNotificationCompatBuilder(context, builder)
        return builder
    }

    override fun buildDownloadServiceForegroundNotification(): Notification {
        val icon = BitmapFactory.decodeResource(resources, R.drawable.notification_icon)
        return builder(NotificationUtils.NOTIFICATION_CHANNEL_DOWNLOAD)
            .setContentTitle(resources.getString(R.string.app_name))
            .setContentText(resources.getString(R.string.worker_download))
            .setSmallIcon(R.drawable.notification_icon)
            .setLargeIcon(icon)
            .build()
    }

    override fun postDownloadTransferProgress(fileOwner: User, file: OCFile, progress: Int, allowPreview: Boolean) {
        val builder = builder(NotificationUtils.NOTIFICATION_CHANNEL_DOWNLOAD)
        val content = resources.getString(
            R.string.downloader_download_in_progress_content,
            progress,
            file.fileName
        )
        builder
            .setSmallIcon(R.drawable.ic_cloud_download)
            .setTicker(resources.getString(R.string.downloader_download_in_progress_ticker))
            .setContentTitle(resources.getString(R.string.downloader_download_in_progress_ticker))
            .setOngoing(true)
            .setProgress(PROGRESS_PERCENTAGE_MAX, progress, progress <= PROGRESS_PERCENTAGE_MIN)
            .setContentText(content)

        if (allowPreview) {
            val openFileIntent = if (PreviewImageFragment.canBePreviewed(file)) {
                PreviewImageActivity.previewFileIntent(context, fileOwner, file)
            } else {
                FileDisplayActivity.openFileIntent(context, fileOwner, file)
            }
            openFileIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            val pendingOpenFileIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                openFileIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
            builder.setContentIntent(pendingOpenFileIntent)
        }
        platformNotificationsManager.notify(AppNotificationManager.TRANSFER_NOTIFICATION_ID, builder.build())
    }

    override fun postUploadTransferProgress(fileOwner: User, file: OCFile, progress: Int) {
        val builder = builder(NotificationUtils.NOTIFICATION_CHANNEL_DOWNLOAD)
        val content = resources.getString(
            R.string.uploader_upload_in_progress_content,
            progress,
            file.fileName
        )
        builder
            .setSmallIcon(R.drawable.ic_cloud_upload)
            .setTicker(resources.getString(R.string.uploader_upload_in_progress_ticker))
            .setContentTitle(resources.getString(R.string.uploader_upload_in_progress_ticker))
            .setOngoing(true)
            .setProgress(PROGRESS_PERCENTAGE_MAX, progress, progress <= PROGRESS_PERCENTAGE_MIN)
            .setContentText(content)

        platformNotificationsManager.notify(AppNotificationManager.TRANSFER_NOTIFICATION_ID, builder.build())
    }

    override fun cancelTransferNotification() {
        platformNotificationsManager.cancel(AppNotificationManager.TRANSFER_NOTIFICATION_ID)
    }
}
