package folder.upload;

import folder.http.PostRequestFields;
import folder.util.FileInfoFields;
import info.smart_tools.smartactors.core.*;
import info.smart_tools.smartactors.core.actors.Actor;
import info.smart_tools.smartactors.core.actors.annotations.Handler;
import info.smart_tools.smartactors.core.impl.SMObject;
import info.smart_tools.smartactors.utils.ioc.IOC;

import java.io.File;
import java.util.UUID;

/**
 * Actor responses for action directed on uploading files on server
 */
public class UploadActor extends Actor {

    /** Относительный путь внутри виртуальной директории, куда перемещается файл */
    private Field<String> folderDestPathF;
    /** Расположение обрабатываемого файла на компьютере */
    private Field<String> filePathF;
    /** Тут  путь к системной директории приложения в которой на клиенте хранятся все файлы */
    private Field<String> storageFolderF;
    /** Настоящее имя файла*/
    private Field<String> fileOriginNameF;
    /** Имя, присвоенное файлу, для хранения в системной директории приложения */
    private Field<String> fileIdF;
    //TODO: Если вдруг, что мало вероятно, почти нереально, что файлы будут больше чем (2 гбайт - 1 байт)
    //TODO: то придется переходить на размер Long и переписывать некоторые куски приложения
    //TODO: не думаю, что кто-то будет использовать это приложение для таких больших файлов

    private Field<String> srcF;
    private Field<String> destF;
    /** Поле для имени коллекции для сохранения */
    private Field<String> collectionNameF;
    /** Поле с данными для сохранение*/
    private Field<IObject> insertDataF;
    private Field<Boolean> statusF = new Field<>(new FieldName("status"));

    private String fileInfoCollectionName;

    public UploadActor(IObject params) {

        try {
            fileInfoCollectionName = new Field<String>(new FieldName("fileInfoCollectionName")).from(params, String.class);
        } catch (ReadValueException | ChangeValueException e) {
            String errMsg = "An error occurred while constructing UploadActor: " + e;
            System.out.println(errMsg);
            throw new RuntimeException(errMsg);
        }
        filePathF = new Field<>(new FieldName("filePath"));
        storageFolderF = new Field<>(new FieldName("storageFolder"));
        srcF = new Field<>(new FieldName("src"));
        destF = new Field<>(new FieldName("dest"));
        folderDestPathF = new Field<>(new FieldName("folderDestPath"));
        fileOriginNameF = new Field<>(new FieldName("fileOriginName"));
        fileIdF = new Field<>(new FieldName("fileId"));
        collectionNameF = new Field<>(new FieldName("collectionName"));
        insertDataF = new Field<>(new FieldName("insertData"));
    }

    /**
     * Генерирует  необходимые для локального и удаленного храрения данные и записывает их в сообщение
     * @param msg содержит filePath - местоположение файла на локальной машине
     *                     filePathAbs - местоположение копии файла для работы приложения
     *                     storageFolderF - директория для хранения файлов внутри системной директории
     * @throws ReadValueException
     * @throws ChangeValueException
     */
    @Handler("prepareFileMetadata")
    public void prepareFileMetadata(IMessage msg) throws ReadValueException, ChangeValueException {

        String originFilePath = filePathF.from(msg, String.class);
        String[] filePathTokens = originFilePath.split("\\\\");
        String fileOriginName = filePathTokens[filePathTokens.length - 1];
        fileOriginNameF.inject(msg, fileOriginName);
        FileInfoFields.PHYSIC_PATH.inject(
                msg, storageFolderF.from(msg, String.class) + "\\" + UUID.randomUUID().toString()
        );
        FileInfoFields.LOGIC_PATH.inject(msg, FileInfoFields.LOGIC_PATH.from(msg, String.class) + "\\" + fileOriginName);
        File file = new File(originFilePath);
        FileInfoFields.FILE_SIZE.inject(msg, Long.valueOf(file.length()).intValue());
        respondOn(msg, response -> {
            statusF.inject(response, Boolean.TRUE);
        });
    }

    /**
     * Записать абсолютный путь источника и целевого файла для хранения в системной директории
     * @param msg содержит filePath - местоположение файла на локальной машине
     *                     filePathAbs - местоположение копии файла для работы приложения
     * @throws ReadValueException
     * @throws ChangeValueException
     */
    @Handler("prepareInfoForCopy")
    public void prepareInfoForCopy(IMessage msg) throws ReadValueException, ChangeValueException {

        String filePathAbs = FileInfoFields.PHYSIC_PATH.from(msg, String.class);
        srcF.inject(msg, filePathF.from(msg, String.class));
        destF.inject(msg, filePathAbs);
    }

    /**
     * Готовит начальные данные о файле для контроля загрузки на удаленный сервер
     * @param msg
     * @throws ReadValueException
     * @throws ChangeValueException
     */
    @Handler("prepareFileInfo")
    public void prepareFileInfo(IMessage msg) throws ReadValueException, ChangeValueException {

        IObject fileInfo = IOC.resolve(IObject.class);
        FileInfoFields.SENT_PARTS.inject(fileInfo, 0);
        FileInfoFields.IS_SENT.inject(fileInfo, Boolean.FALSE);
        FileInfoFields.FILE_SIZE.inject(fileInfo, FileInfoFields.FILE_SIZE.from(msg, Integer.class));
        FileInfoFields.PHYSIC_PATH.inject(fileInfo, FileInfoFields.PHYSIC_PATH.from(msg, String.class));
        msg.setValue(new FieldName(FileInfoFields.LOGIC_PATH.from(msg, String.class).replace('\\', '_')), fileInfo);
    }

    /**
     * Поместить информацию о файле в объект для последующего соханения в БД
     * @param msg содержит по ключу-идентификатору файла информацию о файле для сохранения в БД
     * @throws ReadValueException
     * @throws ChangeValueException
     */
    @Handler("markFileInfoForStorage")
    public void markFileInfoForStorage(IMessage msg) throws ReadValueException, ChangeValueException {

        IObject insertObj = new SMObject();
        //TODO:Сделать служебным мутабельным именем
        FieldName filePathFN = new FieldName(FileInfoFields.LOGIC_PATH.from(msg, String.class).replace('\\', '_'));
        IObject fileInfo = (IObject) msg.getValue(filePathFN);
        insertObj.setValue(filePathFN, fileInfo);
        collectionNameF.inject(msg, fileInfoCollectionName);

        insertDataF.inject(msg, insertObj);
    }

    /**
     * Упаковать информацию о файле для отправки на удаленный сервер
     * @param msg содержит logicPath - логический путь в директории(включая имя файла)
     *                     fileSize - размер файла
     * @throws ReadValueException
     * @throws ChangeValueException
     */
    @Handler("formInfoForServer")
    public void formInfoForServer(IMessage msg) throws ReadValueException, ChangeValueException {

        IObject info = IOC.resolve(IObject.class);
        FileInfoFields.FILE_SIZE.inject(info, FileInfoFields.FILE_SIZE.from(msg, Integer.class));
        fileIdF.inject(info, FileInfoFields.LOGIC_PATH.from(msg, String.class));
        PostRequestFields.POST_REQUEST_DATA.inject(msg, info);
        PostRequestFields.REMOTE_MSG_MAP.inject(msg, "initFileReceivingMm");
    }

    /**
     * Извлечь данные для загрузки файла на сервер, присланные сервером
     * @param msg содержит postResponseData - идентификатор файла, guid выданный сервером, количество кусков, размер куска
     * @throws ReadValueException
     * @throws ChangeValueException
     */
    @Handler("handleUploadInfoFromServer")
    public void handleUploadInfoFromServer(IMessage msg) throws ReadValueException, ChangeValueException {

        IObject data = PostRequestFields.POST_RESPONSE_DATA.from(msg, IObject.class);
        fileIdF.inject(msg, fileIdF.from(data, String.class));
        FileInfoFields.SERVER_GUID.inject(msg, FileInfoFields.SERVER_GUID.from(data, String.class));
        FileInfoFields.PART_SIZE.inject(msg, FileInfoFields.PART_SIZE.from(data, Integer.class));
        FileInfoFields.PARTS_QUANTITY.inject(msg, FileInfoFields.PARTS_QUANTITY.from(data, Integer.class));
    }

    /**
     * Добавить в информацию о файле данные для загрузки на сервер, присланные сервером
     * @param msg
     * @throws ReadValueException
     * @throws ChangeValueException
     */
    @Handler("prepareUploadInfoForStorage")
    public void prepareUploadInfoForStorage(IMessage msg) throws ReadValueException, ChangeValueException {

        FieldName filePathFN = new FieldName(FileInfoFields.LOGIC_PATH.from(msg, String.class).replace('\\', '_'));
        IObject fileInfo = (IObject) msg.getValue(filePathFN);
        FileInfoFields.SERVER_GUID.inject(fileInfo, FileInfoFields.SERVER_GUID.from(msg, String.class));
        FileInfoFields.PART_SIZE.inject(fileInfo, FileInfoFields.PART_SIZE.from(msg, Integer.class));
        FileInfoFields.PARTS_QUANTITY.inject(fileInfo, FileInfoFields.PARTS_QUANTITY.from(msg, Integer.class));
    }

    @Handler("finishUpload")
    public void finishUpload(IMessage msg) throws ReadValueException, ChangeValueException {
        //TODO:Сделать служебным мутабельным именем
        FieldName filePathFN = new FieldName(FileInfoFields.LOGIC_PATH.from(msg, String.class).replace('\\', '_'));
        IObject fileInfo = (IObject) msg.getValue(filePathFN);
        FileInfoFields.IS_SENT.inject(fileInfo, Boolean.TRUE);
    }
}
