package feign.utils;

import com.netflix.client.ClientFactory;
import com.netflix.client.RetryHandler;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.DynamicServerListLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import feign.ribbon.ReactiveLBClient;
import feign.ribbon.ReactiveLBClientFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by dkolog on 4/17/16.
 */
public class CustomReactiveLBClientFactory implements ReactiveLBClientFactory {

    private AtomicBoolean init;
    private List<Integer> ports;

    public CustomReactiveLBClientFactory(List<Integer> ports) {
        this.init = new AtomicBoolean(true);
        this.ports = ports;
    }

    @Override
    public ReactiveLBClient create(String clientName) {
        IClientConfig config = ClientFactory.getNamedConfig(clientName);
        config.getProperties().put("ports", this.ports);
        ILoadBalancer lb = ClientFactory.getNamedLoadBalancer(clientName);

        ReactiveLBClient lbClient = ReactiveLBClient.create(lb, config);

        if (init.compareAndSet(true,false)) {
            ((DynamicServerListLoadBalancer) lb).updateListOfServers();
        }

        return lbClient;
    }
}
