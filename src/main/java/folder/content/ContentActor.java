package folder.content;

import folder.content.util.FolderItem;
import info.smart_tools.smartactors.core.*;
import info.smart_tools.smartactors.core.actors.Actor;
import info.smart_tools.smartactors.core.actors.annotations.Handler;

import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public class ContentActor extends Actor {

    private ListField<FolderItem> folderContentF;

    public ContentActor(IObject params) {
        folderContentF = new ListField<FolderItem>(new FieldName("folderContent"));
    }

    @Handler("getContentByPath")
    public void getContentByPath(IMessage msg) throws ChangeValueException {

        List<FolderItem> content = new LinkedList<FolderItem>();
        content.add(new FolderItem(false, "waybills.xml"));
        content.add(new FolderItem(true, "Documents"));
        content.add(new FolderItem(true, "Reports"));

        respondOn(msg, response -> {
            folderContentF.inject(response, content);
        });
    }
}
