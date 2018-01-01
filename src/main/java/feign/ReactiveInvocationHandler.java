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

import feign.InvocationHandlerFactory.MethodHandler;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import static feign.Util.checkNotNull;
import static feign.Utils.isMono;

/**
 * Modified copy (01/01/2018) of {@link ReflectiveFeign}.
 */
final class ReactiveInvocationHandler implements InvocationHandler {

    private final Target<?> target;
    private final Map<Method, MethodHandler> dispatch;

    private ReactiveInvocationHandler(final Target<?> target,
                                      final Map<Method, MethodHandler> dispatch) {
        this.target = checkNotNull(target, "target must not be null");
        this.dispatch = checkNotNull(dispatch, "dispatch must not be null");
    }

    @Override
    public Object invoke(final Object proxy,
                         final Method method,
                         final Object[] args) throws Throwable {
        switch (method.getName()) {
            case "equals":
                final Object otherHandler = args.length > 0 && args[0] != null
                        ? Proxy.getInvocationHandler(args[0])
                        : null;
                return equals(otherHandler);
            case "hashCode":
                return hashCode();
            case "toString":
                return toString();
            default:
                if (isMono(method.getReturnType())) {
                    return invokeReactiveRequestMethod(method, args);
                }

                throw new FeignException(String.format(
                        "Method %s of class %s must return Reactor type Mono.",
                        method.getName(), method.getDeclaringClass().getSimpleName()));
        }
    }

    private Mono invokeReactiveRequestMethod(final Method method, final Object[] args) {
        try {
            return (Mono) dispatch.get(method).invoke(args);
        } catch (Throwable throwable) {
            return Mono.error(throwable);
        }
    }

    @Override
    public boolean equals(final Object other) {
        if (other instanceof ReactiveInvocationHandler) {
            final ReactiveInvocationHandler otherHandler =
                    (ReactiveInvocationHandler) other;
            return this.target.equals(otherHandler.target);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return target.hashCode();
    }

    @Override
    public String toString() {
        return target.toString();
    }

    static final class Factory implements InvocationHandlerFactory {

        @Override
        public InvocationHandler create(final Target target,
                                        final Map<Method, MethodHandler> dispatch) {
            return new ReactiveInvocationHandler(target, dispatch);
        }
    }
}
