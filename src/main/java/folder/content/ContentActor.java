package folder.content;

import folder.http.PostRequestFields;
import folder.util.FileInfoFields;
import info.smart_tools.smartactors.core.*;
import info.smart_tools.smartactors.core.actors.Actor;
import info.smart_tools.smartactors.core.actors.annotations.Handler;
import info.smart_tools.smartactors.core.addressing.AddressingFields;
import info.smart_tools.smartactors.core.addressing.MessageMapId;
import info.smart_tools.smartactors.core.routers.MessageBus;
import info.smart_tools.smartactors.utils.ioc.IOC;

import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public class ContentActor extends Actor {


    /** Поля для обработки запросов на получение содержимого директории */
    private Field<String> folderPathF;
    private ListField<IObject> fldrContentF;
    private ListField<IObject> filesF;
    private Field<String> logicPathF;
    private ListField<String> targetKeysF;   //ключи для запроса информации по файлам из БД
    private Field<IObject> searchDataF;      //данные, извлеченные из БД
    private Field<String> collectionNameF;
    private Field<String> originalNameF;
    private Field<String> statusF;
    private Field<Boolean> activeF;
    private Field<String> serverGuidF;       //идентификатор файла на сервере

    private String contentFromSrvMm;
    private String downloadFromSrvMm;
    private String fileInfoCollectionName;

    public ContentActor(IObject params) {
        folderPathF = new Field<>(new FieldName("folderPath"));
        fldrContentF = new ListField<>(new FieldName("folderContent"));
        filesF = new ListField<>(new FieldName("files"));
        logicPathF = new Field<>(new FieldName("logicPath"));
        targetKeysF = new ListField<>(new FieldName("targetKeys"));
        searchDataF = new Field<>(new FieldName("searchData"));
        collectionNameF = new Field<>(new FieldName("collectionName"));
        originalNameF = new Field<>(new FieldName("originalName"));
        statusF = new Field<>(new FieldName("status"));
        activeF = new Field<>(new FieldName("active"));
        serverGuidF =  new Field<>(new FieldName("serverGuid"));
        try {
            contentFromSrvMm = new Field<String>(new FieldName("contentFromSrvMm")).from(params, String.class);
            downloadFromSrvMm = new Field<String>(new FieldName("downloadFromSrvMm")).from(params, String.class);
            fileInfoCollectionName = new Field<String>(new FieldName("fileInfoCollectionName")).from(params, String.class);
        } catch (ReadValueException | ChangeValueException e) {
            String errMsg = "An error occurred while constructing ContentActor: " + e;
            System.out.println(errMsg);
            throw new RuntimeException(errMsg);
        }
    }

    @Handler("getContentByPath")
    public void getContentByPath(IMessage msg) throws ChangeValueException, ReadValueException {

        IObject post = IOC.resolve(IObject.class);
        FileInfoFields.LOGIC_PATH.inject(post, FileInfoFields.LOGIC_PATH.from(msg, String.class));
        PostRequestFields.POST_REQUEST_DATA.inject(msg, post);
        PostRequestFields.REMOTE_MSG_MAP.inject(msg, contentFromSrvMm);
    }

    @Handler("handleContentRequest")
    public void handleContentRequest(IMessage msg) throws ReadValueException, ChangeValueException {

        IObject contentData = PostRequestFields.POST_RESPONSE_DATA.from(msg, IObject.class);
        fldrContentF.inject(msg, fldrContentF.from(contentData, IObject.class));
    }

    @Handler("formFilesSrchQuery")
    public void formFileSrchQuery(IMessage msg) throws ReadValueException, ChangeValueException {

        List<IObject> filesInfo = fldrContentF.from(msg, IObject.class);
        List<String> searchKeys = new LinkedList<>();
        for(IObject fileInfo : filesInfo) {
            String fileId = folderPathF.from(msg, String.class) + "\\" + originalNameF.from(fileInfo, String.class);
            searchKeys.add(fileId.replace("\\", "_"));
        }
        collectionNameF.inject(msg, fileInfoCollectionName);
        targetKeysF.inject(msg, searchKeys);
    }

    @Handler("handleLocalContent")
    public void handleLocalContent(IMessage msg) throws ReadValueException, ChangeValueException {

        MutableFieldName uploadedFileKey = new MutableFieldName("def");
        List<IObject> folderContent = fldrContentF.from(msg, IObject.class);
        IObject uploadedFiles = searchDataF.from(msg, IObject.class);
        Field<IObject> uploadedFileF = new Field<>(uploadedFileKey);
        List<IObject> folderFiles = new LinkedList<>();
        List<IObject> forDownloadFiles = new LinkedList<>();
        for(IObject fileInfo : folderContent) {
            String logicPath = folderPathF.from(msg, String.class) + "\\" + originalNameF.from(fileInfo, String.class);
            uploadedFileKey.setName(logicPath.replace("\\", "_"));
            IObject uploadedFile = uploadedFileF.from(uploadedFiles, IObject.class);
            IObject folderFile = IOC.resolve(IObject.class);
            if(uploadedFile != null) {
                originalNameF.inject(folderFile, originalNameF.from(fileInfo, String.class));
                activeF.inject(folderFile, activeF.from(uploadedFile, Boolean.class));
                FileInfoFields.IS_FOLDER.inject(folderFile, FileInfoFields.IS_FOLDER.from(uploadedFile, Boolean.class));
            } else {
                activeF.inject(folderFile, Boolean.FALSE);
                IObject forDownloadFile = IOC.resolve(IObject.class);
                logicPathF.inject(forDownloadFile, logicPath);
                serverGuidF.inject(forDownloadFile, serverGuidF.from(fileInfo, String.class));
                FileInfoFields.IS_FOLDER.inject(forDownloadFile, FileInfoFields.IS_FOLDER.from(fileInfo, Boolean.class));
                forDownloadFiles.add(forDownloadFile);
            }
            folderFiles.add(folderFile);
        }
        respondOn(msg, response -> {
            fldrContentF.inject(response, folderFiles);
        });
        if(forDownloadFiles.size() > 0) {
            IMessage dwnldMsg = new Message(IOC.resolve(IObject.class));
            IObject addrF = IOC.resolve(IObject.class);
            AddressingFields.MESSAGE_MAP_ID_FIELD.inject(addrF, MessageMapId.fromString(downloadFromSrvMm));
            AddressingFields.ADDRESS_FIELD.inject(dwnldMsg, addrF);
            filesF.inject(dwnldMsg, forDownloadFiles);
            MessageBus.send(dwnldMsg);
        }
    }
}
