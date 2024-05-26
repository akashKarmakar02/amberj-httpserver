package com.amberj.net.template;

import com.hubspot.jinjava.Jinjava;

import java.util.Map;

public class JinjavaTemplating {
    Jinjava templateEngine;

    public JinjavaTemplating() {
        templateEngine = new Jinjava();
    }

    public String parse(String templateString, Map<String, ?> context) {
        return templateEngine.render(templateString, context);
    }
}
