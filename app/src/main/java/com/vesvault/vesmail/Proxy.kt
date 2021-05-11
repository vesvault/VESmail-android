package com.vesvault.vesmail;

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.*
import android.util.Log
import java.net.URLEncoder
import java.util.*
import kotlin.concurrent.schedule


public class Proxy : Service() {
        var uerror: Array<Notification.Builder?> = arrayOf()
        private val standbyfn: Runnable = Runnable {
                if (wakect == 1) sleep(this)
                val active = standby()
                if (wakect > 0) wakect--
                if (wakect > 0) setstandby(false, active)
        }
        private var pm: PowerManager? = null
        private var handler: Handler? = null

        private external fun addcert(crt: ByteArray): Int
        private external fun start(): Int
        public external fun watch(): IntArray
        public external fun signal(sig: Int)
        public external fun getdaemons(obj: Any): Int
        public external fun getusers(obj: Any?, last: Int): IntArray
        public external fun getuser(idx: Int): String?
        public external fun getuserprofileurl(idx: Int): String?
        public external fun getusererror(idx: Int): String?
        public external fun sleep(obj: Any?)
        public var watched: Boolean = false
        private val wakeidle = 8
        private var wakect: Int = 0

        override fun onBind(intent: Intent?): IBinder? {
                return null
        }

        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
                Log.d("proxy", "onStartCommand " + intent.toString() + " " + Instance.toString())
                super.onStartCommand(intent, flags, startId)
                val nb: Notification.Builder
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val notificationManager =
                                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                        val mChannel = NotificationChannel(
                                getString(R.string.channel_id),
                                getString(R.string.channel_name),
                                NotificationManager.IMPORTANCE_LOW
                        )
                        mChannel.description = getString(R.string.channel_description)
                        notificationManager.createNotificationChannel(mChannel)
                        val mChannelL = NotificationChannel(
                                getString(R.string.channelL_id),
                                getString(R.string.channelL_name),
                                NotificationManager.IMPORTANCE_HIGH
                        )
                        mChannelL.description = getString(R.string.channelL_description)
                        notificationManager.createNotificationChannel(mChannelL)
                        val mChannelR = NotificationChannel(
                                getString(R.string.channelR_id),
                                getString(R.string.channelR_name),
                                NotificationManager.IMPORTANCE_DEFAULT
                        )
                        mChannelR.description = getString(R.string.channelR_description)
                        notificationManager.createNotificationChannel(mChannelR)
                        nb = Notification.Builder(this, getString(R.string.channel_id))
                } else {
                        nb = Notification.Builder(this)
                }
                nb
                        .setContentTitle(getText(R.string.notification_title))
                        .setSmallIcon(R.drawable.ic_stat_name)
                        .setContentIntent(pendingintent(Intent(this, MainActivity::class.java)))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        nb.setColor(resources.getColor(R.color.vesmail, theme))
                }
                startForeground(7180, nb.build())
                System.loadLibrary("vesmail")
                val ks = java.security.KeyStore.getInstance("AndroidCAStore");
                ks.load(null, null);
                val aliases = ks.aliases()
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
                setstandby(true, true)
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

        private fun pendingintent(intent: Intent): PendingIntent {
                return intent.let { notificationIntent ->
                        PendingIntent.getActivity(applicationContext, 0, notificationIntent, 0)
                }
        }

        public fun profileintent(p: String?, idx: Int): Intent {
                var url: String? = null
                if (idx >= 0) url = Proxy.Instance?.getuserprofileurl(idx)
                if (url == null || !url.startsWith("https://")) url = "https://my.vesmail.email/profile"
                url += if (url.contains('?')) "&"
                else "?"
                url += "local=1&p="
                if (p != null) url += URLEncoder.encode(p, "utf-8")
                val e: String? = Proxy.Instance?.getusererror(idx)
                if (e != null) url += "#" + e
                return Intent()
                        .setAction(Intent.ACTION_VIEW)
                        .addCategory(Intent.CATEGORY_BROWSABLE)
                        .setData(Uri.parse(url))
        }

        public fun usererrors(ustat: IntArray) {
                var i: Int = 0;
                while (i < ustat.size) {
                        if ((ustat[i] and 0x8000) != 0) {
                                if (uerror.size <= i) uerror = uerror.copyOf(i + 1)
                                if (uerror[i] == null) {
                                        val prof = getuser(i);
                                        val er = Error(getusererror(i), prof)
                                        uerror[i] = er.builder(applicationContext, pendingintent(profileintent(prof, i)))
                                        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                        nm.notify(i, uerror[i]!!.build())
                                }
                        } else if (i < uerror.size && uerror[i] != null) {
                                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                nm.cancel(i)
                                uerror[i] = null
                        }
                        i++
                }
        }

        public fun standby(): Boolean {
//                Log.d("standby", Timer().toString())
                var active = watched
                if (!watched) {
                        val stat = watch()
                        var i = 0;
                        while (i < stat.size) if ((stat[i++] and 0x00cc) != 0) {
                                active = true;
                                break;
                        }
                        usererrors(getusers(null, -1))
                }
                watched = false
                return active
        }

        private fun setstandby(clr: Boolean, rst: Boolean) {
                if (rst) wakect = wakeidle
                if (handler == null) handler = Handler(mainLooper)
                val delay: Long = 1500
                if (clr) handler!!.removeCallbacks(standbyfn)
                handler!!.postDelayed(standbyfn, delay)
        }

        public fun wakeup() {
//                Log.d("proxy", "wakeup")
                if (!watched) {
                        mainLooper.run {
                                standby()
                                setstandby(true, true)
                        }
                }
        }

        public class Boot : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent) {
                        android.util.Log.d("proxy", "BroadcastReceiver " + intent.toString() + " " + Proxy.Instance.toString())
                        if  (Instance != null) {
                                Instance?.setstandby(true, false)
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

        companion object {
                public var Instance: Proxy? = null
        }

}
