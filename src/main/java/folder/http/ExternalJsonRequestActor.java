package folder.http;

import info.smart_tools.smartactors.core.*;
import info.smart_tools.smartactors.core.actors.Actor;
import info.smart_tools.smartactors.core.actors.annotations.Handler;
import info.smart_tools.smartactors.core.impl.SMObject;
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
    private String clientUri;

    private CloseableHttpClient client;

    public ExternalJsonRequestActor(IObject params) {

        try {
            serverAddr = new Field<String>(new FieldName("serverAddr")).from(params, String.class);
            clientUri = new Field<String>(new FieldName("clientUri")).from(params, String.class);
            client = HttpClients.createDefault();
        } catch (Exception e) {

            String errMsg = "An error occurred while creating ExternalJsonRequestActor: " + e;
            System.out.println(errMsg);
            throw new RuntimeException(errMsg, e);
        }
    }

    @Handler("post")
    public void post(IMessage msg) throws ChangeValueException, ReadValueException, ExecutionException, InterruptedException, IOException {

        StringBuilder requestBody = new StringBuilder();
        requestBody
                .append("{")
                .append("  \"address\": {")
                .append("       \"messageMapId\":").append("\"").append(
                    PostRequestFields.REMOTE_MSG_MAP.from(msg, String.class)
                ).append("\"")
                .append("  },\n");
        IObject requestData = PostRequestFields.POST_REQUEST_DATA.from(msg, IObject.class);
        IObjectIterator it = requestData.iterator();
        while (it.next()) {
            requestBody.append("\"").append(it.getName()).append("\"").append(":");
            requestBody.append("\"").append(it.getValue()).append("\"").append(",");
        }
        requestBody.append("\"").append("status").append("\"").append(":");
        requestBody.append("\"").append("ok").append("\"");
        requestBody.append("}");
        StringEntity input = new StringEntity(requestBody.toString().replace("\\", "\\\\"), StandardCharsets.UTF_8);
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
