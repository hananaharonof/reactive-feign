/*
 * Copyright 2014 Netflix, Inc.
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

import feign.InvocationHandlerFactory.MethodHandler;
import feign.Request.Options;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import reactor.core.publisher.Mono;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.util.List;

import static feign.Util.checkNotNull;
import static feign.Utils.getReactiveParameterizedType;

/**
 * Modified copy (01/01/2018) of {@link SynchronousMethodHandler}.
 */
final class ReactiveMethodHandler implements MethodHandler {

    private final MethodMetadata metadata;
    private final Target<?> target;
    private final ReactiveClient client;
    private final List<RequestInterceptor> requestInterceptors;
    private final Logger logger;
    private final Logger.Level logLevel;
    private final RequestTemplate.Factory buildTemplateFromArgs;
    private final Options options;

    private ReactiveMethodHandler(Target<?> target, ReactiveClient client,
                                  List<RequestInterceptor> requestInterceptors, Logger logger,
                                  Logger.Level logLevel, MethodMetadata metadata,
                                  RequestTemplate.Factory buildTemplateFromArgs, Options options) {

        this.target = checkNotNull(target, "target");
        this.client = checkNotNull(client, "client for %s", target);
        this.requestInterceptors =
                checkNotNull(requestInterceptors, "requestInterceptors for %s", target);
        this.logger = checkNotNull(logger, "logger for %s", target);
        this.logLevel = checkNotNull(logLevel, "logLevel for %s", target);
        this.metadata = checkNotNull(metadata, "metadata for %s", target);
        this.buildTemplateFromArgs = checkNotNull(buildTemplateFromArgs, "metadata for %s", target);
        this.options = checkNotNull(options, "options for %s", target);
    }

    @Override
    public Object invoke(Object[] argv) throws Throwable {
        RequestTemplate template = buildTemplateFromArgs.create(argv);
        return reactiveExecuteAndDecode(template);
    }

    private Mono<?> reactiveExecuteAndDecode(RequestTemplate template) throws Throwable {
        ParameterizedTypeImpl returnType = getReactiveParameterizedType(metadata.returnType());
        if (returnType == null) {
            throw new FeignException("Reactive-Feign only supports Reactor type Mono.");
        }

        final Request request = targetRequest(template);
        if (logLevel != Logger.Level.NONE) {
            logger.logRequest(metadata.configKey(), logLevel, request);
        }

        return client
                .executeReactive(request, this.options)
                .flatMap(r -> {
                    if (r.statusCode().is4xxClientError()) {
                        return Mono.error(new HttpClientErrorException(r.statusCode()));
                    }
                    if (r.statusCode().is5xxServerError()) {
                        return Mono.error(new HttpServerErrorException(r.statusCode()));
                    }
                    return r.bodyToMono(ParameterizedTypeReference.forType(returnType.getActualTypeArguments()[0]));
                });
    }

    private Request targetRequest(RequestTemplate template) {
        for (RequestInterceptor interceptor : requestInterceptors) {
            interceptor.apply(template);
        }
        return target.apply(new RequestTemplate(template));
    }

    static class Factory {

        private final ReactiveClient client;
        private final List<RequestInterceptor> requestInterceptors;
        private final Logger logger;
        private final Logger.Level logLevel;

        Factory(ReactiveClient client,
                List<RequestInterceptor> requestInterceptors,
                Logger logger,
                Logger.Level logLevel) {

            this.client = checkNotNull(client, "client");
            this.requestInterceptors = checkNotNull(requestInterceptors, "requestInterceptors");
            this.logger = checkNotNull(logger, "logger");
            this.logLevel = checkNotNull(logLevel, "logLevel");
        }

        MethodHandler create(Target<?> target,
                                    MethodMetadata md,
                                    RequestTemplate.Factory buildTemplateFromArgs,
                                    Options options) {

            return new ReactiveMethodHandler(
                    target, client, requestInterceptors, logger, logLevel, md, buildTemplateFromArgs, options);
        }
    }
}
