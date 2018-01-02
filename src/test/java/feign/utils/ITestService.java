package feign.utils;

import feign.Headers;
import feign.RequestLine;
import reactor.core.publisher.Mono;

/**
 * @author haharonof (on 02/01/2018).
 */
public interface ITestService {

    @Headers("Content-Type: application/json")
    @RequestLine("GET /get")
    Mono<String> get();

    @Headers("Content-Type: application/json")
    @RequestLine("POST /post")
    Mono<String> post();

    @Headers("Content-Type: application/json")
    @RequestLine("PUT /put")
    Mono<String> put();

    @Headers("Content-Type: application/json")
    @RequestLine("OPTIONS /options")
    Mono<String> options();

    @Headers("Content-Type: application/json")
    @RequestLine("PATCH /patch")
    Mono<String> patch();

    @Headers("Content-Type: application/json")
    @RequestLine("DELETE /delete")
    Mono<String> delete();
}
