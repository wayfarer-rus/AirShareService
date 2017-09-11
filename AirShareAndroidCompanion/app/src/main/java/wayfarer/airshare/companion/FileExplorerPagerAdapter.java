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

        switch (position) {
            case 0: title = "Local files";
                break;
            case 1: title = "Remote files";
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

    /**
     * A dummy fragment representing a section of the app, but that simply displays dummy text.
     */
    public static class DemoObjectFragment extends Fragment {

        public static final String ARG_OBJECT = "object";

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_collection_object, container, false);
            Bundle args = getArguments();
            ((TextView) rootView.findViewById(android.R.id.text1)).setText(
                    Integer.toString(args.getInt(ARG_OBJECT)));
            return rootView;
        }
    }
}
