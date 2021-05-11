package com.vesvault.vesmail

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.os.Build

class Error constructor(err: String?, _prof: String?) {
    val profile: String = _prof ?: ""
    var channel: Int = R.string.channelR_id
    var srv: String = ""
    var code: String = ""
    var msg1: Int = R.string.XVES_REMOTE
    var msg2: Int = R.string.XVES_UNKNOWN
    var color = R.color.vesmail_error

    init {
        if (err != null) {
            val pos: Int = err.indexOf('.')
            if (pos > 0) {
                srv = err.substring(0, pos).toUpperCase()
                code = err.substring(pos + 1)
            }
        }
        val p = code.indexOf('.')
        val c = if (p > 0) code.substring(0, p) else code
        when (c) {
            "XVES-3" -> {
                channel = R.string.channelL_id
                msg1 = R.string.XVES_LOCAL
                if (p > 0) when(code.substring(p + 1)) {
                    "4" -> {
                        msg2 = R.string.XVES_3_4
                    }
                    "7" -> {
                        msg2 = R.string.XVES_3_7
                    }
                }
            }
            "XVES-7" -> {
                msg2 = R.string.XVES_7
            }
            "XVES-16" -> {
                msg2 = R.string.XVES_16
            }
            "XVES-17" -> {
                msg2 = R.string.XVES_17
            }
            "XVES-18" -> {
                msg2 = R.string.XVES_18
            }
            "XVES-19" -> {
                msg2 = R.string.XVES_19
            }
            "XVES-20" -> {
                msg2 = R.string.XVES_20
            }
            "XVES-22" -> {
                msg2 = R.string.XVES_22
            }
            "XVES-31" -> {
                msg2 = R.string.XVES_31
            }
        }
    }

    public fun message1(ctx: Context): String {
        return ctx.getString(msg1)
    }

    public fun message2(ctx: Context): String {
        return ctx.getString(msg2).replaceFirst("%s", srv).replaceFirst("%s", profile)
    }

    public fun builder(ctx: Context, intent: PendingIntent?): Notification.Builder {
        val nb = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(ctx, ctx.getString(channel))
        else
            Notification.Builder(ctx)
        nb
                .setContentTitle(message1(ctx))
                .setContentText(message2(ctx))
                .setSmallIcon(R.drawable.ic_stat_name)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            nb.setColor(ctx.resources.getColor(color, ctx.theme))
        }
        if (intent != null) nb.setContentIntent(intent)
        return nb;
    }
}