package org.cardanofoundation.hydra.client.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.cardanofoundation.hydra.client.HydraException;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public class MoreJson {

    static ObjectMapper MAPPER;

    static {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(ZonedDateTime.class, new ZonedDateTimeDeserializer());

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);

        MAPPER = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    public static String serialise(Object o) {
        try {
            return MAPPER.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new HydraException("Unable to serialise json", e);
        }
    }

    public static <T> T toObject(String json, Class<T> clazz) throws HydraException {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new HydraException("Unable to deserialise json", e);
        }
    }

    public static JsonNode read(String json) throws HydraException {
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new HydraException("Unable to deserialise json", e);
        }
    }

    public static <T> Map<String, T> convertStringMap(JsonNode o) throws HydraException {
        return MAPPER.convertValue(o, new TypeReference<Map<String, T>>(){});
    }

    public static <T> List<T> convertList(JsonNode o) throws HydraException {
        return MAPPER.convertValue(o, new TypeReference<List<T>>(){});
    }

    public static <T> T convert(JsonNode o, Class<T> clazz) throws HydraException {
        return MAPPER.convertValue(o, clazz);
    }

}
