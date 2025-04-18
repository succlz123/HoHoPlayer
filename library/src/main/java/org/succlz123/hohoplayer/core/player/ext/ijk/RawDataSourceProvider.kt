package org.succlz123.hohoplayer.core.player.ext.ijk

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.net.Uri
import tv.danmaku.ijk.media.player.misc.IMediaDataSource
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

class RawDataSourceProvider private constructor(private var descriptor: AssetFileDescriptor?) : IMediaDataSource {

    companion object {

        fun create(context: Context, uri: Uri?): RawDataSourceProvider? {
            uri ?: return null
            try {
                val fileDescriptor = context.contentResolver.openAssetFileDescriptor(uri, "r")
                        ?: return null
                return RawDataSourceProvider(fileDescriptor)
            } catch (ignore: FileNotFoundException) {
            }
            return null
        }
    }

    private var mediaBytes: ByteArray? = null

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        mediaBytes?.let {
            if (position + 1 >= it.size) {
                return -1
            }
            var length: Int
            if (position + size < it.size) {
                length = size
            } else {
                length = (it.size - position).toInt()
                if (length > buffer.size) length = buffer.size
                length--
            }
            System.arraycopy(it, position.toInt(), buffer, offset, length)
            return length
        } ?: run {
            return -1
        }
    }

    @Throws(IOException::class)
    override fun getSize(): Long {
        val length = descriptor?.length ?: 0L
        val inputStream = descriptor?.createInputStream()
        if (mediaBytes == null && inputStream != null) {
            mediaBytes = readBytes(inputStream)
        }
        return length
    }

    @Throws(IOException::class)
    override fun close() {
        descriptor?.close()
        descriptor = null
        mediaBytes = null
    }

    @Throws(IOException::class)
    private fun readBytes(inputStream: InputStream): ByteArray {
        val byteBuffer = ByteArrayOutputStream()
        val bufferSize = 1024
        val buffer = ByteArray(bufferSize)
        var len: Int
        while (inputStream.read(buffer).also { len = it } != -1) {
            byteBuffer.write(buffer, 0, len)
        }
        return byteBuffer.toByteArray()
    }
}