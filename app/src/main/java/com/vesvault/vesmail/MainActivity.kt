package com.vesvault.vesmail

import android.content.Intent
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.Shape
import android.net.Uri
import android.os.Build
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
    var ustat: IntArray = IntArray(0)
    var uerror: IntArray = IntArray(0)
    var fproxy: Boolean = false
    var fwatch: Boolean = false
    var fwatching: Boolean = false
    private val timer: Timer = Timer()
    var spinner: Spinner? = null
    var daemons: Daemon? = null
    var derror: Daemon? = null
    var handler: Handler? = null
    val B_NONE = 0
    val B_CONN = 1
    val B_ACTN = 2
    val B_ERR = 3
    val watchfn = Runnable {
        watch()
    }
    val blinkfn = Runnable {
        showstat(false)
    }

    class Daemon constructor(_idx: Int, _srv: String, _host: String, _port: String) {
        val idx: Int = _idx
        val srv: String = _srv
        val port: String = _port
        public var chain: Daemon? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Intent(this, Proxy::class.java).also {
            startService(it)
        }
        view = layoutInflater.inflate(R.layout.activity_main, null)
        setContentView(view)
        view!!.findViewById<Button>(R.id.profile_button).setOnClickListener {
            val p: String? = if (ustat.isNotEmpty()) "" else null
            openprofile(p, -1)
        }
        spinner = Spinner(view!!.findViewById(R.id.spinner))
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

    private fun openprofile(p: String?, idx: Int) {
        startActivity(Proxy.Instance?.profileintent(p, idx))
    }

    private fun setdaemon(idx: Int, srv: String, host: String, port: String) {
        android.util.Log.d("daemon",idx.toString() + ':' + srv + ':' + host + ':' + port)
        if (srv == "now") return
        var d: Daemon? = daemons
        if (d == null) {
            daemons = Daemon(idx, srv, host, port)
            return
        }
        while (true) {
            if (d!!.srv == srv) return
            if (d!!.chain != null) {
                d = d.chain
            } else {
                d!!.chain = Daemon(idx, srv, host, port)
                return
            }
        }
    }

    private fun setuser(idx: Int, login: String) {
        android.util.Log.d("user", idx.toString() + ':' + login)
        if (spinner != null) {
            spinner?.remove()
            spinner = null
            view!!.findViewById<TextView>(R.id.users_status).text = getText(R.string.users_list)
        }
        val tbl: TableLayout = view!!.findViewById(R.id.users)
        var tr: TableRow? = tbl.getChildAt(idx) as TableRow?
        if (tr == null) {
            tr = layoutInflater.inflate(R.layout.user_row, null) as TableRow
            tbl.addView(tr)
        }
        tr.findViewById<TextView>(R.id.user).text = login
        tr.findViewById<ImageView>(R.id.profile).setOnClickListener {
            openprofile(login, idx)
        }
    }

    override fun onTrimMemory(level: Int) {
        var i = 0
        while (i < bullets.size) bullets[i++] = null
    }

    private fun proxy(): Boolean {
        if (fproxy) return true
        if (Proxy.Instance == null) return false
        Proxy.Instance!!.getdaemons(this)
        fproxy = true
        return true
    }

    private fun bullet(b: Int): Drawable {
        if (bullets[b] == null) {
            bullets[b] = ContextCompat.getDrawable(applicationContext, bulletRcs[b])
        }
        return bullets[b]!!
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
        img.background = bullet(b)
        return rs
    }

    private fun showstat(flg: Boolean): Boolean {
        if (spinner != null) {
            spinner?.step(bullet(1), bullet(2))
            return true
        }
        val utbl: TableLayout = view!!.findViewById(R.id.users)
        var idx = 0
        var rs = false
        while (true) {
            val ch = utbl.getChildAt(idx) ?: break
            val st = ustat[idx++]
            if (setbullet(ch.findViewById<ImageView>(R.id.stat), st, flg)) rs = true
            if ((st and 0x8000) != 0) {
                if (uerror.size <= idx) uerror = uerror.copyOf(idx + 1)
                if (uerror[idx] == 0) {
                    ch.findViewById<ImageView>(R.id.profile).background = ContextCompat.getDrawable(applicationContext, R.drawable.ic_baseline_settings_e_24)
                    uerror[idx] = 1;
                }
            } else if (idx < uerror.size && uerror[idx] != 0) {
                ch.findViewById<ImageView>(R.id.profile).background = ContextCompat.getDrawable(applicationContext, R.drawable.ic_baseline_settings_24)
                uerror[idx] = 0;
            }
        }
        return rs
    }

    private fun daemonerror(d: Daemon?) {
        if (d == derror) return
        derror = d
        val st: TextView = view!!.findViewById(R.id.users_status)
        val color: Int
        if (d != null) {
            st.text = getString(R.string.proxy_error).replaceFirst("%s", d.port)
            color = R.color.vesmail_error
        } else {
            st.text = getString(R.string.users_list)
            color = R.color.vesmail
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            st.setTextColor(resources.getColor(color, theme))
        else
            st.setTextColor(resources.getColor(color))
    }

    private fun watch() {
        if (handler == null) handler = Handler(mainLooper)
        fwatching = fwatch
        if (fwatching) handler!!.postDelayed(watchfn, 250)
        if (proxy()) {
            Proxy.Instance!!.watched = true
            val stat = Proxy.Instance!!.watch()
            var d: Daemon? = daemons
            while (d != null) {
                if ((stat[d.idx] and 0x0002) != 0) {
                    break
                }
                d = d.chain
            }
            daemonerror(d)
            ustat = Proxy.Instance!!.getusers(this, view!!.findViewById<TableLayout>(R.id.users).childCount)
            Proxy.Instance!!.usererrors(ustat)
            val blink = showstat(true)
            if (blink) handler!!.postDelayed(blinkfn, 125)
        }
    }

}