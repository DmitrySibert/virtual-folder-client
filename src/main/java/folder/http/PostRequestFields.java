package folder.http;

import info.smart_tools.smartactors.core.Field;
import info.smart_tools.smartactors.core.FieldName;
import info.smart_tools.smartactors.core.IObject;

/**
 * Служебные поля для пост запросов
 */
public class PostRequestFields {

    public static final Field<IObject> POST_REQUEST_DATA = new Field<>(new FieldName("postRequestData"));
    public static final Field<IObject> POST_RESPONSE_DATA = new Field<>(new FieldName("postResponseData"));
    public static final Field<String> REMOTE_MSG_MAP = new Field<>(new FieldName("remoteMsgMap"));
}
