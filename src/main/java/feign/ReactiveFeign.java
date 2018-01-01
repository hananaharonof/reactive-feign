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
package feign;

import feign.codec.Encoder;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

import static feign.Util.checkNotNull;
import static feign.Util.isDefault;
import static feign.Utils.getReactiveParameterizedType;

/**
 * Modified copy (01/01/2018) of {@link Feign}.
 */
public class ReactiveFeign extends Feign {
    private final ParseHandlersByName targetToHandlersByName;
    private final InvocationHandlerFactory factory;

    private ReactiveFeign(
            final ParseHandlersByName targetToHandlersByName,
            final InvocationHandlerFactory factory) {
        this.targetToHandlersByName = targetToHandlersByName;
        this.factory = factory;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T newInstance(Target<T> target) {
        final Map<String, InvocationHandlerFactory.MethodHandler> nameToHandler = targetToHandlersByName.apply(target);
        final Map<Method, InvocationHandlerFactory.MethodHandler> methodToHandler = new LinkedHashMap<>();
        final List<DefaultMethodHandler> defaultMethodHandlers = new LinkedList<>();

        for (final Method method : target.type().getMethods()) {
            if (isDefault(method)) {
                final DefaultMethodHandler handler = new DefaultMethodHandler(method);
                defaultMethodHandlers.add(handler);
                methodToHandler.put(method, handler);
            } else {
                methodToHandler.put(method, nameToHandler.get(
                        Feign.configKey(target.type(), method)));
            }
        }

        final InvocationHandler handler = factory.create(target, methodToHandler);
        T proxy = (T) Proxy.newProxyInstance(
                target.type().getClassLoader(),
                new Class<?>[]{target.type()}, handler);

        for (final DefaultMethodHandler defaultMethodHandler :
                defaultMethodHandlers) {
            defaultMethodHandler.bindTo(proxy);
        }

        return proxy;
    }

    public static final class Builder extends Feign.Builder {
        private final List<RequestInterceptor> requestInterceptors = new ArrayList<>();
        private Logger.Level logLevel = Logger.Level.NONE;
        private Contract contract = new Contract.Default();
        private ReactiveClient reactiveClient = new ReactiveClient.Default();
        private Logger logger = new Logger.NoOpLogger();
        private Request.Options options = new Request.Options();
        private InvocationHandlerFactory invocationHandlerFactory = new InvocationHandlerFactory.Default();

        public Builder reactiveClient(final ReactiveClient reactiveClient) {
            this.reactiveClient = reactiveClient;
            return this;
        }

        @Override
        public Builder client(final Client client) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Builder invocationHandlerFactory(InvocationHandlerFactory invocationHandlerFactory) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Builder logLevel(final Logger.Level logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        @Override
        public Builder contract(final Contract contract) {
            this.contract = contract;
            return this;
        }

        @Override
        public Builder retryer(final Retryer retryer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Builder logger(final Logger logger) {
            this.logger = logger;
            return this;
        }

        @Override
        public Builder options(final Request.Options options) {
            this.options = options;
            return this;
        }

        @Override
        public Builder requestInterceptor(
                final RequestInterceptor requestInterceptor) {
            this.requestInterceptors.add(requestInterceptor);
            return this;
        }

        @Override
        public Builder requestInterceptors(
                final Iterable<RequestInterceptor> requestInterceptors) {
            this.requestInterceptors.clear();
            for (RequestInterceptor requestInterceptor : requestInterceptors) {
                this.requestInterceptors.add(requestInterceptor);
            }
            return this;
        }

        @Override
        public <T> T target(final Class<T> apiType, final String url) {
            return target(new Target.HardCodedTarget<>(apiType, url));
        }

        @Override
        public <T> T target(final Target<T> target) {
            return build().newInstance(target);
        }

        @Override
        public ReactiveFeign build() {
            checkNotNull(this.reactiveClient, "Reactor client wasn't provided in Reactive-Feign builder");
            final ReactiveMethodHandler.Factory reactiveMethodHandlerFactory =
                    new ReactiveMethodHandler.Factory(reactiveClient, requestInterceptors, logger, logLevel);

            final ParseHandlersByName handlersByName =
                    new ParseHandlersByName(contract, options, reactiveMethodHandlerFactory);

            return new ReactiveFeign(handlersByName, invocationHandlerFactory);
        }
    }

    private static final class ParseHandlersByName {
        private final Contract contract;
        private final Request.Options options;
        private final ReactiveMethodHandler.Factory reactiveFactory;
        private final Encoder encoder;

        ParseHandlersByName(
                final Contract contract,
                final Request.Options options,
                final ReactiveMethodHandler.Factory reactiveFactory) {

            this.contract = contract;
            this.options = options;
            this.reactiveFactory = reactiveFactory;
            this.encoder = new Encoder.Default();
        }

        Map<String, InvocationHandlerFactory.MethodHandler> apply(final Target key) {
            final List<MethodMetadata> metadata = contract
                    .parseAndValidatateMetadata(key.type());

            final Map<String, InvocationHandlerFactory.MethodHandler> result = new LinkedHashMap<>();

            for (final MethodMetadata md : metadata) {
                BuildTemplateByResolvingArgs buildTemplate;

                if (!md.formParams().isEmpty() && md.template().bodyTemplate() == null) {
                    buildTemplate = new BuildTemplateByResolvingArgs.BuildFormEncodedTemplateFromArgs(md, encoder);
                } else if (md.bodyIndex() != null) {
                    buildTemplate = new BuildTemplateByResolvingArgs.BuildEncodedTemplateFromArgs(md, encoder);
                } else {
                    buildTemplate = new BuildTemplateByResolvingArgs(md);
                }

                result.put(md.configKey(), getMethodHandler(key, buildTemplate, md));
            }

            return result;
        }

        private InvocationHandlerFactory.MethodHandler getMethodHandler(final Target key,
                                                                        final BuildTemplateByResolvingArgs buildTemplate,
                                                                        final MethodMetadata metadata) {

            ParameterizedTypeImpl returnType = getReactiveParameterizedType(metadata.returnType());
            if (returnType == null) {
                throw new FeignException("Reactive-Feign supports only the Reactor type Mono.");
            }

            return reactiveFactory.create(key, metadata, buildTemplate, options);
        }
    }
}
