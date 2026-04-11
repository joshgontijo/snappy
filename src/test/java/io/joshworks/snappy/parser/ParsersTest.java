package io.joshworks.snappy.parser;

import io.joshworks.snappy.parser.parser.ExactTypeParser;
import io.joshworks.snappy.parser.parser.SubTypeWildcardParser;
import io.joshworks.snappy.parser.parser.WildcardParser;
import io.joshworks.snappy.http.MediaType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by Josh Gontijo on 4/30/17.
 */
public class ParsersTest {

    @Before
    public void cleanup() {
        Parsers.available.clear();
    }

    @Test
    public void mostCompatibleFirst() {
        ExactTypeParser exact = new ExactTypeParser();
        SubTypeWildcardParser subTypeWildcard = new SubTypeWildcardParser();
        WildcardParser wildcard = new WildcardParser();

        Parsers.register(MediaType.valueOf("text/*"), subTypeWildcard);
        Parsers.register(MediaType.WILDCARD_TYPE, wildcard);
        Parsers.register(MediaType.TEXT_PLAIN_TYPE, exact);//register as last

        Parser parser = Parsers.getParser(MediaType.TEXT_PLAIN_TYPE);
        assertEquals(exact, parser);
    }

    @Test
    public void mostCompatibleFirst_withMultipleTypes() {
        ExactTypeParser exact = new ExactTypeParser();
        SubTypeWildcardParser subTypeWildcard = new SubTypeWildcardParser();
        WildcardParser wildcard = new WildcardParser();
        JsonParser jsonParser = new JsonParser();

        Parsers.register(MediaType.APPLICATION_JSON_TYPE, jsonParser);
        Parsers.register(MediaType.valueOf("text/*"), subTypeWildcard);
        Parsers.register(MediaType.WILDCARD_TYPE, wildcard);
        Parsers.register(MediaType.TEXT_PLAIN_TYPE, exact);//register as last

        Parser parser = Parsers.getParser(MediaType.TEXT_PLAIN_TYPE);
        assertEquals(exact, parser);
    }

    @Test
    public void with_charset() {
        JsonParser jsonParser = new JsonParser();

        Parsers.register(MediaType.APPLICATION_JSON_TYPE, jsonParser);

        Parser parser = Parsers.getParser(MediaType.valueOf("application/json; charset=utf-8"));
        assertEquals(jsonParser, parser);
    }

    @Test
    public void multiple_types() {
        JsonParser jsonParser = new JsonParser();
        PlainTextParser plainTextParser = new PlainTextParser();

        Parsers.register(MediaType.APPLICATION_JSON_TYPE, jsonParser);
        Parsers.register(MediaType.TEXT_PLAIN_TYPE, plainTextParser);

        assertEquals(jsonParser, Parsers.getParser(MediaType.valueOf("application/json")));
        assertEquals(plainTextParser, Parsers.getParser(MediaType.valueOf("text/plain")));
    }

    @Test
    public void multiple_types_withWildcard() {
        JsonParser jsonParser = new JsonParser();
        PlainTextParser plainTextParser = new PlainTextParser();
        SubTypeWildcardParser subTypeWildcard = new SubTypeWildcardParser();

        Parsers.register(MediaType.APPLICATION_JSON_TYPE, jsonParser);
        Parsers.register(MediaType.TEXT_PLAIN_TYPE, plainTextParser);
        Parsers.register(MediaType.valueOf("text/*"), subTypeWildcard);

        assertEquals(jsonParser, Parsers.getParser(MediaType.valueOf("application/json")));
        assertEquals(plainTextParser, Parsers.getParser(MediaType.valueOf("text/plain")));
        assertEquals(subTypeWildcard, Parsers.getParser(MediaType.valueOf("text/YOLO")));
    }
}