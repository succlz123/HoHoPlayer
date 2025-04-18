package org.succlz123.hohoplayer.app.support

import androidx.recyclerview.widget.RecyclerView.ViewHolder

interface OnItemClickListener<H : ViewHolder?, T> {

    fun onItemClick(holder: H, item: T, position: Int)
}