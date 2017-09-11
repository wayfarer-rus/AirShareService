package wayfarer.airshare.companion;

/**
 * Created by wayfarer on 03/09/2017.
 */

public class Item {
    private Type type;

    public void setType(Type type) {
        this.type = type;
    }

    public boolean isFolder() {
        return this.type.equals(Type.FOLDER);
    }

    public boolean isFile() {
        return this.type.equals(Type.FILE);
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        FOLDER,
        FILE,
        UP
    }
    private String file;
    private int icon;
    private boolean selected;

    Item(String file, Integer icon, Type type) {
        this.file = file;
        this.icon = icon;
        this.type = type;
    }

    @Override
    public String toString() {
        return file;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public String getFile() {
        return file;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

}
