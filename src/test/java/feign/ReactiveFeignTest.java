package feign;

import feign.ribbon.ReactiveRibbonClient;
import feign.utils.CustomReactiveLBClientFactory;
import feign.utils.DummyServer;
import feign.utils.ITestService;
import feign.utils.LocalhostServerListImpl;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author haharonof (on 02/01/2018).
 */
@SpringBootTest
public class ReactiveFeignTest {

    static {
        SpringApplication.run(DummyServer.class, onPort(8080));
        SpringApplication.run(DummyServer.class, onPort(8081));
        SpringApplication.run(DummyServer.class, onPort(8082));
    }

    @Test
    public void correctEndpointTest() {
        Integer port = 8080;

        ITestService service = ReactiveFeign.builder()
                .reactiveClient(new ReactiveClient.Default())
                .target(ITestService.class, String.format("http://localhost:%s/dummy", port));

        assertResponse(RequestMethod.GET, port, service.get().block());
        assertResponse(RequestMethod.POST, port, service.post().block());
        assertResponse(RequestMethod.PATCH, port, service.patch().block());
        assertResponse(RequestMethod.PUT, port, service.put().block());
        assertResponse(RequestMethod.OPTIONS, port, service.options().block());
        assertResponse(RequestMethod.DELETE, port, service.delete().block());
    }

    @Test(expected = HttpClientErrorException.class)
    public void wrongEndpointTest() {
        ITestService service = ReactiveFeign.builder()
                .reactiveClient(new ReactiveClient.Default())
                .target(ITestService.class, "http://localhost:8080/bad");

        service.get().block();
    }

    @Test
    public void ribbonLoadBalancingTest() {
        List<Integer> ports = Arrays.asList(8080, 8081, 8082);
        LocalhostServerListImpl.registerServersList();
        ReactiveRibbonClient ribbonClient = ReactiveRibbonClient.builder()
                .reactiveLbClientFactory(new CustomReactiveLBClientFactory(ports))
                .delegate(new ReactiveClient.Default())
                .build();
        ITestService service = ReactiveFeign.builder()
                .reactiveClient(ribbonClient)
                .target(ITestService.class, "http://localhost/dummy");

        List<String> responses = new ArrayList<>();

        responses.add(service.get().block());
        responses.add(service.get().block());
        responses.add(service.get().block());

        for (Integer port : ports) {
            String expectedResponse = buildResponse(RequestMethod.GET, port);
            Assert.assertTrue(responses.contains(expectedResponse));
        }
    }

    private String buildResponse(RequestMethod expectedRequestMethod, Integer expectedPort) {
        return String.format("%s-%s", expectedRequestMethod.name(), expectedPort);
    }

    private void assertResponse(RequestMethod expectedRequestMethod, Integer expectedPort, String response) {
        Assert.assertEquals(buildResponse(expectedRequestMethod, expectedPort), response);
    }

    private static String[] onPort(int port) {
        String[] args = {"--server.port=" + port};
        return args;
    }
}
