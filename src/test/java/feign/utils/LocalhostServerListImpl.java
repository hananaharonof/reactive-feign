package feign.utils;

import com.netflix.client.config.IClientConfig;
import com.netflix.config.ConfigurationManager;
import com.netflix.loadbalancer.AbstractServerList;
import com.netflix.loadbalancer.Server;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author haharonof (on 02/01/2018).
 */
public class LocalhostServerListImpl extends AbstractServerList {

    private List<Server> servers;

    @Override
    public void initWithNiwsConfig(IClientConfig clientConfig) {
        List<Integer> ports = (List<Integer>) clientConfig.getProperties().get("ports");
        servers = ports.stream()
                .map(p -> new Server("localhost", p))
                .collect(Collectors.toList());
    }

    @Override
    public List getInitialListOfServers() {
        return servers;
    }

    @Override
    public List getUpdatedListOfServers() {
        return servers;
    }

    public static void registerServersList() {
        ConfigurationManager.getConfigInstance().setProperty(
                "localhost.ribbon.NIWSServerListClassName", LocalhostServerListImpl.class.getName());
    }

}
