package wayfarer.airshare.companion

import android.os.Environment
import java.io.File
import java.io.Serializable
import java.util.*

/**
 * Created by wayfarer on 04/09/2017.
 */

internal class FileExplorerState private constructor() : Serializable {
    var remoteUrl: String? = null
        set(remoteUrl) {
            field = remoteUrl
            this.remoteUrlForDownload = remoteUrl + "/__download__"
            this.remoteUrlForUpload = remoteUrl + "/__upload__"
        }
    var remoteUrlForDownload: String? = null
        private set
    var remoteUrlForUpload: String? = null
        private set
    var isRemoteIsRoot: Boolean = false
    var remotePath = "/"
    private val remoteHistory = Stack<String>()

    var path = File(Environment.getExternalStorageDirectory().toString()
//            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
            + "")
    // Check if the first level of the directory structure is the one showing
    private var firstLvl: Boolean? = true

    private var history = Stack<String>()

    var isFirstLevel: Boolean
        get() = firstLvl!!
        set(isFirstLevel) {
            this.firstLvl = isFirstLevel
        }

    fun pushToHistory(path: String) {
        history.push(path)
    }

    fun popFromHistory(): String {
        return history.pop()
    }

    val isEmptyHistory: Boolean
        get() = history.empty()

    fun pushToRemoteHistory(file: String) {
        remoteHistory.push(file)
    }

    fun popFromRemoteHistory(): String {
        return remoteHistory.pop()
    }

    val isEmptyRemoteHistory: Boolean
        get() = remoteHistory.empty()

    companion object {
        val instance = FileExplorerState()

        fun reinitState(state: FileExplorerState) {
            instance.firstLvl = state.firstLvl
            instance.path = state.path
            instance.history = state.history
        }
    }
}
