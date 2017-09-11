package wayfarer.airshare.companion;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.io.File;

/**
 * Created by wayfarer on 03/09/2017.
 */

public class LocalFileExplorerFragment extends FileExplorerFragment
{
    private static final String TAG = "LFExplorer_Fragment";

    @Override
    public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
        Log.d(TAG, "Single tap confirmed");
        int position = getListView().pointToPosition((int) motionEvent.getX(), (int) motionEvent.getY());
        Item chosenFile = (Item) getListView().getItemAtPosition(position);
        View v = getListView().getChildAt(position - getListView().getFirstVisiblePosition());

        if (chosenFile == null || v == null) {
            return false;
        }

        // get current state
        FileExplorerState state = FileExplorerState.Companion.getInstance();
        File sel = new File(state.getPath() + "/" + chosenFile);

        if (chosenFile.getFile().equalsIgnoreCase("up") && !sel.exists()) {
            return false;
        }
        else if (chosenFile.isSelected()) {
            setSelection(chosenFile, v, false);
        }
        else {
            setSelection(chosenFile, v, true);
        }

        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent motionEvent) {
        Log.d(TAG, "Double tap");

        int position = getListView().pointToPosition((int) motionEvent.getX(), (int) motionEvent.getY());
        Item chosenFile = (Item) getListView().getItemAtPosition(position);

        if (chosenFile == null) {
            return false;
        }

        // get current state
        FileExplorerState state = FileExplorerState.Companion.getInstance();
        File sel = new File(state.getPath() + "/" + chosenFile);

        if (sel.isDirectory()) {
            // Adds chosen directory to list
            state.pushToHistory(chosenFile.getFile());

            state.setFirstLevel(false);
            state.setPath(new File(sel + ""));
            parentActivity.loadLocalFileList();

            Log.d(TAG, state.getPath().getAbsolutePath());
        }
        // Checks if 'up' was clicked
        else if (chosenFile.getFile().equalsIgnoreCase("up") && !sel.exists()) {
            // present directory removed from list
            String s = state.popFromHistory();

            // path modified to exclude present directory
            state.setPath(new File(state.getPath().toString().substring(0,
                    state.getPath().toString().lastIndexOf(s))));

            // if there are no more directories in the list, then
            // its the first level
            if (state.isEmptyHistory()) {
                state.setFirstLevel(true);
            }

            parentActivity.loadLocalFileList();

            Log.d(TAG, state.getPath().getAbsolutePath());
        }

        return true;
    }
}
