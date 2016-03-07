package folder.upload;

import info.smart_tools.smartactors.core.*;
import info.smart_tools.smartactors.core.actors.Actor;
import info.smart_tools.smartactors.core.actors.annotations.Handler;
import info.smart_tools.smartactors.core.impl.SMObject;

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
    private Field<Integer> fileSizeF;
    private Field<String> srcF;
    private Field<String> destF;
    /** Данные для post-запроса*/
    private Field<IObject> postRequestDataF;
    private Field<IObject> postResponseDataF;
    private Field<String> remoteMsgMapF;

    /** Поле для имени коллекции для сохранения */
    private Field<String> collectionNameF;
    /** Поле с данными для сохранение*/
    private Field<IObject> insertDataF;

    /** Информация по файлу */
    private Field<Integer> sentPartsF;
    private Field<Integer> partsQuantityF;
    private Field<Integer> partSizeF;
    private Field<Boolean> isSentF;
    private Field<String> serverGuidF;
    /** Полный путь к файлу который хранится в системной директории приложения */
    private Field<String> phyPathF;
    private Field<String> logicPathF;


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
        fileSizeF = new Field<>(new FieldName("fileSize"));
        srcF = new Field<>(new FieldName("src"));
        destF = new Field<>(new FieldName("dest"));
        postRequestDataF = new Field<>(new FieldName("postRequestData"));
        postResponseDataF = new Field<>(new FieldName("postResponseData"));
        folderDestPathF = new Field<>(new FieldName("folderDestPath"));
        fileOriginNameF = new Field<>(new FieldName("fileOriginName"));
        fileIdF = new Field<>(new FieldName("fileId"));
        remoteMsgMapF = new Field<>(new FieldName("remoteMsgMap"));
        sentPartsF = new Field<>(new FieldName("sentParts"));
        partsQuantityF = new Field<>(new FieldName("partsQuantity"));
        partSizeF = new Field<>(new FieldName("partSize"));
        isSentF = new Field<>(new FieldName("isSent"));
        serverGuidF = new Field<>(new FieldName("serverGuid"));
        logicPathF = new Field<>(new FieldName("logicPath"));
        phyPathF = new Field<>(new FieldName("phyPath"));
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
        phyPathF.inject(msg, storageFolderF.from(msg, String.class) + "\\" + UUID.randomUUID().toString());
        logicPathF.inject(msg, logicPathF.from(msg, String.class) + "\\" + fileOriginName);
        File file = new File(originFilePath);
        fileSizeF.inject(msg, Long.valueOf(file.length()).intValue());
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

        String filePathAbs = phyPathF.from(msg, String.class);
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

        IObject fileInfo = new SMObject();
        sentPartsF.inject(fileInfo, 0);
        isSentF.inject(fileInfo, Boolean.FALSE);
        fileSizeF.inject(fileInfo, fileSizeF.from(msg, Integer.class));
        phyPathF.inject(fileInfo, phyPathF.from(msg, String.class));
        msg.setValue(new FieldName(logicPathF.from(msg, String.class).replace('\\', '_')), fileInfo);
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
        FieldName filePathFN = new FieldName(logicPathF.from(msg, String.class).replace('\\', '_'));
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

        //TODO: IOC.resolve(IObject.class);
        IObject info = new SMObject();
        fileSizeF.inject(info, fileSizeF.from(msg, Integer.class));
        fileIdF.inject(info, logicPathF.from(msg, String.class));
        postRequestDataF.inject(msg, info);
        remoteMsgMapF.inject(msg, "beginUploadMessageMap");
    }

    /**
     * Извлечь данные для загрузки файла на сервер, присланные сервером
     * @param msg содержит postResponseData - идентификатор файла, guid выданный сервером, количество кусков, размер куска
     * @throws ReadValueException
     * @throws ChangeValueException
     */
    @Handler("handleUploadInfoFromServer")
    public void handleUploadInfoFromServer(IMessage msg) throws ReadValueException, ChangeValueException {

        IObject data = postResponseDataF.from(msg, IObject.class);
        fileIdF.inject(msg, fileIdF.from(data, String.class));
        serverGuidF.inject(msg, serverGuidF.from(data, String.class));
        partSizeF.inject(msg, partSizeF.from(data, Integer.class));
        partsQuantityF.inject(msg, partsQuantityF.from(data, Integer.class));
    }

    /**
     * Добавить в информацию о файле данные для загрузки на сервер, присланные сервером
     * @param msg
     * @throws ReadValueException
     * @throws ChangeValueException
     */
    @Handler("prepareUploadInfoForStorage")
    public void prepareUploadInfoForStorage(IMessage msg) throws ReadValueException, ChangeValueException {

        FieldName filePathFN = new FieldName(filePathF.from(msg, String.class).replace('\\', '_'));
        IObject fileInfo = (IObject) msg.getValue(filePathFN);
        serverGuidF.inject(fileInfo, serverGuidF.from(msg, String.class));
        partSizeF.inject(fileInfo, partSizeF.from(msg, Integer.class));
        partsQuantityF.inject(fileInfo, partsQuantityF.from(msg, Integer.class));
    }

    @Handler("finishUpload")
    public void finishUpload(IMessage msg) throws ReadValueException, ChangeValueException {
        //TODO:Сделать служебным мутабельным именем
        FieldName filePathFN = new FieldName(logicPathF.from(msg, String.class).replace('\\', '_'));
        IObject fileInfo = (IObject) msg.getValue(filePathFN);
        isSentF.inject(fileInfo, Boolean.TRUE);
    }
}
