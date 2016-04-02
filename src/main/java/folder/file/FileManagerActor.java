package folder.file;

import folder.util.FileInfoFields;
import info.smart_tools.smartactors.core.*;
import info.smart_tools.smartactors.core.actors.Actor;
import info.smart_tools.smartactors.core.actors.annotations.Handler;

import java.io.*;
import java.nio.channels.FileChannel;

/**
 * Actor for actions with files
 */
public class FileManagerActor extends Actor {

    private Field<String> srcF;
    private Field<String> destF;

    public FileManagerActor(IObject params) {
        srcF = new Field<>(new FieldName("src"));
        destF = new Field<>(new FieldName("dest"));
    }

    @Handler("copyFile")
    public void copyFile(IMessage msg) throws ReadValueException, ChangeValueException {

        String srcPath = srcF.from(msg, String.class);
        String destPath = destF.from(msg, String.class);

        File src = new File(srcPath);
        File dest = new File(destPath);

        try {
            try (FileChannel sourceChannel = new FileInputStream(src).getChannel();
                 FileChannel destChannel = new FileOutputStream(dest).getChannel()
            ) {
                destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
            }
        } catch (IOException e) {
            String errStr = "An error occurred while copying file: " + e;
            System.out.println(errStr);
        }
    }

    @Handler("createFileWithSize")
    public void createFileWithSize(IMessage msg) throws ReadValueException, ChangeValueException {
        try {
            RandomAccessFile f = new RandomAccessFile(FileInfoFields.PHYSIC_PATH.from(msg, String.class), "rw");
            f.setLength(FileInfoFields.FILE_SIZE.from(msg, Integer.class));
        } catch (Exception e) {
            String errStr = "An error occurred while creating file: " + e;
            System.out.println(errStr);
        }
    }
}
