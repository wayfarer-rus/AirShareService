package wayfarer.airshare.companion;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Created by wayfarer on 07/09/2017.
 */

public class AirShareActivity extends FragmentActivity
{
    private static final String TAG = "AirShareActivity";
    private static final String FES_STATE_KEY = "FileExporterStateKey";
    /* Data for the file system walker */
    private Item[] localFileList;
    private Item[] remoteFileList;
    /* file system walker state (singleton) */
    private FileExplorerState state;

    FileExplorerPagerAdapter fileExplorerPagerAdapter;
    ViewPager mViewPager;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // recovering the instance state
        if (savedInstanceState != null) {
            assert (FileExplorerState) savedInstanceState.getSerializable(FES_STATE_KEY) != null;
            FileExplorerState.Companion.reinitState((FileExplorerState) savedInstanceState.getSerializable(FES_STATE_KEY));
        }

        state = FileExplorerState.Companion.getInstance();
        setContentView(R.layout.activity_multitab);
        // Create an adapter that when requested, will return a fragment representing an object in
        // the collection.
        //
        // ViewPager and its adapters use support library fragments, so we must use
        // getSupportFragmentManager.
        fileExplorerPagerAdapter = new FileExplorerPagerAdapter(this, getSupportFragmentManager());

        // Set up the ViewPager, attaching the adapter.
        mViewPager = findViewById(R.id.pager);
        mViewPager.setAdapter(fileExplorerPagerAdapter);

        loadLocalFileList();
        loadRemoteFileList();
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        FileExplorerState.Companion.reinitState((FileExplorerState) savedInstanceState.getSerializable(FES_STATE_KEY));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(FES_STATE_KEY, FileExplorerState.Companion.getInstance());
        // call superclass to save any view hierarchy
        super.onSaveInstanceState(outState);
    }

    void loadRemoteFileList() {
        final String url = state.getRemoteUrl() + Uri.encode(state.getRemotePath(), "/").replaceAll("//+", "/");

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("Volley.onResponse", response.toString());
                        try {
                            JSONArray folders = response.getJSONArray("folders");
                            JSONArray files = response.getJSONArray("files");
                            remoteFileList = new Item[folders.length() + files.length()];
                            int i = 0;

                            if ("..".equals(folders.getString(i))) {
                                remoteFileList[0] = new Item("Up", R.drawable.directory_up, Item.Type.UP);
                                state.setRemoteIsRoot(false);
                                ++i;
                            } else {
                                state.setRemoteIsRoot(true);
                            }

                            while (i < folders.length()) {
                                remoteFileList[i] = new Item(folders.getString(i), R.drawable.directory_icon, Item.Type.FOLDER);
                                ++i;
                            }

                            for (int j = 0; j < files.length(); ++j) {
                                remoteFileList[i++] = new Item(files.getString(j), R.drawable.file_icon, Item.Type.FILE);
                            }
                        } catch (Exception e) {
                            Log.e("Volley.onResponse", "Error while parsing JSON", e);
                            remoteFileList = new Item[0];
                        } finally {
                            createRemoteAdapter();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("Volley.onError", error.toString());
                        remoteFileList = new Item[0];
                        createRemoteAdapter();
                    }
                });

        VolleySingleton.getInstance(this).addToRequestQueue(jsObjRequest);
    }

    private void createRemoteAdapter() {
        ListAdapter adapter = new ArrayAdapter<Item>(this,
                android.R.layout.select_dialog_item, android.R.id.text1,
                remoteFileList)
        {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                // creates view
                View view = super.getView(position, convertView, parent);
                final TextView textView = view
                        .findViewById(android.R.id.text1);

                // put the image on the text view
                textView.setCompoundDrawablesWithIntrinsicBounds(
                        remoteFileList[position].getIcon(), 0, 0, 0);

                if (remoteFileList[position].isSelected()) {
                    view.setSelected(true);
                    int c = ResourcesCompat.getColor(getResources(), R.color.selectedItemColor, null);
                    view.setBackgroundColor(c);
                } else {
                    view.setSelected(false);
                    int c = ResourcesCompat.getColor(getResources(), R.color.defaultItemColor, null);
                    view.setBackgroundColor(c);
                }

                // add margin between image and text (support various screen
                // densities)
                int dp5 = (int) (5 * getResources().getDisplayMetrics().density + 0.5f);
                textView.setCompoundDrawablePadding(dp5);

                parent.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent event) {
//                        Log.d(TAG, "ListElement Touched: " + v.toString());
                        GestureDetectorCompat detector = fileExplorerPagerAdapter.getRemoteTouchDetector();
                        return detector.onTouchEvent(event);
                    }
                });

                return view;
            }
        };

        RemoteFileExplorerFragment fragment = (RemoteFileExplorerFragment) fileExplorerPagerAdapter.getItem(1);
        fragment.setListAdapter(adapter);
    }

    void loadLocalFileList() {
        localFileList = null;

        try {
            state.getPath().mkdirs();
        } catch (SecurityException e) {
            Log.e(TAG, "unable to write on the sd card ");
        }

        // Checks whether path exists
        if (state.getPath().exists()) {
            FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    File sel = new File(dir, filename);
                    // Filters based on whether the file is hidden or not
                    return (sel.isFile() || sel.isDirectory())
                            && !sel.isHidden();

                }
            };

            String[] fList = state.getPath().list(filter);
            if (fList == null) fList = new String[0];
            localFileList = new Item[fList.length];

            for (int i = 0; i < fList.length; i++) {
                localFileList[i] = new Item(fList[i], R.drawable.file_icon, Item.Type.FILE);

                // Convert into file path
                File sel = new File(state.getPath(), fList[i]);

                // Set drawables
                if (sel.isDirectory()) {
                    localFileList[i].setIcon(R.drawable.directory_icon);
                    localFileList[i].setType(Item.Type.FOLDER);
                    Log.d("DIRECTORY", localFileList[i].getFile());
                } else {
                    Log.d("FILE", localFileList[i].getFile());
                }
            }

            if (!state.isFirstLevel()) {
                Item temp[] = new Item[localFileList.length + 1];
                System.arraycopy(localFileList, 0, temp, 1, localFileList.length);
                temp[0] = new Item("Up", R.drawable.directory_up, Item.Type.UP);
                localFileList = temp;
            }
        } else {
            Log.e(TAG, "path does not exist");
        }

        ListAdapter adapter = new ArrayAdapter<Item>(this,
                android.R.layout.select_dialog_item, android.R.id.text1,
                localFileList)
        {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                // creates view
                View view = super.getView(position, convertView, parent);
                final TextView textView = view
                        .findViewById(android.R.id.text1);

                // put the image on the text view
                textView.setCompoundDrawablesWithIntrinsicBounds(
                        localFileList[position].getIcon(), 0, 0, 0);

                if (localFileList[position].isSelected()) {
                    view.setSelected(true);
                    int c = ResourcesCompat.getColor(getResources(), R.color.selectedItemColor, null);
                    view.setBackgroundColor(c);
                } else {
                    view.setSelected(false);
                    int c = ResourcesCompat.getColor(getResources(), R.color.defaultItemColor, null);
                    view.setBackgroundColor(c);
                }

                // add margin between image and text (support various screen
                // densities)
                int dp5 = (int) (5 * getResources().getDisplayMetrics().density + 0.5f);
                textView.setCompoundDrawablePadding(dp5);

                parent.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent event) {
//                        Log.d(TAG, "ListElement Touched: " + v.toString());
                        GestureDetectorCompat detector = fileExplorerPagerAdapter.getLocalTouchDetector();
                        return detector.onTouchEvent(event);
                    }
                });

                return view;
            }
        };

        LocalFileExplorerFragment fragment = (LocalFileExplorerFragment) fileExplorerPagerAdapter.getItem(0);
        fragment.setListAdapter(adapter);
    }
}
