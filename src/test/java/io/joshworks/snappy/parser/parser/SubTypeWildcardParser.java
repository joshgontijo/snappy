package io.joshworks.snappy.parser.parser;

import io.joshworks.snappy.parser.Parser;

import java.lang.reflect.Type;

/**
 * Created by Josh Gontijo on 4/30/17.
 */
public class SubTypeWildcardParser implements Parser {
    @Override
    public <T> T readValue(String value, Class<T> valueType) {
        return null;
    }

    @Override
    public <T> T readValue(String value, Type valueType) {
        return null;
    }

    @Override
    public String writeValue(Object value) {
        return null;
    }
}
