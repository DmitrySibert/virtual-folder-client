package folder.database.embedded;

import info.smart_tools.smartactors.core.*;
import info.smart_tools.smartactors.core.actors.Actor;
import info.smart_tools.smartactors.core.actors.annotations.Handler;
import info.smart_tools.smartactors.core.impl.SMObject;
import info.smart_tools.smartactors.utils.ioc.IOC;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.File;
import java.util.Map;

/**
 * Actor for work with mapdb
 */
public class MapDbActor extends Actor {

    private DB db;

    public MapDbActor(IObject params) {

        String dbFilePath, dbFilePassword;
        try {
            dbFilePath = new Field<String>(new FieldName("dbFilePath")).from(params, String.class);
            dbFilePassword = new Field<String>(new FieldName("password")).from(params, String.class);
        } catch (ReadValueException | ChangeValueException e) {
            String err = "An error occurred while creating actor: " + e;
            System.out.println(err);
            throw new RuntimeException(err);
        }

        db = DBMaker.fileDB(new File(dbFilePath))
                .closeOnJvmShutdown()
                .encryptionEnable(dbFilePassword)
                .make();
    }

    @Handler("forceCommit")
    public void forceCommit(IMessage msg) throws ChangeValueException {

        try {
            db.commit();
        } catch (Exception e) {

            System.out.println("An error occurred while commiting data in mapdb: " + e);
            db.rollback();
        }
    }

    @Handler("insert")
    public void insert(IMessage msg) throws ReadValueException, ChangeValueException {

        Map<String, String> map = db.treeMap(KeyValueDB.COLLECTION_NAME.from(msg, String.class));
        IObject insertData = KeyValueDB.INSERT_DATA.from(msg, IObject.class);
        IObjectIterator it = insertData.iterator();
        while (it.next()) {
            map.put(it.getName().toString(), it.getValue().toString());
        }
    }

    @Handler("get")
    public void get(IMessage msg) throws ReadValueException, ChangeValueException {

        try {
            Map<String, String> map = db.treeMap(KeyValueDB.COLLECTION_NAME.from(msg, String.class));
            IObject searchData = IOC.resolve(IObject.class);
            for (String key : KeyValueDB.TARGET_KEYS.from(msg, String.class)) {
                String data = map.get(key);
                if (data != null) {
                    searchData.setValue(new FieldName(key), data);
                }
            }

            KeyValueDB.SEARCH_DATA.inject(msg, searchData);
        } catch (Exception e) {
            System.out.println("An error occurred while inserting data in mapdb: " + e);
        }
    }

    @Handler("delete")
    public void delete(IMessage msg) throws ReadValueException, ChangeValueException {

        try {
            Map<String, String> map = db.treeMap(KeyValueDB.COLLECTION_NAME.from(msg, String.class));
            for (String key : KeyValueDB.TARGET_KEYS.from(msg, String.class)) {
                map.remove(key);
            }
        } catch (Exception e) {
            System.out.println("An error occurred while inserting data in mapdb: " + e);
        }
    }
}
