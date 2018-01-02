package feign.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author haharonof (on 02/01/2018).
 */
@SpringBootApplication
@RestController
@RequestMapping("/dummy")
public class DummyServer {

    @Autowired
    private Environment environment;

    @RequestMapping(path = "/get", method = RequestMethod.GET)
    public String get() {
        return response(RequestMethod.GET);
    }

    @RequestMapping(path = "/post", method = RequestMethod.POST)
    public String post() {
        return response(RequestMethod.POST);
    }

    @RequestMapping(path = "/put", method = RequestMethod.PUT)
    public String put() {
        return response(RequestMethod.PUT);
    }

    @RequestMapping(path = "/options", method = RequestMethod.OPTIONS)
    public String options() {
        return response(RequestMethod.OPTIONS);
    }

    @RequestMapping(path = "/patch", method = RequestMethod.PATCH)
    public String patch() {
        return response(RequestMethod.PATCH);
    }

    @RequestMapping(path = "/delete", method = RequestMethod.DELETE)
    public String delete() {
        return response(RequestMethod.DELETE);
    }

    private String response(RequestMethod method) {
        return String.format("%s-%s", method.name(), environment.getProperty("server.port"));
    }
}
