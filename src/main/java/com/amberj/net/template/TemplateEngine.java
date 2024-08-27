package com.amberj.net.template;

import io.pebbletemplates.pebble.PebbleEngine;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

public class TemplateEngine {
    PebbleEngine templateEngine;

    public TemplateEngine() {
        templateEngine = new PebbleEngine.Builder().build();
    }

    public String parse(String templateString, Map<String, Object> context) throws IOException {
        var compiledTemplate =  templateEngine.getTemplate(templateString);

        Writer writer = new StringWriter();

        compiledTemplate.evaluate(writer, context);

        return writer.toString();
    }
}
