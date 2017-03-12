package io.joshworks.microserver.parser;

import com.google.gson.Gson;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by josh on 3/6/17.
 */
public class JsonParser implements Parser {

    private final Gson gson = new Gson();

    @Override
    public <T> T readValue(String value, Class<T> valueType) throws Exception {
        return gson.fromJson(value, valueType);
    }

    @Override
    public String writeValue(Object input) throws Exception {
        return gson.toJson(input);
    }

    @Override
    public Set<String> mediaTypes() {
        return new HashSet<>(Collections.singletonList("application/json"));
    }
}
