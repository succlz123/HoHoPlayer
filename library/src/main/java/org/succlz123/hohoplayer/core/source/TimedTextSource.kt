package org.succlz123.hohoplayer.core.source

import java.io.Serializable

class TimedTextSource : Serializable {
    var path: String
    var mimeType: String? = null
    var flag = 0

    constructor(path: String) {
        this.path = path
    }

    constructor(path: String, mimeType: String?) {
        this.path = path
        this.mimeType = mimeType
    }

    constructor(path: String, mimeType: String?, flag: Int) {
        this.path = path
        this.mimeType = mimeType
        this.flag = flag
    }
}