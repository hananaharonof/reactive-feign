package feign;

import org.junit.Assert;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.reflect.Type;

/**
 * @author haharonof (on 02/01/2018).
 */
public class UtilsTest {

    @Test
    public void testIsMono() {
        Assert.assertTrue((Utils.isMono(Mono.class)));
        Assert.assertFalse((Utils.isMono(Publisher.class)));
        Assert.assertFalse((Utils.isMono(String.class)));
    }

    @Test
    public void getReactiveParameterizedTypeNullTest() {
        Assert.assertNull(Utils.getReactiveParameterizedType(null));
    }

    @Test
    public void getReactiveParameterizedTypeNotParameterizedTypeTest() {
        Assert.assertNull(Utils.getReactiveParameterizedType(String.class));
    }

    @Test(expected = FeignException.class)
    public void getReactiveParameterizedTypeNotMonoTest() {
        Assert.assertNull(Utils.getReactiveParameterizedType(String.class));
        Type[] types = {String.class};
        Utils.getReactiveParameterizedType(ParameterizedTypeImpl.make(Publisher.class, types, null));
    }

    @Test
    public void getReactiveParameterizedTypeTest() {
        Assert.assertNull(Utils.getReactiveParameterizedType(String.class));
        Type[] types = {String.class};
        ParameterizedTypeImpl parameterizedType = ParameterizedTypeImpl.make(Mono.class, types, null);
        Assert.assertEquals(parameterizedType, Utils.getReactiveParameterizedType(parameterizedType));
    }

}
