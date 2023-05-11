package org.cardanofoundation.hydra.core.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.cardanofoundation.hydra.core.HydraException;
import org.cardanofoundation.hydra.core.model.Party;
import org.cardanofoundation.hydra.core.model.UTXO;

import java.util.List;
import java.util.Map;

public class MoreJson {

    static ObjectMapper MAPPER;

    static {
        MAPPER = new ObjectMapper();
        MAPPER.findAndRegisterModules();
        MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        MAPPER.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        MAPPER.registerModule(new JavaTimeModule());
    }

    public static String serialise(Object o) {
        try {
            return MAPPER.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new HydraException("Unable to serialise json", e);
        }
    }

    public static JsonNode read(String json) throws HydraException {
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new HydraException("Unable to deserialise json", e);
        }
    }

    public static Map<String, UTXO> convertUTxOMap(JsonNode o) throws HydraException {
        return MAPPER.convertValue(o, new TypeReference<Map<String, UTXO>>(){});
    }

    public static List<Party> convertPartiesList(JsonNode o) throws HydraException {
        return MAPPER.convertValue(o, new TypeReference<List<Party>>(){});
    }

    public static <T> T convert(JsonNode o, Class<T> clazz) throws HydraException {
        return MAPPER.convertValue(o, clazz);
    }

}
