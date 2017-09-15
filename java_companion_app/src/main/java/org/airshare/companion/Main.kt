package org.airshare.companion

import khttp.extensions.fileLike
import khttp.get
import khttp.post
import khttp.responses.Response
import org.json.JSONArray
import java.io.File

val remoteUrl = "http://127.0.0.1:8080"
val remoteUrlForDownload = remoteUrl + "/__download__"
val remoteUrlForUpload = remoteUrl + "/__upload__"
var path = "/"

fun main(args: Array<String>) {
    println("Cmd interface for testing air-share service.")
    println("Type \\h for help. Type \\q to quit.")

    do {
        print("[$path] > ")
        val input = readLine()
        val cmd = parse(input!!)
//        println("you've entered: " + input)

        when (cmd[0]) {
            "ls" -> listRemoteDirectory()
            "cd" -> changeRemoteDirectory(cmd)
            "upload" -> uploadFiles(cmd)
            "download" -> downloadFiles(cmd)
            "\\h" -> showHelp()
            else -> {
                println("Unknown command \"$input\". Type \\h for help.")
            }
        }
    } while (input != "\\q")
}

fun showHelp() {
    println("\\h -> show this help")
    println("\\q -> quit")
    println("ls -> show directory content")
    println("cd <path> -> change directory to <path>")
    println("upload <what_path> <where_path> -> upload file or folder to the server")
    println("download <what_path> <where_path> -> download file or folder from the server")
}

fun uploadFiles(cmd: List<String>) {
    val url = remoteUrlForUpload
    val whatPath = cmd[1]
    var wherePath = path

    if (cmd[2] == "..") {
        wherePath = wherePath.dropLastWhile { it != '/' }
    }
    else if (cmd[2] != ".") {
        wherePath = wherePath + "/" + cmd[2]
    }

    val f: File
    val params = mutableMapOf("path" to wherePath)

    if (File(whatPath).isDirectory) {
        f = File("./tmp/_data.zip")
        params["unzip"] = "true"
        // zip it to file
        ZipFileUtil.zipDirectory(File(whatPath), f)
    }
    else {
        f = File(whatPath)
    }

    val files = listOf(f.fileLike())
    val r = post(url, params = params, files = files)

    if (failResponse(r)) return
}

fun downloadFiles(cmd: List<String>) {
    val url = remoteUrlForDownload
    var whatPath = path
    val wherePath = "./tmp"

    if (cmd[1] == "..") {
        whatPath = whatPath.dropLastWhile { it != '/' }
    }
    else if (cmd[1] != ".") {
        whatPath = whatPath + "/" + cmd[1]
    }

    val r = get(url, params = mapOf("path" to whatPath))

    if (failResponse(r)) return

    saveTo(wherePath, r.headers, r.content)
}

private fun saveTo(path: String, headers: Map<String, String>, content: ByteArray) {
    when (headers["Content-Type"]) {
        "file" -> {
            val file = AirShareFile(headers["File-Name"]!!, content)
            file.writeTo(path)
        }
        "folder" -> {
            println("Type is folder")
            val zipFile = AirShareFile(headers["File-Name"]!!, content)
            val f = zipFile.writeTo("./tmp/")
            ZipFileUtil.unzip(f!!, "./tmp/" + headers["Folder-Name"])
            f.delete()
        }
    }
}

fun changeRemoteDirectory(cmd: List<String>) {
    when {
        cmd[1] == ".."      -> path = path.dropLastWhile { it != '/' }
        cmd[1][0] == '/'    -> path = cmd[1]
        path.last() == '/'  -> path += cmd[1]
        else -> path = path + "/" + cmd[1]
    }

    if (path.length > 1 && path.last() == '/') {
        path = path.dropLast(1)
    }
}

fun parse(input: String): List<String> {
    return input.split(" ")
}

fun listRemoteDirectory() {
    val url = remoteUrl + path
    val r = get(url)

    if (failResponse(r)) return

    val json = r.jsonObject
    val folders = json["folders"] as JSONArray
    val files = json["files"] as JSONArray

    println("--- Directories:")
    for (v in folders) println ("[$v]")

    println("--- Files:")
    for (v in files) println ("$v")
}

private fun failResponse(r: Response): Boolean {
    if (r.statusCode != 200) {
        println("Response code: " + r.statusCode)
        println("Check server logs")
        return true
    }
    return false
}
