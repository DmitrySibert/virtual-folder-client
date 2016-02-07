package folder.upload;

import info.smart_tools.smartactors.core.*;
import info.smart_tools.smartactors.core.actors.Actor;
import info.smart_tools.smartactors.core.actors.annotations.Handler;

import java.util.UUID;

/**
 * Actor responses for action directed on uploading files on server
 */
public class UploadActor extends Actor {

    private Field<String> relativePathF;
    private Field<String> storageFolderF;
    private Field<String> filenameF;
    private Field<String> srcF;
    private Field<String> destF;


    public UploadActor(IObject params) {

        relativePathF = new Field<>(new FieldName("relativePath"));
        storageFolderF = new Field<>(new FieldName("storageFolder"));
        filenameF = new Field<>(new FieldName("filename"));
        srcF = new Field<>(new FieldName("src"));
        destF = new Field<>(new FieldName("dest"));
    }

    @Handler("prepareForLocalStorage")
    public void prepareForLocalStorage(IMessage msg) throws ReadValueException, ChangeValueException {

        String filename = storageFolderF.from(msg, String.class) + UUID.randomUUID();
        srcF.inject(msg, relativePathF.from(msg, String.class));
        destF.inject(msg, filename);
    }
}
