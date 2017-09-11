package org.airshare.companion

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


object ZipFileUtil {
    @Throws(IOException::class)
    fun zipDirectory(dir: File, zipFile: File) {
        val fout = FileOutputStream(zipFile, false)
        val zout = ZipOutputStream(fout)
        zipSubDirectory(dir.name + "/", dir, zout)
        zout.close()
    }

    @Throws(IOException::class)
    private fun zipSubDirectory(basePath: String, dir: File, zout: ZipOutputStream) {
        val buffer = ByteArray(4096)
        val files = dir.listFiles()
        for (file in files!!) {
            if (file.isDirectory) {
                val path = basePath + file.name + "/"
                zout.putNextEntry(ZipEntry(path))
                zipSubDirectory(path, file, zout)
                zout.closeEntry()
            } else {
                val fin = FileInputStream(file)
                zout.putNextEntry(ZipEntry(basePath + file.name))

                do {
                    val length = fin.read(buffer)

                    if (length > 0 )
                        zout.write(buffer, 0, length)
                } while (length > 0)

                zout.closeEntry()
                fin.close()
            }
        }
    }
}