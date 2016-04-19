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
 * Actor responses for action directed on uploading folders
 */
public class FolderUploadActor extends Actor {

    /** Расположение обрабатываемого файла на компьютере */
    private Field<String> filePathF;
    /** Настоящее имя файла*/
    private Field<String> fileOriginNameF;
    /** Имя, присвоенное файлу, для хранения в системной директории приложения */
    private Field<String> fileIdF;

    /** Поле для имени коллекции для сохранения */
    private Field<String> collectionNameF;
    /** Поле с данными для сохранение*/
    private Field<IObject> insertDataF;

    private String fileInfoCollectionName;
    private String createFolderServerMm;

    public FolderUploadActor(IObject params) {

        try {
            fileInfoCollectionName = new Field<String>(new FieldName("fileInfoCollectionName")).from(params, String.class);
            createFolderServerMm = new Field<String>(new FieldName("createFolderServerMm")).from(params, String.class);
        } catch (ReadValueException | ChangeValueException e) {
            String errMsg = "An error occurred while constructing FileUploadActor: " + e;
            System.out.println(errMsg);
            throw new RuntimeException(errMsg);
        }
        filePathF = new Field<>(new FieldName("filePath"));
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

        //TODO: необходимо подумать о том, надо ли обрабатывать все внутренности
        //TODO: директории, если ее кинули драг.н.дропом
        //FileInfoFields.ORIGINAL_NAME.inject(msg, FileInfoFields.ORIGINAL_NAME.from(msg, String.class));
        //FileInfoFields.LOGIC_PATH.inject(msg, FileInfoFields.LOGIC_PATH.from(msg, String.class));
        FileInfoFields.LOGIC_PATH.inject(
                msg,
                FileInfoFields.LOGIC_PATH.from(msg, String.class) + "\\" + FileInfoFields.ORIGINAL_NAME.from(msg, String.class)
        );
    }

    /**
     * Упаковать информацию о директории для отправки на удаленный сервер
     * @param msg содержит logicPath - логический путь в директории(включая имя файла)
     * @throws ReadValueException
     * @throws ChangeValueException
     */
    @Handler("formInfoForServer")
    public void formInfoForServer(IMessage msg) throws ReadValueException, ChangeValueException {

        IObject info = IOC.resolve(IObject.class);
        FileInfoFields.LOGIC_PATH.inject(info, FileInfoFields.LOGIC_PATH.from(msg, String.class));
        FileInfoFields.ORIGINAL_NAME.inject(info, FileInfoFields.ORIGINAL_NAME.from(msg, String.class));
        FileInfoFields.IS_FOLDER.inject(info, Boolean.TRUE);
        fileIdF.inject(info, FileInfoFields.LOGIC_PATH.from(msg, String.class));
        PostRequestFields.POST_REQUEST_DATA.inject(msg, info);
        PostRequestFields.REMOTE_MSG_MAP.inject(msg, createFolderServerMm);
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
        FileInfoFields.SERVER_GUID.inject(msg, FileInfoFields.SERVER_GUID.from(data, String.class));
    }

    /**
     * Готовит начальные данные о файле для контроля загрузки на удаленный сервер
     * @param msg
     * @throws ReadValueException
     * @throws ChangeValueException
     */
    @Handler("prepareFileInfo")
    public void prepareFileInfo(IMessage msg) throws ReadValueException, ChangeValueException {

        IObject folderInfo = IOC.resolve(IObject.class);
        FileInfoFields.IS_SENT.inject(folderInfo, Boolean.TRUE);
        FileInfoFields.IS_FOLDER.inject(folderInfo, Boolean.TRUE);
        FileInfoFields.SERVER_GUID.inject(folderInfo, FileInfoFields.SERVER_GUID.from(msg, String.class));
        msg.setValue(new FieldName(FileInfoFields.LOGIC_PATH.from(msg, String.class).replace('\\', '_')), folderInfo);
    }

    /**
     * Поместить информацию о файле в объект для последующего соханения в БД
     * @param msg содержит по ключу-идентификатору файла информацию о файле для сохранения в БД
     * @throws ReadValueException
     * @throws ChangeValueException
     */
    @Handler("markFileInfoForStorage")
    public void markFileInfoForStorage(IMessage msg) throws ReadValueException, ChangeValueException {

        IObject insertObj = IOC.resolve(IObject.class);
        //TODO:Сделать служебным мутабельным именем
        FieldName filePathFN = new FieldName(FileInfoFields.LOGIC_PATH.from(msg, String.class).replace('\\', '_'));
        IObject fileInfo = (IObject) msg.getValue(filePathFN);
        insertObj.setValue(filePathFN, fileInfo);
        collectionNameF.inject(msg, fileInfoCollectionName);

        insertDataF.inject(msg, insertObj);
    }
}
