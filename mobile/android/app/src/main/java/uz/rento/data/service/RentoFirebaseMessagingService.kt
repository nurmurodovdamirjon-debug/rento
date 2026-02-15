package uz.rento.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import uz.rento.R
import uz.rento.domain.usecase.notification.RegisterFcmTokenUseCase
import uz.rento.presentation.MainActivity
import javax.inject.Inject

/**
 * RentoFirebaseMessagingService — FCM bildirishnomalarni qabul qilish
 *
 * Firebase Cloud Messaging xabarlarni qabul qiladi va
 * lokal notification ko'rsatadi.
 */
@AndroidEntryPoint
class RentoFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        const val CHANNEL_ID = "rento_notifications"
        const val CHANNEL_NAME = "Rento bildirishnomalar"
    }

    @Inject
    lateinit var registerFcmTokenUseCase: RegisterFcmTokenUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Yangi FCM token: $token")

        // Tokenni serverga yuborish
        serviceScope.launch {
            registerFcmTokenUseCase.register(token)
                .onSuccess { Log.d(TAG, "Token serverga yuborildi") }
                .onFailure { Log.e(TAG, "Token yuborishda xatolik", it) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Xabar qabul qilindi: ${message.data}")

        // Notification payload
        val title = message.notification?.title ?: message.data["title"] ?: "Rento"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val type = message.data["type"] ?: "system"
        val refId = message.data["ref_id"]

        showNotification(title, body, type, refId)
    }

    private fun showNotification(
        title: String,
        body: String,
        type: String,
        refId: String?
    ) {
        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", type)
            putExtra("ref_id", refId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Rento ilovasidan bildirishnomalar"
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
