package folder.http;

import info.smart_tools.smartactors.core.*;
import info.smart_tools.smartactors.core.actors.Actor;
import info.smart_tools.smartactors.core.actors.annotations.Handler;
import info.smart_tools.smartactors.core.addressing.AddressingFields;
import info.smart_tools.smartactors.core.addressing.MessageMapId;
import info.smart_tools.smartactors.core.impl.SMObject;
import info.smart_tools.smartactors.utils.ioc.IOC;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

/**
 * Actor which send post requests
 */
public class ExternalJsonRequestActor extends Actor {

    private String serverAddr;

    private CloseableHttpClient client;

    public ExternalJsonRequestActor(IObject params) {

        try {
            serverAddr = new Field<String>(new FieldName("serverAddr")).from(params, String.class);
            client = HttpClients.createDefault();
        } catch (Exception e) {

            String errMsg = "An error occurred while creating ExternalJsonRequestActor: " + e;
            System.out.println(errMsg);
            throw new RuntimeException(errMsg, e);
        }
    }

    @Handler("post")
    public void post(IMessage msg) throws ChangeValueException, ReadValueException, ExecutionException, InterruptedException, IOException {

        IObject requestData = PostRequestFields.POST_REQUEST_DATA.from(msg, IObject.class);
        IObject addrF = IOC.resolve(IObject.class);
        AddressingFields.MESSAGE_MAP_ID_FIELD.inject(addrF, MessageMapId.fromString(PostRequestFields.REMOTE_MSG_MAP.from(msg, String.class)));
        AddressingFields.ADDRESS_FIELD.inject(requestData, addrF);

        StringEntity input = new StringEntity(requestData.toString(), StandardCharsets.UTF_8);
        input.setContentType("application/json");
        HttpPost post = new HttpPost(serverAddr);
        post.setHeader("Content-type", "application/json");
        post.setEntity(input);

        HttpResponse response = client.execute(post);
        BufferedReader br = new BufferedReader(
                new InputStreamReader((response.getEntity().getContent())));

        PostRequestFields.POST_RESPONSE_DATA.inject(msg, new SMObject(br.readLine()));
    }
}
