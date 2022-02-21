package com.alibaba.nacossync.extension.support;

import com.ecwid.consul.SingleUrlParameters;
import com.ecwid.consul.UrlParameters;
import com.ecwid.consul.Utils;
import com.ecwid.consul.transport.DefaultHttpTransport;
import com.ecwid.consul.transport.HttpRequest;
import com.ecwid.consul.transport.HttpResponse;
import com.ecwid.consul.transport.HttpTransport;
import com.ecwid.consul.v1.ConsulRawClient;
import com.ecwid.consul.v1.OperationException;
import com.ecwid.consul.v1.Request;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.NewService;

import java.util.Objects;

import static com.ecwid.consul.json.GsonFactory.*;

/**
 * @description: makePutRequest 支持动态增加Header 参数
 * @author: wenhui.bai
 * @email: wenhui.bai@shihengtech.com
 * @create: 2022-02-21 14:56
 **/
public class ConsulRawClientEnhance extends ConsulRawClient {


    private static final HttpTransport DEFAULT_HTTP_TRANSPORT = new DefaultHttpTransport();

    private final String agentAddress;


    public ConsulRawClientEnhance(String agentHost, int agentPort) {
        super(agentHost, agentPort);

        // check that agentHost has scheme or not
        String agentHostLowercase = agentHost.toLowerCase();
        if (!agentHostLowercase.startsWith("https://") && !agentHostLowercase.startsWith("http://")) {
            // no scheme in host, use default 'http'
            agentHost = "http://" + agentHost;
        }
        this.agentAddress = Utils.assembleAgentAddress(agentHost, agentPort, ConsulRawClient.DEFAULT_PATH);
    }


    @Override
    public HttpResponse makePutRequest(Request request) {

        HttpRequest httpRequest = getHttpRequest(request);

        return DEFAULT_HTTP_TRANSPORT.makePutRequest(httpRequest);
    }



    private HttpRequest getHttpRequest(Request request) {

        String url = prepareUrl(agentAddress + request.getEndpoint());
        url = Utils.generateUrl(url, request.getUrlParameters());

        String consulReqContent = request.getContent();
        NewService newService = getGson().fromJson(consulReqContent, NewService.class);

        if (Objects.nonNull(newService)) {

            String serviceInstanceIPAddress = newService.getAddress();

            HttpRequest httpRequest = HttpRequest.Builder.newBuilder()
                    .setUrl(url)
                    .setBinaryContent(request.getBinaryContent())
                    .addHeaders(Utils.createTokenMap(request.getToken()))
                    .addHeader("X-Forwarded-For" , serviceInstanceIPAddress)
                    .build();

            return httpRequest;

        }

        HttpRequest httpRequest = HttpRequest.Builder.newBuilder()
                .setUrl(url)
                .setBinaryContent(request.getBinaryContent())
                .addHeaders(Utils.createTokenMap(request.getToken()))
                .build();

        return httpRequest;
    }

    @Override
    public HttpResponse makePutRequest(String endpoint, String content, UrlParameters... urlParams) {


        NewService newService = getGson().fromJson(content, NewService.class);

        String url = prepareUrl(agentAddress + endpoint);
        url = Utils.generateUrl(url, urlParams);

        HttpRequest request = null;
        
        if(Objects.nonNull(newService)) {

             request = HttpRequest.Builder.newBuilder()
                    .setUrl(url)
                    .setContent(content)
                    .addHeader("X-Forwarded-For" , newService.getAddress())
                    .build();
        } else {

            request = HttpRequest.Builder.newBuilder()
                    .setUrl(url)
                    .setContent(content)
                    .build();
        }

        return DEFAULT_HTTP_TRANSPORT.makePutRequest(request);
    }


    /**
     * 扩展指定IP摘除
     * @param serviceId
     * @param token
     * @param serviceInstanceIPAddress
     * @return
     */
    public Response<Void> agentServiceDeregister(String serviceId, String token, String serviceInstanceIPAddress) {

        UrlParameters tokenParam = token != null ? new SingleUrlParameters("token", token) : null;

        NewService newService = new NewService();
        newService.setAddress(serviceInstanceIPAddress);
        String newServiceContent = getGson().toJson(newService);
        HttpResponse httpResponse = this.makePutRequest("/v1/agent/service/deregister/" + serviceId, newServiceContent, tokenParam);

        if (httpResponse.getStatusCode() == 200) {
            return new Response<Void>(null, httpResponse);
        } else {
            throw new OperationException(httpResponse);
        }
    }

    private String prepareUrl(String url) {
        if (url.contains(" ")) {
            return Utils.encodeUrl(url);
        } else {
            return url;
        }
    }
}
