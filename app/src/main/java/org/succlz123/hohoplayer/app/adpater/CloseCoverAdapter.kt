package org.succlz123.hohoplayer.app.adpater

import android.content.Context
import android.view.View
import org.succlz123.hohoplayer.app.R
import org.succlz123.hohoplayer.app.support.AppPlayerData
import org.succlz123.hohoplayer.core.adapter.BaseCoverAdapter
import org.succlz123.hohoplayer.support.message.HoHoMessage

class CloseCoverAdapter(context: Context) : BaseCoverAdapter(context) {

    override val key: String
        get() = "CloseCoverAdapter"

    public override fun onCreateCoverView(context: Context): View {
        return View.inflate(context, R.layout.layout_close_cover, null)
    }

    override fun getCoverLevel(): Int {
        return levelMedium(10)
    }

    override fun onAdapterBind() {
        getView().findViewById<View>(R.id.iv_close).setOnClickListener {
            sendOne2ManyAdapterEvent(HoHoMessage.obtain(what = AppPlayerData.Event.EVENT_CODE_REQUEST_CLOSE))
        }
    }
}
