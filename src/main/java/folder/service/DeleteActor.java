package folder.service;

import folder.database.embedded.KeyValueDB;
import folder.http.PostRequestFields;
import folder.util.FileInfoFields;
import info.smart_tools.smartactors.core.*;
import info.smart_tools.smartactors.core.actors.Actor;
import info.smart_tools.smartactors.core.actors.annotations.Handler;
import info.smart_tools.smartactors.core.addressing.AddressingFields;
import info.smart_tools.smartactors.core.addressing.MessageMapId;
import info.smart_tools.smartactors.utils.ioc.IOC;

import java.util.LinkedList;
import java.util.List;

/**
 */
public class DeleteActor extends Actor{

    private String fileInfoCollectionName;
    private String deleteFilesServerMm;
    private MessageMapId deleteFilesMmId;
    private MessageMapId deleteFilesFailedMmId;

    private ListField<String> filesPathF;
    private ListField<IObject> filesF;
    private ListField<String> guidsF;
    private Field<Boolean> statusF;


    public DeleteActor(IObject params) {

        try {
            fileInfoCollectionName = new Field<String>(new FieldName("fileInfoCollectionName")).from(params, String.class);
            deleteFilesServerMm = new Field<String>(new FieldName("deleteFilesServerMm")).from(params, String.class);
            deleteFilesMmId = MessageMapId.fromString(
                    new Field<String>(new FieldName("deleteFilesMmId")).from(params, String.class)
            );
            deleteFilesFailedMmId = MessageMapId.fromString(
                    new Field<String>(new FieldName("deleteFilesFailedMmId")).from(params, String.class)
            );
        } catch (ReadValueException | ChangeValueException e) {
            String errMsg = "An error occurred while constructing DeleteActor: " + e;
            System.out.println(errMsg);
            throw new RuntimeException(errMsg);
        }
        filesPathF = new ListField<>(new FieldName("filesPath"));
        filesF = new ListField<>(new FieldName("files"));
        guidsF = new ListField<>(new FieldName("guids"));
        statusF = new Field<>(new FieldName("status"));
    }

    //TODO: метод необходимо унести в специальный актор, который занимется обработкой
    //TODO: запросов, связанных с информацией о файлах, в встроенную базу данных
    @Handler("formFilesKeysQuery")
    public void formFilesKeysQuery(IMessage msg) throws ReadValueException, ChangeValueException {

        List<String> trgKeys = new LinkedList<>();
        for(String logicPath : filesPathF.from(msg, String.class)) {
            trgKeys.add(logicPath.replace("\\", "_"));
        }
        KeyValueDB.COLLECTION_NAME.inject(msg, fileInfoCollectionName);
        KeyValueDB.TARGET_KEYS.inject(msg, trgKeys);
    }

    //TODO: метод необходимо унести в специальный актор, который занимется обработкой
    //TODO: запросов, связанных с информацией о файлах, в встроенную базу данных
    @Handler("handleSrchResult")
    public void handleSrchResult(IMessage msg) throws ReadValueException, ChangeValueException {

        List<IObject> files = new LinkedList<>();
        IObject filesInfo = KeyValueDB.SEARCH_DATA.from(msg, IObject.class);
        IObjectIterator it = filesInfo.iterator();
        while (it.next()) {
            IObject file = IOC.resolve(IObject.class, it.getValue());
            files.add(file);
        }
        filesF.inject(msg, files);
    }

    @Handler("formFilesGuidsList")
    public void formFilesGuidsList(IMessage msg) throws ReadValueException, ChangeValueException {

        List<String> guids = new LinkedList<>();
        for (IObject file : filesF.from(msg, IObject.class)) {
            guids.add(FileInfoFields.SERVER_GUID.from(file, String.class));
        }
        guidsF.inject(msg, guids);
    }

    @Handler("prepareInfoForServer")
    public void prepareInfoForServer(IMessage msg) throws ReadValueException, ChangeValueException {

        IObject info = IOC.resolve(IObject.class);
        guidsF.inject(info, guidsF.from(msg, String.class));
        PostRequestFields.POST_REQUEST_DATA.inject(msg, info);
        PostRequestFields.REMOTE_MSG_MAP.inject(msg, deleteFilesServerMm);
    }

    @Handler("handleDeleteResponse")
    public void handleDeleteResponse(IMessage msg) throws ReadValueException, ChangeValueException, DeleteValueException {

        IObject data = PostRequestFields.POST_RESPONSE_DATA.from(msg, IObject.class);
        AddressingFields.MESSAGE_MAP_FIELD.delete(msg);
        IObject addrF = IOC.resolve(IObject.class);
        if (statusF.from(data, Boolean.class)) {
            AddressingFields.MESSAGE_MAP_ID_FIELD.inject(addrF, deleteFilesMmId);
        } else {
            AddressingFields.MESSAGE_MAP_ID_FIELD.inject(addrF, deleteFilesFailedMmId);
        }
        AddressingFields.ADDRESS_FIELD.inject(msg, addrF);
    }

    @Handler("successResult")
    public void successResult(IMessage msg) throws ReadValueException, ChangeValueException {

        respondOn(msg, response -> {
            statusF.inject(response, Boolean.TRUE);
        });
    }

    @Handler("errorResult")
    public void errorResult(IMessage msg) throws ReadValueException, ChangeValueException {

        respondOn(msg, response -> {
            statusF.inject(response, Boolean.FALSE);
        });
    }
}
