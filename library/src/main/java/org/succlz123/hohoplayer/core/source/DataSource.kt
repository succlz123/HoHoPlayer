package org.succlz123.hohoplayer.core.source

import android.content.ContentResolver
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.net.Uri
import java.io.IOException
import java.io.Serializable
import java.util.*

class DataSource : Serializable {

    var tag: String? = null

    var data: String? = null

    var fromNetwork: Boolean = true

    var title: String? = null

    var id: Long = 0

    var uri: Uri? = null

    var extra: HashMap<String, String>? = null

    var timedTextSource: TimedTextSource? = null

    // a video folder in assets, the path name is video/xxx.mp4
    var assetsPath: String? = null

    // when play android raw resource, set this.
    var rawId = -1

    var startPos = 0

    var isLive = false

    constructor() {}

    constructor(data: String?) {
        this.data = data
    }

    constructor(title: String?, data: String?) {
        this.title = title
        this.data = data
    }

    override fun toString(): String {
        return "DataSource{" +
                "tag='" + tag + '\'' +
                ", data='" + data + '\'' +
                ", title='" + title + '\'' +
                ", id=" + id +
                ", uri=" + uri +
                ", extra=" + extra +
                ", timedTextSource=" + timedTextSource +
                ", assetsPath='" + assetsPath + '\'' +
                ", rawId=" + rawId +
                ", startPos=" + startPos +
                ", isLive=" + isLive +
                '}'
    }

    companion object {

        @JvmStatic
        fun buildRawPath(packageName: String, rawId: Int): Uri {
            return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + packageName + "/" + rawId)
        }

        @JvmStatic
        fun buildAssetsUri(assetsPath: String): Uri {
            return Uri.parse("file:///android_asset/$assetsPath")
        }

        @JvmStatic
        fun getAssetsFileDescriptor(context: Context, assetsPath: String?): AssetFileDescriptor? {
            return try {
                if (assetsPath.isNullOrEmpty()) {
                    null
                } else {
                    context.assets.openFd(assetsPath)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }
    }
}