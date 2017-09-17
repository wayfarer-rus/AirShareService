package wayfarer.airshare.companion

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.EditText
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        verifyStoragePermissions(this)
    }

    fun onConnect(view: View) {
        Log.d(TAG, "connect button pressed")
        val remoteIp = (this.findViewById(R.id.remoteServerIP) as EditText).text.toString()
        Log.d(TAG, "input from user: " + remoteIp)

        if (!remoteIp.isEmpty()) {
            val url = buildUrl(remoteIp)
            val a = this

            doAsync {
                try {
                    val r = khttp.get(url, timeout= 5.0)

                    if (r.statusCode != 200) {
                        uiThread {
                            toast("Connection error: " + r.statusCode)
                        }
                    } else {
                        FileExplorerState.instance.remoteUrl = url
                        val intent = Intent(a, AirShareActivity::class.java)
                        startActivity(intent)
                    }
                } catch (error: Exception) {
                    uiThread {
                        toast("Connection error: " + error.localizedMessage)
                    }
                }
            }
        }
    }

    private fun buildUrl(ip: String): String {
        return "http://$ip:8080"
    }

    companion object {
        private val TAG = "Main"

        // Storage Permissions
        private val REQUEST_EXTERNAL_STORAGE = 1
        private val PERMISSIONS_STORAGE = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        /**
         * Checks if the app has permission to write to device storage
         *
         * If the app does not has permission then the user will be prompted to grant permissions
         *
         * @param activity
         */
        fun verifyStoragePermissions(activity: Activity) {
            // Check if we have write permission
            val permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)

            if (permission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(
                        activity,
                        PERMISSIONS_STORAGE,
                        REQUEST_EXTERNAL_STORAGE
                )
            }
        }

        /**
         * @param view         View to animate
         * @param toVisibility Visibility at the end of animation
         * @param toAlpha      Alpha at the end of animation
         * @param duration     Animation duration in ms
         */
        fun animateView(view: View, toVisibility: Int, toAlpha: Float, duration: Int) {
            val show = toVisibility == View.VISIBLE
            if (show) {
                view.alpha = 0f
            }
            view.visibility = View.VISIBLE
            view.animate()
                    .setDuration(duration.toLong())
                    .alpha(if (show) toAlpha else 0F)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            view.visibility = toVisibility
                        }
                    })
        }
    }
}
