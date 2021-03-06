package folder.upload;

import com.google.common.primitives.Bytes;
import folder.http.PostRequestFields;
import folder.util.FileInfoFields;
import info.smart_tools.smartactors.core.*;
import info.smart_tools.smartactors.core.actors.Actor;
import info.smart_tools.smartactors.core.actors.annotations.Handler;
import info.smart_tools.smartactors.core.addressing.AddressingFields;
import info.smart_tools.smartactors.core.addressing.IMessageMapId;
import info.smart_tools.smartactors.core.addressing.MessageMapId;
import info.smart_tools.smartactors.core.addressing.maps.MessageMap;
import info.smart_tools.smartactors.core.impl.SMObject;
import info.smart_tools.smartactors.utils.ioc.IOC;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Актор выдает необходимые кусочки файла во время загрузки на удаленный сервер
 */
public class PartsMakerActor extends Actor {

    /** Данные о файле */
    private Field<String> filePathF;
    private Field<Integer> sentPartsF;
    private Field<Integer> partsQuantityF;
    private Field<Integer> partSizeF;
    private Field<String> partF;
    private Field<String> serverGuidF;
    private Field<Integer> partNumberF;
    private Field<String> logicPathF;
    /** Полный путь к файлу который хранится в системной директории приложения */
    private Field<String> phyPathF;
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
        partF = new Field<>(new FieldName("part"));
        serverGuidF = new Field<>(new FieldName("serverGuid"));
        partNumberF = new Field<>(new FieldName("partNumber"));

        encodeTargetF = new ListField<>(new FieldName("encodeTarget"));
        encodeResultF = new Field<>(new FieldName("encodeResult"));

        statusF = new Field<>(new FieldName("status"));
        logicPathF = new Field<>(new FieldName("logicPath"));
        phyPathF = new Field<>(new FieldName("phyPath"));

        fieldName = new MutableFieldName("default");
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
        IObject addrF = IOC.resolve(IObject.class);
        AddressingFields.MESSAGE_MAP_ID_FIELD.inject(addrF, uploadFilePartMmId);
        AddressingFields.ADDRESS_FIELD.inject(msg, addrF);
    }

    @Handler("makePart")
    public void makePart(IMessage msg) throws ReadValueException, ChangeValueException, DeleteValueException {

        fieldName.setName(logicPathF.from(msg, String.class).replace('\\', '_'));
        IObject fileInfo = (IObject) msg.getValue(fieldName);
        if (!sentPartsF.from(fileInfo, Integer.class).equals(partsQuantityF.from(fileInfo, Integer.class))) {
            String phyPath = FileInfoFields.PHYSIC_PATH.from(fileInfo, String.class);
            Integer partSize = Math.min(
                    FileInfoFields.PART_SIZE.from(fileInfo, Integer.class),
                    FileInfoFields.FILE_SIZE.from(fileInfo, Integer.class) -
                            FileInfoFields.SENT_PARTS.from(fileInfo, Integer.class) * FileInfoFields.PART_SIZE.from(fileInfo, Integer.class)
            );
            byte[] part = new byte[partSize];
            try {
                RandomAccessFile file = new RandomAccessFile(phyPath, "rw");
                Integer sentBytes = sentPartsF.from(fileInfo, Integer.class) * partSizeF.from(fileInfo, Integer.class);
                file.skipBytes(sentBytes);
                file.read(part, 0, partSize);
                file.close();
            } catch (IOException e) {
                System.out.println("An error occurred while making a part of file: " + e);
            }
            encodeTargetF.inject(msg, Bytes.asList(part));
        } else {
            AddressingFields.MESSAGE_MAP_FIELD.delete(msg);
            IObject addrF = IOC.resolve(IObject.class);
            AddressingFields.MESSAGE_MAP_ID_FIELD.inject(addrF, finishUploadMmId);
            AddressingFields.ADDRESS_FIELD.inject(msg, addrF);
        }
    }

    @Handler("formFilePartRequest")
    public void formPostRequest(IMessage msg) throws ChangeValueException, ReadValueException {

        IObject postData = IOC.resolve(IObject.class);
        fieldName.setName(logicPathF.from(msg, String.class).replace('\\', '_'));

        IObject fileInfo = (IObject) msg.getValue(fieldName);
        serverGuidF.inject(postData, serverGuidF.from(fileInfo, String.class));
        partF.inject(postData, encodeResultF.from(msg, String.class));
        partNumberF.inject(postData, sentPartsF.from(fileInfo, Integer.class) + 1);

        PostRequestFields.POST_REQUEST_DATA.inject(msg, postData);
        PostRequestFields.REMOTE_MSG_MAP.inject(msg, "filePartReceivingMm");
    }

    @Handler("handleFilePartSend")
    public void handleFilePartSend(IMessage msg) throws ReadValueException, ChangeValueException {

        IObject data = PostRequestFields.POST_RESPONSE_DATA.from(msg, IObject.class);
        if(statusF.from(data, Boolean.class)) {
            //увеличиваем счетчик отосланных кусков
            fieldName.setName(logicPathF.from(msg, String.class).replace('\\', '_'));
            IObject fileInfo = (IObject) msg.getValue(fieldName);
            sentPartsF.inject(fileInfo, sentPartsF.from(fileInfo, Integer.class) + 1);
        } else {
            //переключаем карту сообщений на начало
            MessageMap curMsgMap = AddressingFields.MESSAGE_MAP_FIELD.from(msg, MessageMap.class);
            curMsgMap.toStart();
        }
    }
}
