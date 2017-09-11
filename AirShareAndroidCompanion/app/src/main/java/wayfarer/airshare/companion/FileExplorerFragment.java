package wayfarer.airshare.companion;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import org.jetbrains.annotations.NotNull;

/**
 * Created by wayfarer on 03/09/2017.
 */

abstract class FileExplorerFragment extends ListFragment
        implements GestureDetector.OnDoubleTapListener
{
    private static final String TAG = "FExplorer_Fragment";

    protected AirShareActivity parentActivity;
    private View progressOverlay;

    public void setParentActivity(AirShareActivity parentActivity) {
        this.parentActivity = parentActivity;
    }

    @NotNull
    public AirShareActivity getParentActivity() {
        return parentActivity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_file_explorer, container, false);
        progressOverlay = v.findViewById(R.id.progress_overlay);
        return v;
    }

    @Override
    public abstract boolean onSingleTapConfirmed(MotionEvent motionEvent);

    @Override
    public abstract boolean onDoubleTap(MotionEvent motionEvent);

    protected void setSelection(Item chosenFile, View v, boolean selection) {
        chosenFile.setSelected(selection);
        v.setSelected(selection);
        int c = ResourcesCompat.getColor(getResources(),
                selection?R.color.selectedItemColor:R.color.defaultItemColor,
                null);
        v.setBackgroundColor(c);
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent motionEvent) {
        Log.d(TAG, "Double tap event");
        return true;
    }

    public void showProgressBar() {
        Log.d(TAG, "showProgressBar!!!");
        MainActivity.animateView(progressOverlay, View.VISIBLE, 0.4f, 200);
        getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    public void hideProgressBar() {
        Log.d(TAG, "hideProgressBar!!!");
        MainActivity.animateView(progressOverlay, View.GONE, 0, 200);
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

}
