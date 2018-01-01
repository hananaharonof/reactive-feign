/*
 * Copyright 2018 Hanan Aharonof.
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
package feign;

import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;

/**
 * @author Hanan Aharonof.
 */
public interface ReactiveClient extends Client {

    Mono<ClientResponse> executeReactive(Request request, Request.Options options) throws IOException;

    class Default implements ReactiveClient {
        private WebClient reactiveClient;

        public Default() {
            this.reactiveClient = WebClient.create();
        }

        @Override
        public Response execute(Request request, Request.Options options) throws IOException {
            throw new UnsupportedOperationException();
        }

        public Mono<ClientResponse> executeReactive(Request request, Request.Options options) throws IOException {
            WebClient.RequestHeadersUriSpec<?> spec = createSpec(request);
            if (spec == null) {
                return Mono.error(new UnsupportedOperationException());
            }
            request.headers().forEach((key, value) -> spec.header(key, value.toArray(new String[value.size()])));
            spec.acceptCharset(request.charset());
            spec.uri(request.url());
            return spec.exchange();
        }

        private WebClient.RequestHeadersUriSpec<?> createSpec(Request request) {
            if (HttpMethod.GET.toString().equalsIgnoreCase(request.method())) {
                return reactiveClient.get();
            }
            if (HttpMethod.POST.toString().equalsIgnoreCase(request.method())) {
                return reactiveClient.post();
            }
            if (HttpMethod.PUT.toString().equalsIgnoreCase(request.method())) {
                return reactiveClient.put();
            }
            if (HttpMethod.DELETE.toString().equalsIgnoreCase(request.method())) {
                return reactiveClient.delete();
            }
            if (HttpMethod.PATCH.toString().equalsIgnoreCase(request.method())) {
                return reactiveClient.patch();
            }
            if (HttpMethod.OPTIONS.toString().equalsIgnoreCase(request.method())) {
                return reactiveClient.options();
            }
            if (HttpMethod.HEAD.toString().equalsIgnoreCase(request.method())) {
                return reactiveClient.head();
            }
            return null;
        }
    }
}
