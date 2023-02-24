package org.cardanofoundation.hydra.client.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ZonedDateTimeDeserializer extends JsonDeserializer<ZonedDateTime> {

    @Override
    public ZonedDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        LocalDate localDate = LocalDate.parse(
                jsonParser.getText(),
                DateTimeFormatter.ISO_LOCAL_DATE);

        return localDate.atStartOfDay(ZoneOffset.UTC);
    }

}