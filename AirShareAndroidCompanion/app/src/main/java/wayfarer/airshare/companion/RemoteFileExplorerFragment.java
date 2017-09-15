package wayfarer.airshare.companion;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by wayfarer on 07/09/2017.
 */

public class RemoteFileExplorerFragment extends FileExplorerFragment {
    private static final String TAG = "RFExplorer_Fragment";

    @Override
    public boolean onDoubleTap(MotionEvent motionEvent) {
        int position = getListView().pointToPosition((int) motionEvent.getX(), (int) motionEvent.getY());
        Item chosenFile = (Item) getListView().getItemAtPosition(position);

        if (chosenFile == null) {
            return false;
        }

        // get current state
        FileExplorerState state = FileExplorerState.Companion.getInstance();
        String selection = state.getRemotePath() + "/" + chosenFile;

        if (chosenFile.isFolder()) {
            state.pushToRemoteHistory(chosenFile.getFile());
            state.setRemoteIsRoot(false);
            state.setRemotePath(selection);
            parentActivity.loadRemoteFileList();
        }
        else if (!chosenFile.isFile()) { // meaning it is an UP action
            String s = state.popFromRemoteHistory();

            // path modified to exclude present directory
            state.setRemotePath(state.getRemotePath().substring(0,
                    state.getRemotePath().lastIndexOf(s)));

            // if there are no more directories in the list, then
            // its the first level
            if (state.isEmptyRemoteHistory()) {
                state.setRemoteIsRoot(true);
            }

            parentActivity.loadRemoteFileList();
        }

        return true;
    }
}
