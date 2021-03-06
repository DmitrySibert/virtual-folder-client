package folder.download;

import com.google.common.primitives.Bytes;
import folder.encode.EncodeFields;
import folder.http.PostRequestFields;
import folder.util.FileInfoFields;
import info.smart_tools.smartactors.core.*;
import info.smart_tools.smartactors.core.actors.Actor;
import info.smart_tools.smartactors.core.actors.annotations.Handler;
import info.smart_tools.smartactors.core.addressing.AddressingFields;
import info.smart_tools.smartactors.core.addressing.MessageMapId;
import info.smart_tools.smartactors.core.addressing.maps.IMessageMapSource;
import info.smart_tools.smartactors.core.addressing.maps.MessageMap;
import info.smart_tools.smartactors.core.routers.MessageBus;
import info.smart_tools.smartactors.utils.ioc.IOC;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.UUID;

/**
 * Актор занимается загрузкой файлов с сервера
 */
public class DownloadActor extends Actor {

    private ListField<IObject> filesF;
    private Field<Integer> partNumberF;
    private Field<String> filePartF;
    private Field<String> storageFolderF;

    private Field<IObject> fileInfoF;
    private MutableFieldName fileInfoFN;

    private String filesInfoMm;
    private String initDownloadFileMm;
    private String downloadFileMm;
    private String downloadFolderMm;
    private String getFilePartMm;
    private String finishFileDownloadMm;

    public DownloadActor(IObject params) {

        filesF = new ListField<>(new FieldName("files"));
        partNumberF = new Field<>(new FieldName("partNumber"));
        filePartF = new Field<>(new FieldName("filePart"));
        storageFolderF = new Field<>(new FieldName("storageFolder"));
        fileInfoFN = new MutableFieldName("def");
        fileInfoF = new Field<>(fileInfoFN);
        try {
            filesInfoMm = new Field<String>(new FieldName("filesInfoMm")).from(params, String.class);
            initDownloadFileMm = new Field<String>(new FieldName("initDownloadFileMm")).from(params, String.class);
            downloadFileMm = new Field<String>(new FieldName("downloadFileMm")).from(params, String.class);
            downloadFolderMm = new Field<String>(new FieldName("downloadFolderMm")).from(params, String.class);
            getFilePartMm = new Field<String>(new FieldName("getFilePartMm")).from(params, String.class);
            finishFileDownloadMm = new Field<String>(new FieldName("finishFileDownloadMm")).from(params, String.class);
        } catch (ReadValueException | ChangeValueException e) {
            String errMsg = "An error occurred while constructing DownloadActor: " + e;
            System.out.println(errMsg);
            throw new RuntimeException(errMsg);
        }
    }

    /**
     * Сформировать запрос на сервер, для получения полной информации о файлах
     * @param msg содержит files - список файлов, с serverGuid и logicPath
     */
    @Handler("formFilesInfoRequest")
    public void formFilesInfoRequest(IMessage msg) throws ChangeValueException, ReadValueException {

        IObject postData = IOC.resolve(IObject.class);
        filesF.inject(postData, filesF.from(msg, IObject.class));
        PostRequestFields.POST_REQUEST_DATA.inject(msg, postData);
        PostRequestFields.REMOTE_MSG_MAP.inject(msg, filesInfoMm);
    }

    /**
     * Запустить процедуру закачки файлов с сервера, на основе его ответа
     * @param msg
     * @throws ReadValueException
     * @throws ChangeValueException
     */
    @Handler("handleDownloadInfoFromServer")
    public void handleDownloadInfoFromServer(IMessage msg) throws ReadValueException, ChangeValueException {

        IObject data = PostRequestFields.POST_RESPONSE_DATA.from(msg, IObject.class);
        for (IObject file : filesF.from(data, IObject.class)) {
            IMessage dwnldMsg = new Message(IOC.resolve(IObject.class));
            IObject addrF = IOC.resolve(IObject.class);
            if (FileInfoFields.IS_FOLDER.from(file, Boolean.class)) {
                AddressingFields.MESSAGE_MAP_ID_FIELD.inject(addrF, MessageMapId.fromString(downloadFolderMm));
                AddressingFields.ADDRESS_FIELD.inject(dwnldMsg, addrF);
            } else {
                AddressingFields.MESSAGE_MAP_ID_FIELD.inject(addrF, MessageMapId.fromString(initDownloadFileMm));
                AddressingFields.ADDRESS_FIELD.inject(dwnldMsg, addrF);
            }
            FileInfoFields.LOGIC_PATH.inject(
                    dwnldMsg, FileInfoFields.LOGIC_PATH.from(file, String.class) +
                    FileInfoFields.ORIGINAL_NAME.from(file, String.class)
            );
            FileInfoFields.SERVER_GUID.inject(dwnldMsg, FileInfoFields.SERVER_GUID.from(file, String.class));
            FileInfoFields.PARTS_QUANTITY.inject(dwnldMsg, FileInfoFields.PARTS_QUANTITY.from(file, Integer.class));
            FileInfoFields.PART_SIZE.inject(dwnldMsg, FileInfoFields.PART_SIZE.from(file, Integer.class));
            FileInfoFields.FILE_SIZE.inject(dwnldMsg, FileInfoFields.FILE_SIZE.from(file, Integer.class));
            FileInfoFields.IS_FOLDER.inject(dwnldMsg, FileInfoFields.IS_FOLDER.from(file, Boolean.class));
            MessageBus.send(dwnldMsg);
        }
    }

    /**
     * Формируем данные для нового файла под закачку
     * @param msg
     */
    @Handler("prepareFileInfo")
    public void prepareFileInfo(IMessage msg) throws ChangeValueException, ReadValueException {

        IObject fileInfo = IOC.resolve(IObject.class);
        FileInfoFields.DOWNLOAD_PARTS.inject(fileInfo, 0);
        FileInfoFields.PARTS_QUANTITY.inject(fileInfo, FileInfoFields.PARTS_QUANTITY.from(msg, Integer.class));
        FileInfoFields.PART_SIZE.inject(fileInfo, FileInfoFields.PART_SIZE.from(msg, Integer.class));
        FileInfoFields.FILE_SIZE.inject(fileInfo, FileInfoFields.FILE_SIZE.from(msg, Integer.class));
        FileInfoFields.IS_FOLDER.inject(fileInfo, FileInfoFields.IS_FOLDER.from(msg, Boolean.class));
        FileInfoFields.SERVER_GUID.inject(fileInfo, FileInfoFields.SERVER_GUID.from(msg, String.class));
        FileInfoFields.ACTIVE.inject(fileInfo, Boolean.FALSE);
        String filePhysicPath = storageFolderF.from(msg, String.class) + "\\" + UUID.randomUUID().toString();
        FileInfoFields.PHYSIC_PATH.inject(fileInfo, filePhysicPath);
        FileInfoFields.PHYSIC_PATH.inject(msg, filePhysicPath);
        fileInfoFN.setName(FileInfoFields.LOGIC_PATH.from(msg, String.class).replace('\\', '_'));
        fileInfoF.inject(msg, fileInfo);
    }

    /**
     * Формируем данные для директории
     * @param msg
     */
    @Handler("prepareFolderInfo")
    public void prepareFolderInfo(IMessage msg) throws ChangeValueException, ReadValueException {

        IObject fileInfo = IOC.resolve(IObject.class);
        FileInfoFields.SERVER_GUID.inject(fileInfo, FileInfoFields.SERVER_GUID.from(msg, String.class));
        FileInfoFields.IS_FOLDER.inject(fileInfo, FileInfoFields.IS_FOLDER.from(msg, Boolean.class));
        FileInfoFields.ACTIVE.inject(fileInfo, Boolean.TRUE);
        fileInfoFN.setName(FileInfoFields.LOGIC_PATH.from(msg, String.class).replace("\\", "_"));
        fileInfoF.inject(msg, fileInfo);
    }

    @Handler("initFileDownload")
    public void initFileDownload(IMessage msg) throws ChangeValueException, DeleteValueException, ReadValueException {

        IMessage dwnldFileMsg = new Message(IOC.resolve(IObject.class));
        IObject addrF = IOC.resolve(IObject.class);
        AddressingFields.MESSAGE_MAP_ID_FIELD.inject(addrF, MessageMapId.fromString(downloadFileMm));
        AddressingFields.ADDRESS_FIELD.inject(dwnldFileMsg, addrF);
        fileInfoFN.setName(FileInfoFields.LOGIC_PATH.from(msg, String.class).replace('\\', '_'));
        FileInfoFields.LOGIC_PATH.inject(dwnldFileMsg, FileInfoFields.LOGIC_PATH.from(msg, String.class));
        fileInfoF.inject(dwnldFileMsg, fileInfoF.from(msg, IObject.class));
        MessageBus.send(dwnldFileMsg);
    }

    @Handler("formFilePartRequest")
    public void formFilePartRequest(IMessage msg) throws ReadValueException, ChangeValueException {

        fileInfoFN.setName(FileInfoFields.LOGIC_PATH.from(msg, String.class).replace('\\', '_'));
        IObject fileInfo = fileInfoF.from(msg, IObject.class);
        IObject post = IOC.resolve(IObject.class);
        FileInfoFields.SERVER_GUID.inject(post, FileInfoFields.SERVER_GUID.from(fileInfo, String.class));
        partNumberF.inject(post, FileInfoFields.DOWNLOAD_PARTS.from(fileInfo, Integer.class) + 1);
        PostRequestFields.REMOTE_MSG_MAP.inject(msg, getFilePartMm);
        PostRequestFields.POST_REQUEST_DATA.inject(msg, post);
    }

    @Handler("handleFilePartReceiving")
    public void handleFilePartReceiving(IMessage msg) throws ReadValueException, ChangeValueException {

        IObject data = PostRequestFields.POST_RESPONSE_DATA.from(msg, IObject.class);
        EncodeFields.DECODE_TARGET.inject(msg, filePartF.from(data, String.class));
    }

    @Handler("saveFilePart")
    public void saveFilePart(IMessage msg) throws ReadValueException, ChangeValueException, IOException {

        fileInfoFN.setName(FileInfoFields.LOGIC_PATH.from(msg, String.class).replace('\\', '_'));
        IObject fileInfo = fileInfoF.from(msg, IObject.class);
        try {
            RandomAccessFile f = new RandomAccessFile(FileInfoFields.PHYSIC_PATH.from(fileInfo, String.class), "rw");
            List<Byte> part = EncodeFields.DECODE_RESULT.from(msg, Byte.class);
            f.skipBytes(FileInfoFields.DOWNLOAD_PARTS.from(fileInfo, Integer.class) * FileInfoFields.PART_SIZE.from(fileInfo, Integer.class));
            f.write(Bytes.toArray(part));
            f.close();
        } catch (IOException e) {
            int k = 10;
            //TODO:даже и не знаю че тут можно сделать
        }
        FileInfoFields.DOWNLOAD_PARTS.inject(fileInfo, FileInfoFields.DOWNLOAD_PARTS.from(fileInfo, Integer.class) + 1);
    }

    @Handler("checkEnding")
    public void checkEnding(IMessage msg) throws ReadValueException, ChangeValueException, DeleteValueException {

        fileInfoFN.setName(FileInfoFields.LOGIC_PATH.from(msg, String.class).replace('\\', '_'));
        IObject fileInfo = fileInfoF.from(msg, IObject.class);
        if(!FileInfoFields.DOWNLOAD_PARTS.from(fileInfo, Integer.class).equals(
                FileInfoFields.PARTS_QUANTITY.from(fileInfo, Integer.class))
        ) {
            AddressingFields.MESSAGE_MAP_FIELD.from(msg, MessageMap.class).toStart();
        } else {
            FileInfoFields.ACTIVE.inject(fileInfo, Boolean.TRUE);
            AddressingFields.MESSAGE_MAP_FIELD.from(msg, MessageMap.class).insertMessageMapId(MessageMapId.fromString(finishFileDownloadMm));
        }
    }
}
