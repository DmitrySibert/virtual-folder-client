package folder.encode;

import info.smart_tools.smartactors.core.Field;
import info.smart_tools.smartactors.core.FieldName;
import info.smart_tools.smartactors.core.ListField;

/**

 */
public class EncodeFields {

    public static final ListField<Byte> ENCODE_TARGET = new ListField<>(new FieldName("encodeTarget")) ;
    public static final Field<String> ENCODE_RESULT = new Field<>(new FieldName("encodeResult")) ;
    public static final Field<String> DECODE_TARGET = new Field<>(new FieldName("decodeTarget")) ;
    public static final ListField<Byte> DECODE_RESULT = new ListField<>(new FieldName("decodeResult")) ;
}
