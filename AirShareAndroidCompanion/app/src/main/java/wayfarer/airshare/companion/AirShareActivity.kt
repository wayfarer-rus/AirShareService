package wayfarer.airshare.companion

import android.net.Uri
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.view.ViewPager
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import org.json.JSONArray
import java.io.File
import java.io.FilenameFilter

/**
 * Created by wayfarer on 07/09/2017.
 */

class AirShareActivity : FragmentActivity() {
    /* Data for the file system walker */
    private var localFileList: Array<Item?>? = null
    private var remoteFileList: Array<Item?>? = null
    /* file system walker state (singleton) */
    private var state: FileExplorerState? = null

    internal var fileExplorerPagerAdapter: FileExplorerPagerAdapter? = null
    private lateinit var mViewPager: ViewPager

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // recovering the instance state
        if (savedInstanceState != null) {
            assert(savedInstanceState.getSerializable(FES_STATE_KEY) != null)
            FileExplorerState.reinitState(savedInstanceState.getSerializable(FES_STATE_KEY) as FileExplorerState)
        }

        state = FileExplorerState.instance
        setContentView(R.layout.activity_multitab)
        // Create an adapter that when requested, will return a fragment representing an object in
        // the collection.
        //
        // ViewPager and its adapters use support library fragments, so we must use
        // getSupportFragmentManager.
        fileExplorerPagerAdapter = FileExplorerPagerAdapter(this, supportFragmentManager)

        // Set up the ViewPager, attaching the adapter.
        mViewPager = findViewById(R.id.pager)
        mViewPager.adapter = fileExplorerPagerAdapter

        loadLocalFileList()
        loadRemoteFileList()
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        FileExplorerState.reinitState(savedInstanceState.getSerializable(FES_STATE_KEY) as FileExplorerState)
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(FES_STATE_KEY, FileExplorerState.instance)
        // call superclass to save any view hierarchy
        super.onSaveInstanceState(outState)
    }

    fun loadRemoteFileList() {
        val url = state!!.remoteUrl!! + Uri.encode(state!!.remotePath, "/").replace("//+".toRegex(), "/")

        doAsync {
            val r = khttp.get(url)

            if (r.statusCode != 200) {
                remoteFileList = arrayOfNulls(0)

                uiThread {
                    toast("Connection error: " + r.statusCode)
                }
            } else {
                val json = r.jsonObject
                val folders = json["folders"] as JSONArray
                val files = json["files"] as JSONArray

                remoteFileList = Array(folders.length(),
                        init = {i ->
                            if (".." == folders.getString(i)) {
                                state!!.isRemoteIsRoot = false
                                Item("Up", R.drawable.directory_up, Item.Type.UP)
                            } else {
                                state!!.isRemoteIsRoot = true
                                Item(folders.getString(i), R.drawable.directory_icon, Item.Type.FOLDER)
                            }
                        })

                remoteFileList = remoteFileList!!.plus(Array(files.length(), init = { i ->
                    Item(files.getString(i), R.drawable.file_icon, Item.Type.FILE)
                }))
            }

            uiThread {
                createRemoteAdapter()
            }
        }
    }

    private fun createRemoteAdapter() {
        val adapter = object : ArrayAdapter<Item>(this,
                android.R.layout.select_dialog_item, android.R.id.text1,
                remoteFileList!!) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                // creates view
                val view = super.getView(position, convertView, parent)
                val textView = view
                        .findViewById<TextView>(android.R.id.text1)

                // put the image on the text view
                textView.setCompoundDrawablesWithIntrinsicBounds(
                        remoteFileList!![position]!!.icon, 0, 0, 0)

                if (remoteFileList!![position]!!.isSelected) {
                    view.isSelected = true
                    val c = ResourcesCompat.getColor(resources, R.color.selectedItemColor, null)
                    view.setBackgroundColor(c)
                } else {
                    view.isSelected = false
                    val c = ResourcesCompat.getColor(resources, R.color.defaultItemColor, null)
                    view.setBackgroundColor(c)
                }

                // add margin between image and text (support various screen
                // densities)
                val dp5 = (5 * resources.displayMetrics.density + 0.5f).toInt()
                textView.compoundDrawablePadding = dp5

                parent.setOnTouchListener { _, event ->
                    //                        Log.d(TAG, "ListElement Touched: " + v.toString());
                    val detector = fileExplorerPagerAdapter!!.remoteTouchDetector
                    detector.onTouchEvent(event)
                }

                return view
            }
        }

        val fragment = fileExplorerPagerAdapter!!.getItem(1) as RemoteFileExplorerFragment
        fragment.listAdapter = adapter
    }

    fun loadLocalFileList() {
        localFileList = null

        try {
            state!!.path.mkdirs()
        } catch (e: SecurityException) {
            Log.e(TAG, "unable to write on the sd card ")
        }

        // Checks whether path exists
        if (state!!.path.exists()) {
            val filter = FilenameFilter { dir, filename ->
                val sel = File(dir, filename)
                // Filters based on whether the file is hidden or not
                (sel.isFile || sel.isDirectory) && !sel.isHidden
            }

            var fList: Array<String?>? = state!!.path.list(filter)
            if (fList == null) fList = arrayOfNulls(0)

            localFileList = Array(fList.size, init = {i ->
                val item = Item(fList!![i], R.drawable.file_icon, Item.Type.FILE)
                // Convert into file path
                val sel = File(state!!.path, fList!![i])

                // Set drawables
                if (sel.isDirectory) {
                    item.icon = R.drawable.directory_icon
                    item.type = Item.Type.FOLDER
                    Log.d("DIRECTORY", item.file)
                } else {
                    Log.d("FILE", item.file)
                }

                item
            })

            if (!state!!.isFirstLevel) {
                val temp = Array<Item?>(1, init = {i ->
                    Item("Up", R.drawable.directory_up, Item.Type.UP)
                })

                localFileList = temp.plus(localFileList!!)
                /*
                val temp = arrayOfNulls<Item>(localFileList!!.size + 1)
                System.arraycopy(localFileList!!, 0, temp, 1, localFileList!!.size)
                temp[0] = Item("Up", R.drawable.directory_up, Item.Type.UP)
                localFileList = temp*/
            }
        } else {
            Log.e(TAG, "path does not exist")
        }

        createLocalAdapter()
    }

    private fun createLocalAdapter() {
        val adapter = object : ArrayAdapter<Item>(this,
                android.R.layout.select_dialog_item, android.R.id.text1,
                localFileList!!) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                // creates view
                val view = super.getView(position, convertView, parent)
                val textView = view
                        .findViewById<TextView>(android.R.id.text1)

                // put the image on the text view
                textView.setCompoundDrawablesWithIntrinsicBounds(
                        localFileList!![position]!!.icon, 0, 0, 0)

                if (localFileList!![position]!!.isSelected) {
                    view.isSelected = true
                    val c = ResourcesCompat.getColor(resources, R.color.selectedItemColor, null)
                    view.setBackgroundColor(c)
                } else {
                    view.isSelected = false
                    val c = ResourcesCompat.getColor(resources, R.color.defaultItemColor, null)
                    view.setBackgroundColor(c)
                }

                // add margin between image and text (support various screen
                // densities)
                val dp5 = (5 * resources.displayMetrics.density + 0.5f).toInt()
                textView.compoundDrawablePadding = dp5

                parent.setOnTouchListener { _, event ->
                    //                        Log.d(TAG, "ListElement Touched: " + v.toString());
                    val detector = fileExplorerPagerAdapter!!.localTouchDetector
                    detector.onTouchEvent(event)
                }

                return view
            }
        }

        val fragment = fileExplorerPagerAdapter!!.getItem(0) as LocalFileExplorerFragment
        fragment.listAdapter = adapter
    }

    companion object {
        private val TAG = "AirShareActivity"
        private val FES_STATE_KEY = "FileExporterStateKey"
    }
}
