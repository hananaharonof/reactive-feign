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

import com.netflix.client.ClientFactory;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;

/**
 * Modified copy (01/01/2018) of {@link LBClientFactory}.
 */
public interface ReactiveLBClientFactory {
    ReactiveLBClient create(String clientName);

    final class Default implements ReactiveLBClientFactory {
        @Override
        public ReactiveLBClient create(String clientName) {
            IClientConfig config = ClientFactory.getNamedConfig(clientName);
            ILoadBalancer lb = ClientFactory.getNamedLoadBalancer(clientName);
            return ReactiveLBClient.create(lb, config);
        }
    }
}
