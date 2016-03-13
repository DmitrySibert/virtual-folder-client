package folder.http;

import com.google.common.base.Charsets;
import info.smart_tools.smartactors.core.*;
import info.smart_tools.smartactors.core.actors.Actor;
import info.smart_tools.smartactors.core.actors.annotations.Handler;
import info.smart_tools.smartactors.core.impl.SMObject;
import info.smart_tools.smartactors.core.services.endpoints.netty.http.HttpClient;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpVersion;
import org.apache.http.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

/**
 * Actor which send post requests
 */
public class ExternalJsonRequestActor extends Actor {

    private Field<String> remoteMsgMapF;
    private Field<IObject> postRequestDataF;
    private Field<IObject> postResponseDataF;

    private String serverAddr;
    private String clientUri;

    private CloseableHttpClient client;
    private IObject responseBodyBuf;

    public ExternalJsonRequestActor(IObject params) {

        try {
            serverAddr = new Field<String>(new FieldName("serverAddr")).from(params, String.class);
            clientUri = new Field<String>(new FieldName("clientUri")).from(params, String.class);
            remoteMsgMapF = new Field<>(new FieldName("remoteMsgMap"));
            postRequestDataF = new Field<>(new FieldName("postRequestData"));
            postResponseDataF = new Field<>(new FieldName("postResponseData"));
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
                .append("       \"messageMapId\":").append("\"").append(remoteMsgMapF.from(msg, String.class)).append("\"")
                .append("  },\n");
        IObject requestData = postRequestDataF.from(msg, IObject.class);
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

        postResponseDataF.inject(msg, new SMObject(br.readLine()));
    }
}
