package folder.content.util;

/**
 *  A folder item
 */
public class FolderItem {

    private boolean isFolder;
    private String name;

    public FolderItem(boolean isFolder, String name) {
        this.isFolder = isFolder;
        this.name = name;
    }

    public boolean getIsFolder() {
        return isFolder;
    }

    public String getName() {
        return name;
    }

}
