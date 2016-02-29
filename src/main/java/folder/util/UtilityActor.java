package folder.util;

import info.smart_tools.smartactors.core.ChangeValueException;
import info.smart_tools.smartactors.core.IMessage;
import info.smart_tools.smartactors.core.IObject;
import info.smart_tools.smartactors.core.ReadValueException;
import info.smart_tools.smartactors.core.actors.Actor;
import info.smart_tools.smartactors.core.actors.annotations.Handler;
import info.smart_tools.smartactors.core.addressing.AddressingFields;
import info.smart_tools.smartactors.core.addressing.maps.MessageMap;

/**
 * Актор служембных функций
 */
public class UtilityActor extends Actor {

    public UtilityActor(IObject params) {
    }

    @Handler("refreshMessageMap")
    public void refreshMessageMap(IMessage msg) throws ReadValueException, ChangeValueException {

        MessageMap curMsgMap = AddressingFields.MESSAGE_MAP_FIELD.from(msg, MessageMap.class);
        curMsgMap.toStart();
    }
}
