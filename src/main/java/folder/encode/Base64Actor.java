package folder.encode;

import com.google.common.primitives.Bytes;
import info.smart_tools.smartactors.core.*;
import info.smart_tools.smartactors.core.actors.Actor;
import info.smart_tools.smartactors.core.actors.annotations.Handler;

import java.util.Base64;
import java.util.List;

/**
 * Актор кодирует и декодирует объекты в\из Base64
 */
public class Base64Actor extends Actor {

    public Base64Actor(IObject params) {
    }

    @Handler("encode")
    public void encode(IMessage msg) throws ReadValueException, ChangeValueException {

        List<Byte> targ = EncodeFields.ENCODE_TARGET.from(msg, Byte.class);
        String encodeStr = Base64.getUrlEncoder().withoutPadding().encodeToString(Bytes.toArray(targ));
        EncodeFields.ENCODE_RESULT.inject(msg, encodeStr);
    }

    @Handler("decode")
    public void decode(IMessage msg) throws ReadValueException, ChangeValueException {

        String targ = EncodeFields.DECODE_TARGET.from(msg, String.class);
        byte[] decodeRes = Base64.getUrlDecoder().decode(targ);
        EncodeFields.DECODE_RESULT.inject(msg, Bytes.asList(decodeRes));
    }
}
