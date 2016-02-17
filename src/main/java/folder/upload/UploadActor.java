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
    /** Полный путь к файлу который хранится в системной директории приложения */
    private Field<String> filePathAbsF;
    /** Настоящее имя файла*/
    private Field<String> fileOriginNameF;
    /** Имя, присвоенное файлу, для хранения в системной директории приложения */
    private Field<String> fileIdF;
    private Field<Long> fileSizeF;
    private Field<String> srcF;
    private Field<String> destF;
    /** Данные для post-запроса*/
    private Field<IObject> postRequestDataF;
    /** Поле для имени коллекции для сохранения */
    private Field<String> collectionNameF;
    /** Поле с данными для сохранение*/
    private Field<IObject> insertDataF;

    /** Информация по файлу */
    private Field<Integer> sentPartsF;
    private Field<Integer> partsQuantityF;
    private Field<Boolean> isSentF;

    private String fileInfoCollectionName;

    public UploadActor(IObject params) {

        try {
            fileInfoCollectionName = new Field<String>(new FieldName("fileInfoCollectionName")).from(params, String.class);
        } catch (ReadValueException | ChangeValueException e) {
            System.out.println("An error occurred while constructing UploadActor: " + e);
        }
        filePathF = new Field<>(new FieldName("filePath"));
        storageFolderF = new Field<>(new FieldName("storageFolder"));
        filePathAbsF = new Field<>(new FieldName("filePathAbs"));
        fileSizeF = new Field<>(new FieldName("fileSize"));
        srcF = new Field<>(new FieldName("src"));
        destF = new Field<>(new FieldName("dest"));
        postRequestDataF = new Field<>(new FieldName("postRequestData"));
        folderDestPathF = new Field<>(new FieldName("folderDestPath"));
        fileOriginNameF = new Field<>(new FieldName("fileOriginName"));
        fileIdF = new Field<>(new FieldName("fileId"));
        sentPartsF = new Field<>(new FieldName("sentParts"));
        partsQuantityF = new Field<>(new FieldName("partsQuantity"));
        isSentF = new Field<>(new FieldName("isSent"));
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

        String filePathAbs = filePathAbsF.from(msg, String.class);
        srcF.inject(msg, filePathF.from(msg, String.class));
        destF.inject(msg, filePathAbs);
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

        String[] filePathTokens = filePathF.from(msg, String.class).split("\\\\");
        String fileOriginName = filePathTokens[filePathTokens.length - 1];
        fileOriginNameF.inject(msg, fileOriginName);
        String filePathAbs = storageFolderF.from(msg, String.class) + UUID.randomUUID().toString();
        filePathAbsF.inject(msg, filePathAbs);
        File file = new File(filePathAbs);
        fileSizeF.inject(msg, file.length());
    }

    /**
     * Готовит начальные данные о файле для контроля загрузки на удаленный сервер
     * @param msg
     * @throws ReadValueException
     * @throws ChangeValueException
     */
    @Handler("prepareFileInfoForStorage")
    public void prepareFileInfoForStorage(IMessage msg) throws ReadValueException, ChangeValueException {

        IObject insertObj = new SMObject();
        IObject fileInfo = new SMObject();
        sentPartsF.inject(fileInfo, 0);
        isSentF.inject(fileInfo, Boolean.FALSE);
        fileSizeF.inject(fileInfo, fileSizeF.from(msg, Long.class));
        filePathF.inject(fileInfo, filePathAbsF.from(msg, String.class));
        insertObj.setValue(new FieldName(filePathF.from(msg, String.class)), fileInfo);

        collectionNameF.inject(msg, fileInfoCollectionName);
        insertDataF.inject(msg, insertObj);
    }

    /**
     * Упаковать информацию о файле для отправки на удаленный сервер
     * @param msg содержит filePath - местоположение файла на локальной машине
     *                     fileSize - размер файла
     *                     fileOriginName - оригинальное имя файла
     * @throws ReadValueException
     * @throws ChangeValueException
     */
    @Handler("formInfoForServer")
    public void formInfoForServer(IMessage msg) throws ReadValueException, ChangeValueException {

        //IOC.resolve(IObject.class);??
        IObject info = new SMObject();
        fileSizeF.inject(info, fileSizeF.from(msg, Long.class));
        fileOriginNameF.inject(info, fileOriginNameF.from(msg, String.class));
        fileIdF.inject(info, filePathF.from(msg, String.class));
        postRequestDataF.inject(msg, info);
    }
}
