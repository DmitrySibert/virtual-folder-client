package folder.util;

import info.smart_tools.smartactors.core.Field;
import info.smart_tools.smartactors.core.FieldName;

/**
 * Информационные поля файла виртуальной директории
 */
public class FileInfoFields {

    public static final Field<Integer> FILE_SIZE = new Field<>(new FieldName("fileSize"));
    public static final Field<Integer> SENT_PARTS = new Field<>(new FieldName("sentParts"));
    public static final Field<Integer> PARTS_QUANTITY = new Field<>(new FieldName("partsQuantity"));
    public static final Field<Integer> PART_SIZE = new Field<>(new FieldName("partSize"));
    public static final Field<Integer> DOWNLOAD_PARTS = new Field<>(new FieldName("downloadParts"));
    public static final Field<Boolean> IS_SENT = new Field<>(new FieldName("isSent"));
    public static final Field<Boolean> ACTIVE = new Field<>(new FieldName("active"));
    public static final Field<String> SERVER_GUID = new Field<>(new FieldName("serverGuid"));
    public static final Field<String> LOGIC_PATH = new Field<>(new FieldName("logicPath"));
    public static final Field<String> PHYSIC_PATH = new Field<>(new FieldName("phyPath"));
    public static final Field<String> ORIGINAL_NAME = new Field<>(new FieldName("originalName"));

}
