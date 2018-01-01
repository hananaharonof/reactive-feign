/*
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package feign.ribbon;

import com.netflix.client.AbstractReactiveLoadBalancerAwareClient;
import com.netflix.client.ClientRequest;
import com.netflix.client.RequestSpecificRetryHandler;
import com.netflix.client.RetryHandler;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import feign.ReactiveClient;
import feign.Request;
import feign.RequestTemplate;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;

/**
 * Modified copy (01/01/2018) of {@link LBClient}.
 */
public final class ReactiveLBClient extends AbstractReactiveLoadBalancerAwareClient<ReactiveLBClient.RibbonRequest, ClientResponse> {

    private final int connectTimeout;
    private final int readTimeout;
    private final IClientConfig clientConfig;

    public static ReactiveLBClient create(ILoadBalancer lb, IClientConfig clientConfig) {
        return new ReactiveLBClient(lb, clientConfig);
    }

    ReactiveLBClient(ILoadBalancer lb, IClientConfig clientConfig) {
        super(lb, clientConfig);
        this.setRetryHandler(RetryHandler.DEFAULT);
        this.clientConfig = clientConfig;

        connectTimeout = clientConfig.get(CommonClientConfigKey.ConnectTimeout);
        readTimeout = clientConfig.get(CommonClientConfigKey.ReadTimeout);
    }

    @Override
    public Mono<ClientResponse> executeReactive(RibbonRequest request, IClientConfig configOverride) throws IOException {
        Request.Options options;
        if (configOverride != null) {
            options =
                    new Request.Options(
                            configOverride.get(CommonClientConfigKey.ConnectTimeout, connectTimeout),
                            (configOverride.get(CommonClientConfigKey.ReadTimeout, readTimeout)));
        } else {
            options = new Request.Options(connectTimeout, readTimeout);
        }
        return request.client().executeReactive(request.toRequest(), options);
    }

    @Override
    public RequestSpecificRetryHandler getRequestSpecificRetryHandler(
            RibbonRequest request, IClientConfig requestConfig) {
        if (clientConfig.get(CommonClientConfigKey.OkToRetryOnAllOperations, false)) {
            return new RequestSpecificRetryHandler(true, true, this.getRetryHandler(), requestConfig);
        }
        if (!request.toRequest().method().equals("GET")) {
            return new RequestSpecificRetryHandler(true, false, this.getRetryHandler(), requestConfig);
        } else {
            return new RequestSpecificRetryHandler(true, true, this.getRetryHandler(), requestConfig);
        }
    }

    static class RibbonRequest extends ClientRequest implements Cloneable {

        private final Request request;
        private final ReactiveClient client;

        RibbonRequest(ReactiveClient client, Request request, URI uri) {
            this.client = client;
            this.request = request;
            setUri(uri);
        }

        Request toRequest() {
            return new RequestTemplate()
                    .method(request.method())
                    .append(getUri().toASCIIString())
                    .headers(request.headers())
                    .body(request.body(), request.charset())
                    .request();
        }

        ReactiveClient client() {
            return client;
        }

        public Object clone() {
            return new RibbonRequest(client, request, getUri());
        }
    }
}
