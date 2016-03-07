package folder.database.embedded;

import info.smart_tools.smartactors.core.*;
import info.smart_tools.smartactors.core.actors.Actor;
import info.smart_tools.smartactors.core.actors.annotations.Handler;
import info.smart_tools.smartactors.core.impl.SMObject;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.File;
import java.util.Map;

/**
 * Actor for work with mapdb
 */
public class MapDbActor extends Actor {

    private Field<String> collectionNameF;
    private Field<IObject> insertDataF;
    private Field<IObject> searchDataF;
    private ListField<String> searchKeysF;

    private DB db;

    public MapDbActor(IObject params) {

        String dbFilePath, dbFilePassword;
        try {
            dbFilePath = new Field<String>(new FieldName("dbFilePath")).from(params, String.class);
            dbFilePassword = new Field<String>(new FieldName("password")).from(params, String.class);
            collectionNameF = new Field<>(new FieldName("collectionName"));
            insertDataF = new Field<>(new FieldName("insertData"));
            searchDataF = new Field<>(new FieldName("searchData"));
            searchKeysF = new ListField<>(new FieldName("searchKeys"));
        } catch (ReadValueException | ChangeValueException e) {
            String err = "An error occurred while creating actor: " + e;
            System.out.println(err);
            throw new RuntimeException(err);
        }

        System.out.println(dbFilePath);
        try {
            db = DBMaker.fileDB(new File(dbFilePath))
                    .closeOnJvmShutdown()
                    .encryptionEnable(dbFilePassword)
                    .make();
        } catch (Exception e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
        System.out.println("mapDBvishla");
    }

    @Handler("insert")
    public void insert(IMessage msg) throws ReadValueException, ChangeValueException {

        try {
            Map<String, String> map = db.treeMap(collectionNameF.from(msg, String.class));
            IObject insertData = insertDataF.from(msg, IObject.class);
            IObjectIterator it = insertData.iterator();
            while (it.next()) {
                map.put(it.getName().toString(), it.getValue().toString());
            }
            db.commit();
        } catch (Exception e) {

            System.out.println("An error occurred while inserting data in mapdb: " + e);
            db.rollback();
        }
    }

    @Handler("get")
    public void get(IMessage msg) throws ReadValueException, ChangeValueException {

        try {
            Map<String, String> map = db.treeMap(collectionNameF.from(msg, String.class));
            IObject searchData = new SMObject();
            for (String key : searchKeysF.from(msg, String.class)) {
                String data = map.get(key);
                if (data != null) {
                    searchData.setValue(new FieldName(key), data);
                }
            }

            searchDataF.inject(msg, searchData);
        } catch (Exception e) {
            System.out.println("An error occurred while inserting data in mapdb: " + e);
        }
    }
}
