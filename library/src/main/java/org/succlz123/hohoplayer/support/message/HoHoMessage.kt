package org.succlz123.hohoplayer.support.message

class HoHoMessage private constructor() {

    companion object {
        const val UID_NONE = -1

        const val FLAG_IN_USE = 1 shl 0

        const val FLAG_ASYNCHRONOUS = 1 shl 1

        val sPoolSync = Any()

        private var sPool: HoHoMessage? = null

        private var sPoolSize = 0

        private const val MAX_POOL_SIZE = 50

        fun obtain(): HoHoMessage {
            synchronized(sPoolSync) {
                val m = sPool
                if (m != null) {
                    sPool = m.next
                    m.next = null
                    m.flags = 0 // clear in-use flag
                    sPoolSize--
                    return m
                }
            }
            return HoHoMessage()
        }

        fun obtain(orig: HoHoMessage): HoHoMessage {
            val m = obtain()
            m.what = orig.what
            m.argInt = orig.argInt
            m.argLong = orig.argLong
            m.argDouble = orig.argDouble
            m.argFloat = orig.argFloat
            m.argBol = orig.argBol
            m.argString = orig.argString
            m.argObj = orig.argObj

            m.sendingUid = orig.sendingUid
            m.workSourceUid = orig.workSourceUid
            val data = orig.extra
            if (data != null) {
                m.extra = HashMap(data)
            }
            m.callback = orig.callback
            return m
        }

        fun obtain(what: Int = 0, argInt: Int = 0, argLong: Long = 0L, argDouble: Double = 0.0,
                   argFloat: Float = 0.0f, argBol: Boolean = false, argString: String? = null,
                   argObj: Any? = null, extra: HashMap<String, Any?>? = null): HoHoMessage {
            val m = obtain()
            m.what = what
            m.argInt = argInt
            m.argLong = argLong
            m.argDouble = argDouble
            m.argFloat = argFloat
            m.argBol = argBol
            m.argString = argString
            m.argObj = argObj
            m.extra = extra
            return m
        }
    }

    var what = 0

    var argInt: Int = 0
    var argLong: Long = 0L
    var argDouble: Double = 0.0
    var argFloat: Float = 0.0f
    var argBol: Boolean = false
    var argString: String? = null
    var argObj: Any? = null

    var extra: HashMap<String, Any?>? = null

    var sendingUid = UID_NONE

    var workSourceUid = UID_NONE

    var flags = 0

    var time: Long = 0

    var callback: Runnable? = null

    var next: HoHoMessage? = null

    fun recycle() {
        check(!isInUse) { "This message cannot be recycled because it is still in use." }
        recycleUnchecked()
    }

    private fun recycleUnchecked() {
        // Mark the message as in use while it remains in the recycled object pool.
        // Clear out all other details.
        flags = FLAG_IN_USE

        what = 0
        argInt = 0
        argLong = 0L
        argDouble = 0.0
        argFloat = 0.0f
        argBol = false
        argString = null
        argObj = null
        extra?.clear()
        extra = null

        sendingUid = UID_NONE
        workSourceUid = UID_NONE
        time = 0
        callback = null
        synchronized(sPoolSync) {
            if (sPoolSize < MAX_POOL_SIZE) {
                next = sPool
                sPool = this
                sPoolSize++
            }
        }
    }

    fun getDataNoNone(): HashMap<String, Any?> {
        var d = extra
        if (d == null) {
            d = HashMap()
        }
        extra = d
        return d
    }

    var isAsynchronous: Boolean
        get() = flags and FLAG_ASYNCHRONOUS != 0
        set(async) {
            flags = if (async) {
                flags or FLAG_ASYNCHRONOUS
            } else {
                flags and FLAG_ASYNCHRONOUS.inv()
            }
        }

    val isInUse: Boolean
        get() = flags and FLAG_IN_USE == FLAG_IN_USE

    fun markInUse() {
        flags = flags or FLAG_IN_USE
    }

    fun getIntFromExtra(key: String): Int? {
        return extra?.get(key) as? Int
    }

    fun getIntFromExtra(key: String, default: Int): Int {
        return extra?.get(key) as? Int ?: default
    }

    override fun toString(): String {
        return "HoHoMessage(what=$what, argInt=$argInt, argLong=$argLong, argDouble=$argDouble, argFloat=$argFloat, argBol=$argBol, argChar=$argString, argObj=$argObj, data=$extra, sendingUid=$sendingUid, workSourceUid=$workSourceUid, flags=$flags, time=$time, callback=$callback, next=$next)"
    }
}