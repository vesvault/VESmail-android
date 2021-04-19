package com.vesvault.vesmail

import android.content.Intent
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.Shape
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import androidx.core.view.isNotEmpty
import java.net.URLEncoder
import java.util.*
import kotlin.concurrent.schedule

class MainActivity : AppCompatActivity() {
    var view: View? = null
    val bullets: Array<Drawable?> = arrayOf(null, null, null, null)
    val bulletRcs: Array<Int> = arrayOf(
        R.drawable.stat_bullet,
        R.drawable.stat_bullet_c,
        R.drawable.stat_bullet_a,
        R.drawable.stat_bullet_e
    )
    val daemonidx: IntArray = IntArray(64)
    var stat: IntArray = IntArray(0)
    var ustat: IntArray = IntArray(0)
    var fproxy: Boolean = false
    var fwatch: Boolean = false
    var fwatching: Boolean = false
    private val timer: Timer = Timer()
    val B_NONE = 0
    val B_CONN = 1
    val B_ACTN = 2
    val B_ERR = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Intent(this, Proxy::class.java).also {
            startService(it)
        }
        view = layoutInflater.inflate(R.layout.activity_main, null)
        setContentView(view)
        view!!.findViewById<Button>(R.id.profile_button).setOnClickListener {
            openprofile(null)
        }
    }

    override fun onResume() {
        super.onResume()
        fwatch = true
        if (!fwatching) watch()
    }

    override fun onPause() {
        super.onPause()
        fwatch = false
    }

    private fun openprofile(p: String?) {
        var url: String = "https://vesmail.email/profile/?local=1"
        if (p != null) url += "&p=" + URLEncoder.encode(p, "utf-8")
        startActivity(Intent()
                .setAction(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setData(Uri.parse(url)))
    }

    private fun setdaemon(idx: Int, srv: String, host: String, port: String) {
        android.util.Log.d("daemon",idx.toString() + ':' + srv + ':' + host + ':' + port)
        if (srv == "now") return
        val tbl: TableLayout = view!!.findViewById(R.id.daemons)
        val tr: TableRow = layoutInflater.inflate(R.layout.proxy_row, null) as TableRow
        tbl.addView(tr)
        tr.findViewById<TextView>(R.id.srv).text = srv
        tr.findViewById<TextView>(R.id.host).text = host
        tr.findViewById<TextView>(R.id.port).text = port
        daemonidx[tbl.childCount - 1] = idx
    }

    private fun setuser(idx: Int, login: String) {
        android.util.Log.d("user", idx.toString() + ':' + login)
        val tbl: TableLayout = view!!.findViewById(R.id.users)
        var tr: TableRow? = tbl.getChildAt(idx) as TableRow?
        if (tr == null) {
            tr = layoutInflater.inflate(R.layout.user_row, null) as TableRow
            tbl.addView(tr)
        }
        tr.findViewById<TextView>(R.id.user).text = login
        tr.findViewById<ImageView>(R.id.profile).setOnClickListener {
            openprofile(login)
        }
    }

    override fun onTrimMemory(level: Int) {
        var i = 0
        while (i < bullets.size) bullets[i++] = null
    }

    private fun proxy(): Boolean {
        if (fproxy) return true
        if (Proxy.Instance == null) return false
        Proxy.Instance!!.watched = true
        Proxy.Instance!!.getdaemons(this)
        view!!.findViewById<TextView>(R.id.daemon_status).text = getText(R.string.daemons_running)
        fproxy = true
        return true
    }

    private fun setbullet(img: ImageView, st: Int, flg: Boolean): Boolean {
        val b: Int
        var rs: Boolean = false
        if (st >= 0 && (st and 0x0020) == 0) {
            if ((st and 0x00cc) != 0) {
                if (flg) b = B_ACTN
                else b = B_CONN
                rs = flg
            } else if ((st and 0x0010) != 0) {
                b = B_ACTN
            } else if ((st and 0x0001) != 0) {
                b = B_CONN
            } else if ((st and 0x0002) != 0) {
                b = B_ERR
            } else {
                b = B_NONE
            }
        } else b = B_ERR
        if (bullets[b] == null) {
            bullets[b] = ContextCompat.getDrawable(applicationContext, bulletRcs[b])
        }
        img.background = bullets[b]
        return rs
    }

    private fun showstat(flg: Boolean): Boolean {
        val tbl: TableLayout = view!!.findViewById(R.id.daemons)
        var idx = 0
        var rs = false
        while (true) {
            val ch = tbl.getChildAt(idx) ?: break
            val st = stat[daemonidx[idx++]]
            if (setbullet(ch.findViewById<ImageView>(R.id.stat), st, flg)) rs = true
        }
        val utbl: TableLayout = view!!.findViewById(R.id.users)
        idx = 0
        while (true) {
            val ch = utbl.getChildAt(idx) ?: break
            val st = ustat[idx++]
            if (setbullet(ch.findViewById<ImageView>(R.id.stat), st, flg)) rs = true
        }
        return rs
    }

    private fun watch() {
        if (proxy()) {
            stat = Proxy.Instance!!.watch()
            ustat = Proxy.Instance!!.getusers(this, view!!.findViewById<TableLayout>(R.id.users).childCount)
            val blink = showstat(true)
            if (blink) timer.schedule(125) {
                runOnUiThread {
                    showstat(false)
                }
            }
            val uhdr: Int =
                if (ustat.isNotEmpty()) R.string.users_list
                else R.string.users_empty
            view!!.findViewById<TextView>(R.id.users_status).text = getText(uhdr)
        }
        fwatching = fwatch
        if (fwatching) timer.schedule(250) {
            runOnUiThread {
                watch()
            }
        }
    }

}