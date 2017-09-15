package wayfarer.airshare.companion;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GestureDetectorCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by wayfarer on 07/09/2017.
 */

public class FileExplorerPagerAdapter extends FragmentPagerAdapter {
    private final LocalFileExplorerFragment localFileExplorerFragment;
    private final RemoteFileExplorerFragment remoteFileExplorerFragment;
    private GestureDetectorCompat mLocalTouchDetector;
    private GestureDetectorCompat mRemoteTouchDetector;

    public FileExplorerPagerAdapter(AirShareActivity airShareActivity, FragmentManager fm) {
        super(fm);
        // Create local File Explorer Fragment
        localFileExplorerFragment = new LocalFileExplorerFragment();
        mLocalTouchDetector = new GestureDetectorCompat(airShareActivity, new MyGestureListener());
        mLocalTouchDetector.setOnDoubleTapListener(localFileExplorerFragment);
        localFileExplorerFragment.setParentActivity(airShareActivity);

        // Create remote File Explorer Fragment
        remoteFileExplorerFragment = new RemoteFileExplorerFragment();
        mRemoteTouchDetector = new GestureDetectorCompat(airShareActivity, new MyGestureListener());
        mRemoteTouchDetector.setOnDoubleTapListener(remoteFileExplorerFragment);
        remoteFileExplorerFragment.setParentActivity(airShareActivity);
    }

    @Override
    public Fragment getItem(int i) {
        Fragment fragment = null;
        switch (i) {
            case 0:
                // Local files
                fragment = localFileExplorerFragment;
                break;
            case 1:
                // Remote files
                fragment = remoteFileExplorerFragment;
                break;
        }

        return fragment;
    }

    @Override
    public int getCount() {
        // Two pages: Local and Remote
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        String title = "";
        FileExplorerState state = FileExplorerState.Companion.getInstance();

        switch (position) {
            case 0: title = "Local: " + state.getPath().toString();
                break;
            case 1: title = "Remote: " + state.getRemotePath();
                break;
        }

        return title;
    }

    public GestureDetectorCompat getLocalTouchDetector() {
        return mLocalTouchDetector;
    }

    public GestureDetectorCompat getRemoteTouchDetector() {
        return mRemoteTouchDetector;
    }

}
