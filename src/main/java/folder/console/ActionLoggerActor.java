package folder.console;

import info.smart_tools.smartactors.core.*;
import info.smart_tools.smartactors.core.actors.Actor;
import info.smart_tools.smartactors.core.actors.annotations.Handler;

/**
 * Log all info received by external sources
 */
public class ActionLoggerActor extends Actor {

    private Field<String> logMsgF = new Field<>(new FieldName("logMsg"));

    public ActionLoggerActor(IObject params) {
    }

    @Handler("logConsole")
    public void logConsole(IMessage msg) throws ReadValueException, ChangeValueException {

        System.out.println(logMsgF.from(msg, String.class));
    }
}
