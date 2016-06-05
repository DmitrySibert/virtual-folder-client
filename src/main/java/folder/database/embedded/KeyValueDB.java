package folder.database.embedded;

import info.smart_tools.smartactors.core.Field;
import info.smart_tools.smartactors.core.FieldName;
import info.smart_tools.smartactors.core.IObject;
import info.smart_tools.smartactors.core.ListField;

/**
 */
public class KeyValueDB {

    public static final Field<String> COLLECTION_NAME = new Field<>(new FieldName("collectionName"));
    public static final Field<IObject> INSERT_DATA = new Field<>(new FieldName("insertData"));
    public static final Field<IObject> SEARCH_DATA = new Field<>(new FieldName("searchData"));
    public static final ListField<String> TARGET_KEYS = new ListField<>(new FieldName("targetKeys"));
    public static final Field<Boolean> FORCE_COMMIT = new Field<>(new FieldName("forceCommit"));

}
