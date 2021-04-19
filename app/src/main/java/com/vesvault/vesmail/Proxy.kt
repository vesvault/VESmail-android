package com.vesvault.vesmail;

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Debug
import android.os.IBinder
import android.util.Log


public class Proxy : Service() {
        private external fun addcert(crt: ByteArray): Int
        private external fun start(): Int
        public external fun watch(): IntArray
        public external fun signal(sig: Int)
        public external fun getdaemons(obj: Any): Int
        public external fun getusers(obj: Any, last: Int): IntArray
        public var watched: Boolean = false

        override fun onCreate() {

                Log.d("proxy", "onCreate")
                super.onCreate()
        }

        override fun onBind(intent: Intent?): IBinder? {
                return ProxyBinder()
        }

        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
                Log.d("proxy", "onStartCommand " + intent.toString() + " " + Instance.toString())
                super.onStartCommand(intent, flags, startId)
                val pendingIntent: PendingIntent =
                        Intent(this, MainActivity::class.java).let { notificationIntent ->
                                PendingIntent.getActivity(applicationContext, 0, notificationIntent, 0)
                        }
                val nb: Notification.Builder
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val name = getString(R.string.channel_name)
                        val descriptionText = getString(R.string.channel_description)
                        val importance = NotificationManager.IMPORTANCE_LOW
                        val mChannel = NotificationChannel(
                                getString(R.string.channel_id),
                                name,
                                importance
                        )
                        mChannel.description = descriptionText
                        val notificationManager =
                                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.createNotificationChannel(mChannel)
                        nb = Notification.Builder(this, getString(R.string.channel_id))
                } else {
                        nb = Notification.Builder(this)
                }
                nb
                        .setContentTitle(getText(R.string.notification_title))
                        .setSmallIcon(R.drawable.ic_stat_name)
                        .setContentIntent(pendingIntent)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        nb.setColor(resources.getColor(R.color.vesmail, theme))
                }
                startForeground(7180, nb.build())
                System.loadLibrary("vesmail")
                val ks = java.security.KeyStore.getInstance("AndroidCAStore");
                ks.load(null, null);
                val aliases = ks.aliases();
                while (aliases.hasMoreElements()) {
                        val alias = aliases.nextElement();
                        val cert = ks.getCertificate(alias);
                        addcert(cert.encoded)
                }
                start()
                Instance = this

                val alm = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val alarmIntent = Intent(this, Boot::class.java)
                val pi = PendingIntent.getBroadcast(applicationContext, 0, alarmIntent, 0)
                alm.setInexactRepeating(
                        AlarmManager.RTC,
                        System.currentTimeMillis(),
                        400000,
                        pi
                )

                return START_STICKY
        }

        override fun onDestroy() {
                Log.d("proxy", "onDestroy")
                super.onDestroy()
                sendBroadcast(Intent(this, BroadcastReceiver::class.java))
                signal(15)
                watch()
                Instance = null
        }

        public fun standby() {
                if (!watched) watch()
                watched = false
        }

        public class Boot : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent) {
                        android.util.Log.d("proxy", "BroadcastReceiver " + intent.action + " " + Proxy.Instance.toString())
                        if (Proxy.Instance != null) {
                                Proxy.Instance?.standby()
                                return
                        }
                        Intent(context, Proxy::class.java).also {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        context?.startForegroundService(it)
                                } else {
                                        context?.startService(it)
                                }
                        }
                }
        }

        inner class ProxyBinder : Binder() {
        }

        companion object {
                public var Instance: Proxy? = null
        }

}
