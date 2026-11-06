package id.aruna.domino

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import android.content.pm.ServiceInfo

/**
 * Captures the device display only after the user approves Android's
 * MediaProjection consent dialog. Frames are currently discarded after
 * acquisition; no screenshots are saved or uploaded.
 */
class ScreenObserverService : Service() {
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private lateinit var workerThread: HandlerThread
    private lateinit var workerHandler: Handler
    private var frameCount = 0L

    override fun onCreate() {
        super.onCreate()
        workerThread = HandlerThread("ArunaScreenObserver").also { it.start() }
        workerHandler = Handler(workerThread.looper)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopObserver()
                stopSelf()
            }
            ACTION_START -> startObserver(intent)
        }
        return START_NOT_STICKY
    }

    private fun startObserver(intent: Intent) {
        if (projection != null) return

        val notification = buildNotification("Menunggu layar permainan…")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        val permissionData = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(EXTRA_PERMISSION_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_PERMISSION_DATA)
        }

        if (permissionData == null) {
            stopObserver()
            stopSelf()
            return
        }

        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = runCatching { manager.getMediaProjection(resultCode, permissionData) }.getOrNull()
        val activeProjection = projection
        if (activeProjection == null) {
            stopObserver()
            stopSelf()
            return
        }

        activeProjection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                releaseCaptureResources(stopProjection = false)
                sendStatus(false)
                stopSelf()
            }
        }, workerHandler)

        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)

        imageReader = ImageReader.newInstance(
            metrics.widthPixels,
            metrics.heightPixels,
            PixelFormat.RGBA_8888,
            2
        ).also { reader ->
            reader.setOnImageAvailableListener({ source ->
                val frame = source.acquireLatestImage()
                if (frame != null) {
                    frame.close()
                    frameCount += 1
                    if (frameCount % 30L == 0L) {
                        updateNotification("Pengamat aktif • $frameCount frame diterima")
                        sendStatus(true)
                    }
                }
            }, workerHandler)
        }

        virtualDisplay = activeProjection.createVirtualDisplay(
            "ARUNA_DOMINO_OBSERVER",
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            workerHandler
        )

        if (virtualDisplay == null) {
            stopObserver()
            stopSelf()
            return
        }

        updateNotification("Pengamat aktif • layar sedang ditangkap")
        sendStatus(true)
    }

    private fun stopObserver() {
        releaseCaptureResources()
        sendStatus(false)
        if (Build.VERSION.SDK_INT >= 24) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun releaseCaptureResources(stopProjection: Boolean = true) {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.setOnImageAvailableListener(null, null)
        imageReader?.close()
        imageReader = null
        val activeProjection = projection
        projection = null
        if (stopProjection) activeProjection?.stop()
    }

    private fun sendStatus(active: Boolean) {
        sendBroadcast(Intent(ACTION_STATUS).setPackage(packageName).apply {
            putExtra(EXTRA_ACTIVE, active)
            putExtra(EXTRA_FRAMES, frameCount)
        })
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pengamat ARUNA DOMINO",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(message: String): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("ARUNA DOMINO")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentIntent(openApp)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("ARUNA DOMINO")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentIntent(openApp)
                .setOngoing(true)
                .build()
        }
    }

    private fun updateNotification(message: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(message))
    }

    override fun onDestroy() {
        releaseCaptureResources()
        workerThread.quitSafely()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "id.aruna.domino.action.OBSERVER_START"
        const val ACTION_STOP = "id.aruna.domino.action.OBSERVER_STOP"
        const val ACTION_STATUS = "id.aruna.domino.action.OBSERVER_STATUS"
        const val EXTRA_RESULT_CODE = "permission_result_code"
        const val EXTRA_PERMISSION_DATA = "permission_data"
        const val EXTRA_ACTIVE = "observer_active"
        const val EXTRA_FRAMES = "observer_frames"
        private const val CHANNEL_ID = "aruna_domino_observer"
        private const val NOTIFICATION_ID = 7301
    }
}
