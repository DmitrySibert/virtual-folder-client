package folder.upload;

import com.google.common.primitives.Bytes;
import info.smart_tools.smartactors.core.*;
import info.smart_tools.smartactors.core.actors.Actor;
import info.smart_tools.smartactors.core.actors.annotations.Handler;
import info.smart_tools.smartactors.core.addressing.AddressingFields;
import info.smart_tools.smartactors.core.addressing.IMessageMapId;
import info.smart_tools.smartactors.core.addressing.MessageMapId;
import info.smart_tools.smartactors.core.addressing.maps.MessageMap;
import info.smart_tools.smartactors.core.impl.SMObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Актор выдает необходимые кусочки файла во время загрузки на удаленный сервер
 */
public class PartsMakerActor extends Actor {

    /** Данные о файле */
    private Field<String> filePathF;
    private Field<Integer> sentPartsF;
    private Field<Integer> partsQuantityF;
    private Field<Integer> partSizeF;
    private Field<String> filePartF;
    private Field<String> serverGuidF;
    private Field<Integer> partNumberF;
    /** Данные для post-запроса*/
    private Field<IObject> postRequestDataF;
    private Field<IObject> postResponseDataF;
    private Field<String> remoteMsgMapF;
    /** Данные об отправке куска файла */
    private Field<Boolean> statusF;
    /** Данные для кодирования */
    private ListField<Byte> encodeTargetF;
    private Field<String> encodeResultF;
    /** Служебные элементы актора*/
    private MutableFieldName fieldName;
    private IMessageMapId finishUploadMmId;
    private IMessageMapId uploadFilePartMmId;

    public PartsMakerActor(IObject params) {

        filePathF = new Field<>(new FieldName("filePath"));
        sentPartsF = new Field<>(new FieldName("sentParts"));
        partsQuantityF = new Field<>(new FieldName("partsQuantity"));
        partSizeF = new Field<>(new FieldName("partSize"));
        filePartF = new Field<>(new FieldName("filePart"));
        serverGuidF = new Field<>(new FieldName("serverGuid"));
        partNumberF = new Field<>(new FieldName("partNumber"));

        encodeTargetF = new ListField<>(new FieldName("encodeTarget"));
        encodeResultF = new Field<>(new FieldName("encodeResult"));

        postRequestDataF = new Field<>(new FieldName("postRequestData"));
        postResponseDataF = new Field<>(new FieldName("postResponseData"));
        remoteMsgMapF = new Field<>(new FieldName("remoteMsgMap"));
        statusF = new Field<>(new FieldName("status"));

        try {
            finishUploadMmId = MessageMapId.fromString(
                    new Field<String>(new FieldName("finishUploadMmId")).from(params, String.class)
            );
            uploadFilePartMmId = MessageMapId.fromString(
                    new Field<String>(new FieldName("uploadFilePartMmId")).from(params, String.class)
            );
        } catch (ReadValueException | ChangeValueException e) {
            String errMsg = "An error occurred while constructing PartsMakerActor: " + e;
            System.out.println(errMsg);
            throw new RuntimeException(errMsg);
        }
    }

    @Handler("initPartsUpload")
    public void initPartsUpload(IMessage msg) throws DeleteValueException, ChangeValueException {

        AddressingFields.MESSAGE_MAP_FIELD.delete(msg);
        //TODO: resolve with IOC??
        IObject addrF = new SMObject();
        AddressingFields.MESSAGE_MAP_ID_FIELD.inject(addrF, uploadFilePartMmId);
        AddressingFields.ADDRESS_FIELD.inject(msg, addrF);
    }

    @Handler("makePart")
    public void makePart(IMessage msg) throws ReadValueException, ChangeValueException, DeleteValueException {

        fieldName.setName(filePathF.from(msg, String.class));
        IObject fileInfo = (IObject) msg.getValue(fieldName);
        if (!sentPartsF.from(fileInfo, Integer.class).equals(partsQuantityF.from(fileInfo, Integer.class))) {
            String sysFilePath = filePathF.from(fileInfo, String.class);
            byte[] part = null;
            try {
                byte[] data = Files.readAllBytes(Paths.get(sysFilePath));
                Integer from = sentPartsF.from(fileInfo, Integer.class) * partSizeF.from(fileInfo, Integer.class);
                Integer to = from + partSizeF.from(fileInfo, Integer.class) - 1;
                to = to > data.length - 1 ? data.length - 1 : to;
                part = Arrays.copyOfRange(data, from, to);
            } catch (IOException e) {
                System.out.println("An error occurred while making a part of file: " + e);
            }
            encodeTargetF.inject(msg, Bytes.asList(part));
        } else {
            AddressingFields.MESSAGE_MAP_FIELD.delete(msg);
            //TODO: resolve with IOC??
            IObject addrF = new SMObject();
            AddressingFields.MESSAGE_MAP_ID_FIELD.inject(addrF, finishUploadMmId);
            AddressingFields.ADDRESS_FIELD.inject(msg, addrF);
        }
    }

    @Handler("formFilePartRequest")
    public void formPostRequest(IMessage msg) throws ChangeValueException, ReadValueException {

        IObject postData = new SMObject();
        fieldName.setName(filePathF.from(msg, String.class));

        IObject fileInfo = (IObject) msg.getValue(fieldName);
        serverGuidF.inject(postData, serverGuidF.from(fileInfo, String.class));
        filePartF.inject(postData, encodeResultF.from(msg, String.class));
        partNumberF.inject(postData, sentPartsF.from(fileInfo, Integer.class) + 1);

        postRequestDataF.inject(msg, postData);
        remoteMsgMapF.inject(msg, "uploadFilePart");
    }

    @Handler("handleFilePartSend")
    public void handleFilePartSend(IMessage msg) throws ReadValueException, ChangeValueException {

        IObject data = postResponseDataF.from(msg, IObject.class);
        if(statusF.from(msg, Boolean.class)) {
            //увеличиваем счетчик отосланных кусков
            fieldName.setName(filePathF.from(msg, String.class));
            IObject fileInfo = (IObject) msg.getValue(fieldName);
            sentPartsF.inject(fileInfo, sentPartsF.from(fileInfo, Integer.class) + 1);
        } else {
            //переключаем карту сообщений на начало
            MessageMap curMsgMap = AddressingFields.MESSAGE_MAP_FIELD.from(msg, MessageMap.class);
            curMsgMap.toStart();
        }
    }
}
