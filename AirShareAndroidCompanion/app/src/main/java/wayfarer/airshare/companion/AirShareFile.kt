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

//private fun File.fillWith(data: JSONArray): File? {
//    val buff = data.toByteArray()
//    this.writeBytes(buff)
//    return this
//}

//private fun JSONArray.toByteArray(): ByteArray {
//    val buff = ByteArray(this.length())
//
//    for (i in buff.indices) {
//        buff[i] = (this[i] as Int).toByte()
//    }
//
//    return buff
//}
