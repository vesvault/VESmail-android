package com.vesvault.vesmail

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView

class Spinner constructor(v: View) {
    val view = v
    val ids: Array<Int> = arrayOf(R.id.stby0, R.id.stby1, R.id.stby2, R.id.stby3, R.id.stby4, R.id.stby5, R.id.stby6, R.id.stby7)
    var idx: Int = 0;

    fun step(b_off: Drawable, b_on: Drawable) {
        view.findViewById<ImageView>(ids[idx++]).background = b_off
        if (idx >= ids.size) idx = 0;
        view.findViewById<ImageView>(ids[idx]).background = b_on
    }

    fun remove() {
        view.visibility = View.GONE
    }
}