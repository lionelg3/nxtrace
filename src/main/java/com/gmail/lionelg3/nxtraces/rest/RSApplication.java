package com.gmail.lionelg3.nxtraces.rest;

import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * Collect mail traces from Unix systems
 *
 * @author Lionel G (lionelg3@gmail.com)
 *         Date: 17/12/15, Time: 00:08
 */
public class RSApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> clazz = new HashSet<Class<?>>();

        clazz.add(PostTraceService.class);
        clazz.add(PingService.class);
        clazz.add(QueryService.class);

        return clazz;
    }
}
