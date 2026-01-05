package io.github.jrohila.simpleragserver;

import io.micronaut.runtime.Micronaut;

public class SimpleRagServerApplication {

    public static void main(String[] args) {
        Micronaut.build(args)
            .eagerInitSingletons(true)
            .mainClass(SimpleRagServerApplication.class)
            .start();
    }

}