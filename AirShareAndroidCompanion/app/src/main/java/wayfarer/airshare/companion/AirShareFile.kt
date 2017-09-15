package org.airshare.companion

import java.io.File

class AirShareFile (private val fileName: String, private val data: ByteArray) {

    @kotlin.jvm.JvmOverloads
    fun writeTo(path: String, name: String? = null) : File? {
        File(path).mkdirs()

        val p = path + "/" + (if (name.isNullOrEmpty()) fileName else name)
        val res = File(p)
        res.writeBytes(data)
        return res
    }
}
