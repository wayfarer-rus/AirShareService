package wayfarer.airshare.companion

import org.json.JSONArray
import java.io.File

class AirShareFile (private val fileName: String, private val data: JSONArray) {

    fun toFile(path: String) : File? {
        File(path).mkdirs()
        return File(path + "/" + fileName).fillWith(data)
    }
}

private fun File.fillWith(data: JSONArray): File? {
    val buff = data.toByteArray()
    this.writeBytes(buff)
    return this
}

private fun JSONArray.toByteArray(): ByteArray {
    val buff = ByteArray(this.length())

    for (i in buff.indices) {
        buff[i] = (this[i] as Int).toByte()
    }

    return buff
}
