package com.newrelic.fit.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

public class JsonFlattenFunction implements Function<String, String> {

    private static final Logger log =
            LoggerFactory.getLogger(JsonFlattenFunction.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String PREFIX_SEPARATOR = "_";

    @Override
    public String apply(String jsonMessage) {
        String flatJson;
        try {
            flatJson = flatten(jsonMessage);
        } catch (IOException e) {
            log.error("Error transforming json message. Error: {}", e.getMessage());
            return null;
        }
        return flatJson;
    }

    private String flatten(String jsonMessage) throws IOException {

        JsonNode rootNode = mapper.readTree(jsonMessage);
        ObjectNode objectNode = mapper.createObjectNode();

        ObjectNode flat = flatten(rootNode, objectNode, "");

        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(flat);
    }

    private ObjectNode flatten(JsonNode jsonNode, ObjectNode objectNode, String keyPrefix) {
        Iterator<Map.Entry<String, JsonNode>> iterator = jsonNode.fields();

        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> currentNode = iterator.next();

            JsonNode nodeValue = currentNode.getValue();

            log.debug("current node type {}, key: {}, value: {}", currentNode.getValue().getNodeType(), currentNode.getKey(), currentNode.getValue());

            if (nodeValue.getNodeType() == JsonNodeType.STRING ||
                    nodeValue.getNodeType() == JsonNodeType.BOOLEAN ||
                    nodeValue.getNodeType() == JsonNodeType.NUMBER ||
                    nodeValue.getNodeType() == JsonNodeType.NULL) {

                objectNode.set(keyPrefix + currentNode.getKey(), nodeValue);

            } else if (nodeValue.getNodeType() == JsonNodeType.OBJECT) {
                flatten(currentNode.getValue(), objectNode, keyPrefix + currentNode.getKey() + PREFIX_SEPARATOR);
            } else if (nodeValue.getNodeType() == JsonNodeType.ARRAY) {
                Iterator<JsonNode> arrayNode = currentNode.getValue().elements();

                int arrayIndex = 0;

                while (arrayNode.hasNext()) {
                    JsonNode next = arrayNode.next();
                    log.debug("processing array element: {}" + next.toString());

                    if (next.getNodeType() == JsonNodeType.OBJECT) {
                        flatten(next, objectNode, keyPrefix + currentNode.getKey() + "_" + arrayIndex++ + PREFIX_SEPARATOR);
                    } else {
                        objectNode.set(keyPrefix + currentNode.getKey() + "_" + (arrayIndex++) + PREFIX_SEPARATOR, next);
                    }

                }
            } else {
                log.debug("Unhandled node type: " + nodeValue.getNodeType().name());
            }
        }
        return objectNode;
    }
}
