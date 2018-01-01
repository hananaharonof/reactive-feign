/*
*
* Copyright 2013 Netflix, Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
*/
package com.netflix.client;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.LoadBalancerContext;
import com.netflix.loadbalancer.reactive.LoadBalancerCommand;
import reactor.core.publisher.Mono;
import rx.Observable;
import rx.RxReactiveStreams;

import java.net.URI;

/**
 * Modified copy (01/01/2018) of {@link AbstractLoadBalancerAwareClient}.
 */
public abstract class AbstractReactiveLoadBalancerAwareClient<S extends ClientRequest, T> extends LoadBalancerContext implements IReactiveClient<S, T>, IClientConfigAware {

    public AbstractReactiveLoadBalancerAwareClient(ILoadBalancer lb) {
        super(lb);
    }

    public AbstractReactiveLoadBalancerAwareClient(ILoadBalancer lb, IClientConfig clientConfig) {
        super(lb, clientConfig);
    }

    public Mono<T> executeWithLoadBalancer(S request) throws ClientException {
        return executeWithLoadBalancer(request, null);
    }

    public Mono<T> executeWithLoadBalancer(final S request, final IClientConfig requestConfig) throws ClientException {
        LoadBalancerCommand<T> command = buildLoadBalancerCommand(request, requestConfig);

        try {
            Observable<T> observable = command.submit(
                    server -> {
                        URI finalUri = reconstructURIWithServer(server, request.getUri());
                        S requestForServer = (S) request.replaceUri(finalUri);
                        try {
                            Mono<T> mono = AbstractReactiveLoadBalancerAwareClient.this.executeReactive(requestForServer, requestConfig);
                            return RxReactiveStreams.toObservable(mono);
                        } catch (Exception e) {
                            return Observable.error(e);
                        }
                    });
            return Mono.from(RxReactiveStreams.toPublisher(observable));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
    
    public abstract RequestSpecificRetryHandler getRequestSpecificRetryHandler(S request, IClientConfig requestConfig);

    protected LoadBalancerCommand<T> buildLoadBalancerCommand(final S request, final IClientConfig config) {
		RequestSpecificRetryHandler handler = getRequestSpecificRetryHandler(request, config);
		LoadBalancerCommand.Builder<T> builder = LoadBalancerCommand.<T>builder()
				.withLoadBalancerContext(this)
				.withRetryHandler(handler)
				.withLoadBalancerURI(request.getUri());
		customizeLoadBalancerCommandBuilder(request, config, builder);
		return builder.build();
	}

	protected void customizeLoadBalancerCommandBuilder(final S request, final IClientConfig config,
			final LoadBalancerCommand.Builder<T> builder) {
		// do nothing by default, give a chance to its derived class to customize the builder
	}
}


