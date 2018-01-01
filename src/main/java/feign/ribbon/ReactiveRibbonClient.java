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

import com.netflix.client.ClientException;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import feign.ReactiveClient;
import feign.Request;
import feign.Response;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;

/**
 * Some methods on this class were copied (01/01/2018) from {@link RibbonClient}
 */
public class ReactiveRibbonClient implements ReactiveClient {

    private final ReactiveClient delegate;
    private final ReactiveLBClientFactory reactiveLbClientFactory;


    public static ReactiveRibbonClient create() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    ReactiveRibbonClient(ReactiveClient delegate, ReactiveLBClientFactory reactiveLbClientFactory) {
        this.delegate = delegate;
        this.reactiveLbClientFactory = reactiveLbClientFactory;
    }

    @Override
    public Response execute(Request request, Request.Options options) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mono<ClientResponse> executeReactive(Request request, Request.Options options) throws IOException {
        try {
            URI asUri = URI.create(request.url());
            String clientName = asUri.getHost();
            URI uriWithoutHost = cleanUrl(request.url(), clientName);
            ReactiveLBClient.RibbonRequest ribbonRequest =
                    new ReactiveLBClient.RibbonRequest(delegate, request, uriWithoutHost);

            return lbClient(clientName).executeWithLoadBalancer(ribbonRequest,
                    new FeignOptionsClientConfig(options));
        } catch (ClientException e) {
            return Mono.error(e);
        }

    }


    static URI cleanUrl(String originalUrl, String host) {
        return URI.create(originalUrl.replaceFirst(host, ""));
    }

    private ReactiveLBClient lbClient(String clientName) {
        return reactiveLbClientFactory.create(clientName);
    }


    static class FeignOptionsClientConfig extends DefaultClientConfigImpl {

        public FeignOptionsClientConfig(Request.Options options) {
            setProperty(CommonClientConfigKey.ConnectTimeout, options.connectTimeoutMillis());
            setProperty(CommonClientConfigKey.ReadTimeout, options.readTimeoutMillis());
        }

        @Override
        public void loadProperties(String clientName) {

        }

        @Override
        public void loadDefaultValues() {

        }

    }

    public static final class Builder {

        Builder() {
        }

        private ReactiveClient delegate;
        private ReactiveLBClientFactory reactiveLbClientFactory;

        public Builder delegate(ReactiveClient delegate) {
            this.delegate = delegate;
            return this;
        }

        public Builder reactiveLbClientFactory(ReactiveLBClientFactory reactiveLbClientFactory) {
            this.reactiveLbClientFactory = reactiveLbClientFactory;
            return this;
        }

        public ReactiveRibbonClient build() {
            return new ReactiveRibbonClient(
                    delegate != null ? delegate : new Default(),
                    reactiveLbClientFactory != null ? reactiveLbClientFactory : new ReactiveLBClientFactory.Default()
            );
        }
    }
}
