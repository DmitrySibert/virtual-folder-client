package folder.file.system;

import info.smart_tools.smartactors.core.*;
import info.smart_tools.smartactors.core.actors.Actor;
import info.smart_tools.smartactors.core.actors.annotations.Handler;
import info.smart_tools.smartactors.core.addressing.MessageMapId;
import info.smart_tools.smartactors.core.addressing.maps.MessageMap;

import java.io.File;
import java.util.UUID;

/**
 * Actor controls application system folder
 */
//TODO: Probably need to subfolders name according particular strategy which resolves by IOC
public class FileSystemManagerActor extends Actor {

    private String sysFolderPath;
    private Integer curFolderOrder;
    private String curFolderName;
    private Integer maxFileCounter;
    private Integer fileCounter;
    private String getFolderWithCreateMmId;
    private Field<String> storageFolderF;



    public FileSystemManagerActor(IObject params) {

        try {
            sysFolderPath = (new Field<String>(new FieldName("sysFolderPath"))).from(params, String.class);
            maxFileCounter = (new Field<Integer>(new FieldName("maxFileCounter"))).from(params, Integer.class);
            getFolderWithCreateMmId = (new Field<String>(new FieldName("getFolderWithCreateMmId"))).from(params, String.class);
            storageFolderF = new Field<>(new FieldName("storageFolder"));
        } catch (ReadValueException | ChangeValueException e) {
            System.out.println("An error occurred while constructing FileSystemManagerActor: " + e);
        }

        try {
            File file = new File(sysFolderPath);
            if (!file.exists()) {
                if (!file.mkdir()) {
                    String errStr = "Cannot open\\create application's system folder.";
                    System.out.println(errStr);
                    throw new RuntimeException(errStr);
                }
            }

            File[] subFolders = file.listFiles(File::isDirectory);
            File curFolder;
            if (subFolders.length > 0) {
                curFolder = subFolders[0];
                curFolderName = subFolders[0].getName();
                Integer maxOrderNum = getFolderOrderNum(curFolderName);
                for (File folder : subFolders) {
                    Integer curFolderOrder = getFolderOrderNum(folder.getName());
                    if (curFolderOrder > maxOrderNum) {
                        curFolder = folder;
                        curFolderName = subFolders[0].getName();
                        maxOrderNum = curFolderOrder;
                    }
                }
            } else {
                curFolderName = "1_" + UUID.randomUUID();
                curFolder = new File(sysFolderPath + "\\" + curFolderName);
                if (!curFolder.mkdir()) {
                    String errStr = "Cannot create application's system folder's subfolder.";
                    System.out.println(errStr);
                    throw new RuntimeException(errStr);
                }
            }
            curFolderOrder = getFolderOrderNum(curFolder.getName());
            fileCounter = curFolder.listFiles().length;
        } catch (Exception e) {
            System.out.println(e.toString());
            throw new RuntimeException(e);
        }
    }

    private Integer getFolderOrderNum(String folderName) {

        String[] nameToken = folderName.split("_");
        return Integer.parseInt(nameToken[0]);
    }

    @Handler("createSubfolder")
    public void createSubfolder(IMessage msg) {
        curFolderOrder++;
        fileCounter = 0;
        curFolderName = curFolderOrder + "_" + UUID.randomUUID();
        File newFolder = new File(sysFolderPath + "\\" + curFolderName);
        //TODO: handle error while creating new folder
        Boolean res = newFolder.mkdir();
    }

    private Field<MessageMap> mmF = new Field<>(new FieldName("messageMap"));

    @Handler("getCurrentFolder")
    public void getCurrentFolder(IMessage msg) throws ReadValueException, ChangeValueException {

        if (fileCounter >= maxFileCounter) {
            mmF.from(msg, MessageMap.class).insertMessageMapId(MessageMapId.fromString(getFolderWithCreateMmId));
        } else {
            fileCounter++;
            storageFolderF.inject(msg, sysFolderPath + "\\" + curFolderName);
        }
    }
}
