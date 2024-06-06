package com.amberj.net.httpserver;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MultipartFormDataParser {

    public static Map<String, Object> parseMultipartForm(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.startsWith("multipart/form-data")) {
            throw new IllegalArgumentException("Request is not multipart/form-data");
        }

        String boundary = extractBoundary(contentType);
        byte[] requestBody = readRequestBody(exchange);

        return parseParts(new String(requestBody, StandardCharsets.UTF_8), boundary);
    }

    private static String extractBoundary(String contentType) {
        String[] parts = contentType.split(";");
        for (String part : parts) {
            part = part.trim();
            if (part.startsWith("boundary=")) {
                return part.substring(9);
            }
        }
        throw new IllegalArgumentException("Content-Type does not contain boundary");
    }

    private static Map<String, Object> parseParts(String body, String boundary) throws IOException {
        Map<String, Object> formData = new HashMap<>();
        String[] parts = body.split("--" + boundary);

        for (String part : parts) {
            if (part.trim().isEmpty() || part.equals("--")) {
                continue;
            }

            int headersEndIndex = part.indexOf("\r\n\r\n");
            if (headersEndIndex == -1) {
                continue;
            }

            String headersPart = part.substring(0, headersEndIndex);
            String bodyPart = part.substring(headersEndIndex + 4).trim();

            String[] headers = headersPart.split("\r\n");
            String fieldName = null;
            String fileName = null;
            for (String header : headers) {
                if (header.startsWith("Content-Disposition:")) {
                    String[] elements = header.split(";");
                    for (String element : elements) {
                        element = element.trim();
                        if (element.startsWith("name=\"")) {
                            fieldName = element.substring(6, element.length() - 1);
                        } else if (element.startsWith("filename=\"")) {
                            fileName = element.substring(10, element.length() - 1);
                        }
                    }
                }
            }

            if (fieldName != null) {
                if (fileName == null) {
                    // This is a form field
                    formData.put(fieldName, bodyPart);
                } else {
                    // This is a file field
                    // Save the file with its original name
                    File file = saveFile(bodyPart, fileName);
                    formData.put(fieldName, file);
                }
            }
        }

        return formData;
    }

    private static File saveFile(String fileContent, String fileName) throws IOException {
        File file = new File(System.getProperty("java.io.tmpdir"), fileName);
        file.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(fileContent.getBytes());
        }
        return file;
    }

    private static byte[] readRequestBody(HttpExchange exchange) throws IOException {
        InputStream inputStream = exchange.getRequestBody();
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toByteArray();
    }
}
