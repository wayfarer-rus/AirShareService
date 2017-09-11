package wayfarer.airshare.companion

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.ListFragment
import android.support.v4.content.res.ResourcesCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import khttp.extensions.fileLike
import khttp.responses.Response
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.uiThread
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import kotlin.collections.set

/**
 * Created by wayfarer on 07/09/2017.
 */

class FileExplorerControlsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_file_explorer_controls, container, false)
        rootView.findViewById<View>(R.id.unselectAll)
                .setOnClickListener { onUnselectAll() }

        rootView.findViewById<View>(R.id.selectAll)
                .setOnClickListener { onSelectAll() }

        rootView.findViewById<View>(R.id.sendToRemote)
                .setOnClickListener { onSend() }

        if (this.parentFragment is RemoteFileExplorerFragment) {
            val v = rootView.findViewById<Button>(R.id.sendToRemote)
            v.setText(R.string.remote_controls_button_download)
        }

        return rootView
    }

    private fun onSelectAll() {
        val f = this.parentFragment as ListFragment

        when (f) {
            is LocalFileExplorerFragment -> Log.d(TAG, "Select All pressed on Local View")
            is RemoteFileExplorerFragment -> Log.d(TAG, "Select All pressed on Remote View")
            else -> Log.d(TAG, "Select All pressed on " + f.toString())
        }

        val count = f.listView.count
        Log.d(TAG, "count is " + count)
        setSelection(f, count, true)
    }

    private fun setSelection(f: ListFragment, count: Int, selection: Boolean) {
        for (i in 0 until count) {
            val item = f.listView.getItemAtPosition(i) as Item

            if (item.type == Item.Type.UP)
                continue

            item.isSelected = selection
            val view = f.listView.getChildAt(i)

            if (view != null) {
                view.isSelected = selection
                val c = ResourcesCompat.getColor(resources,
                        if (selection) R.color.selectedItemColor else R.color.defaultItemColor, null)
                view.setBackgroundColor(c)
            }
        }
    }

    private fun onUnselectAll() {
        Log.d(TAG, "Unselect All pressed")
        val f = this.parentFragment as ListFragment
        setSelection(f, f.listView.count, false)
    }

    private fun onSend() {
        Log.d(TAG, "Send To Remote pressed")
        val f = this.parentFragment as ListFragment
        val selectedItems = (0 until f.listView.count)
                .map { f.listView.getItemAtPosition(it) as Item }
                .filter { it.isSelected }

        if (selectedItems.isEmpty())
            return

        if (f is LocalFileExplorerFragment) {
            upload(selectedItems, f)
        } else if (f is RemoteFileExplorerFragment) {
            download(selectedItems, f)
        }
    }

    private fun download(selectedItems: List<Item>, fragment: RemoteFileExplorerFragment) {
        val state = FileExplorerState.instance
        fragment.showProgressBar()

        doAsync {
            selectedItems
                    .map { (state.remotePath + "/" + it.file).replace("//+".toRegex(), "/") }
                    .forEach {
                        val r = khttp.get(state.remoteUrlForDownload!!, params = mapOf("path" to it))

                        if (failResponse(r)) {
                            uiThread {
                                fragment.hideProgressBar()
                                toast("Download failed. Check connection.")
                            }

                            return@doAsync
                        }
                        else {
                            saveTo(state.path.toString(), r.jsonObject)
                        }
                    }

            uiThread {
                fragment.hideProgressBar()
                fragment.parentActivity.loadLocalFileList()
            }
        }
    }

    private fun failResponse(r: Response): Boolean {
        if (r.statusCode != 200) {
            Log.d(TAG, "Response code: " + r.statusCode)
            Log.d(TAG, "Check server logs")
            return true
        }

        return false
    }

    @Throws(JSONException::class)
    private fun saveTo(path: String, json: JSONObject) {
        val iter = json.keys()

        while (iter.hasNext()) {
            val key = iter.next()

            when (key) {
                "file" -> {
                    val fileName = json.getJSONObject("file").getString("path")
                    val fileData = json.getJSONObject("file").getJSONObject("data").getJSONArray("data")
                    val file = AirShareFile(fileName, fileData)
                    file.toFile(path)

                }
                "folder" -> {
                    val folder = json.getJSONObject("folder")
                    val folderPath = folder.getString("path")
                    val newPath = path + "/" + folderPath

                    if (folder.has("folders")) {
                        saveToLoop(newPath, folder.getJSONArray("folders"))
                    }

                    if (folder.has("files")) {
                        saveToLoop(newPath, folder.getJSONArray("files"))
                    }
                }
            }
        }
    }

    @Throws(JSONException::class)
    private fun saveToLoop(path: String, jsonArray: JSONArray) {
        for (i in 0 until jsonArray.length())
            saveTo(path, jsonArray.getJSONObject(i))
    }

    private fun upload(selectedItems: List<Item>, fragment: LocalFileExplorerFragment) {
        val state = FileExplorerState.instance
        fragment.showProgressBar()
        val cacheDir = context.cacheDir

        doAsync {
            var sleepForUnzipTime = 0L

            for (item in selectedItems) {
                val wherePath = state.remotePath.replace("//+".toRegex(), "/")
                val whatPath = state.path.toString() + "/" + item.file
                val params = mutableMapOf("path" to wherePath)
                val f:File

                if (item.isFolder) {
                    f = File.createTempFile("_date", "zip", cacheDir)
                    params["unzip"] = "true"
                    // zip it to file
                    val before = System.currentTimeMillis()
                    ZipFileUtil.zipDirectory(File(whatPath), f)
                    sleepForUnzipTime += System.currentTimeMillis() - before
                } else {
                    f = File(whatPath)
                }

                val files = listOf(f.fileLike())
                val r = khttp.post(state.remoteUrlForUpload!!, params = params, files = files)

                if (failResponse(r)) {
                    uiThread {
                        fragment.hideProgressBar()
                        toast("Upload failed. Check connection.")
                    }

                    return@doAsync
                }

            }

            Thread.sleep(sleepForUnzipTime)

            uiThread {
                fragment.hideProgressBar()
                fragment.parentActivity.loadRemoteFileList()
            }

        }
    }

    companion object {
        private val TAG = "FEControls_Fragment"
    }

}
