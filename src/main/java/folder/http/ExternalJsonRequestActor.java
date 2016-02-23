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

    private HttpClient client;
    private IObject responseBodyBuf;

    public ExternalJsonRequestActor(IObject params) {

        try {
            serverAddr = new Field<String>(new FieldName("serverAddr")).from(params, String.class);
            clientUri = new Field<String>(new FieldName("clientUri")).from(params, String.class);
            remoteMsgMapF = new Field<>(new FieldName("remoteMsgMap"));
            postRequestDataF = new Field<>(new FieldName("postRequestData"));
            postResponseDataF = new Field<>(new FieldName("postResponseData"));
            ChannelInboundHandler handler = new SimpleChannelInboundHandler<FullHttpResponse>(FullHttpResponse.class) {
                @Override
                protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
                    //TODO: IOC.resolve(IObject.class);
                    responseBodyBuf = new SMObject(msg.content().toString(Charsets.UTF_8));
                }
            };
            client = new HttpClient(new URI(serverAddr), handler);
            client.start().get();
        } catch (Exception e) {

            String errMsg = "An error occurred while creating ExternalJsonRequestActor: ";
            System.out.println(errMsg);
            throw new RuntimeException(errMsg, e);
        }
    }

    @Handler("post")
    public void post(IMessage msg) throws ChangeValueException, ReadValueException, ExecutionException, InterruptedException {

        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, serverAddr);
        request.headers().set(HttpHeaders.Names.HOST, "localhost");
        request.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/json");

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

        ByteBuf bbuf = Unpooled.copiedBuffer(requestBody.toString(), StandardCharsets.UTF_8);
        request.headers().set(HttpHeaders.Names.CONTENT_LENGTH, bbuf.readableBytes());
        request.content().clear().writeBytes(bbuf);
        request.headers().set(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);
        client.send(request);

        postResponseDataF.inject(msg, responseBodyBuf);
    }
}
