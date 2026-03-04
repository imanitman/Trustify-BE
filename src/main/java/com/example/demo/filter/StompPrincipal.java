package com.example.demo.filter;
import java.io.Serializable;
import java.security.Principal;

public class StompPrincipal implements Principal, Serializable {

    private static final long serialVersionUID = 1L;
    private final String name;
    public StompPrincipal(String name) {
        this.name = name;
    }
    @Override
    public String getName() {
        return name;
    }
}
