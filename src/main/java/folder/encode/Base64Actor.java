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

    private ListField<Byte> encodeTargetF;
    private Field<String> encodeResultF;

    private Field<String> decodeTargetF;
    private ListField<Byte> decodeResultF;

    public Base64Actor(IObject params) {

        encodeTargetF = new ListField<>(new FieldName("encodeTarget"));
        encodeResultF = new Field<>(new FieldName("encodeResult"));
        decodeTargetF = new Field<>(new FieldName("decodeTarget"));
        decodeResultF = new ListField<>(new FieldName("decodeResult"));
    }

    @Handler("encode")
    public void encode(IMessage msg) throws ReadValueException, ChangeValueException {

        List<Byte> targ = encodeTargetF.from(msg, Byte.class);
        String encodeStr = Base64.getEncoder().withoutPadding().encodeToString(Bytes.toArray(targ));
        encodeResultF.inject(msg, encodeStr);
    }

    @Handler("decode")
    public void decode(IMessage msg) throws ReadValueException, ChangeValueException {

        String targ = decodeTargetF.from(msg, String.class);
        byte[] decodeRes = Base64.getDecoder().decode(targ);
        decodeResultF.inject(msg, Bytes.asList(decodeRes));
    }
}
