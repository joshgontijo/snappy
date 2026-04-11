/*
 * Copyright 2017 Josue Gontijo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.joshworks.snappy.parser;

import io.joshworks.snappy.http.MediaType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by josh on 3/13/17.
 */
public class MediaTypesTest {

    @Test
    public void withCharset() throws Exception {
        String type = "application/json; charset=UTF-8";
        MediaType result = MediaType.valueOf(type);
        assertEquals("application", result.getType());
        assertEquals("json", result.getSubtype());
        assertEquals(1, result.getParameters().size());
        assertEquals("UTF-8", result.getParameters().get("charset"));
        assertEquals(type, result.toString());
    }

    @Test
    public void multipleParameters() throws Exception {
        String type = "application/json; charset=UTF-8 q=1 qs=2";
        MediaType result = MediaType.valueOf(type);
        assertEquals("application", result.getType());
        assertEquals("json", result.getSubtype());
        assertEquals(3, result.getParameters().size());
        assertEquals("UTF-8", result.getParameters().get("charset"));
        assertEquals("1", result.getParameters().get("q"));
        assertEquals("2", result.getParameters().get("qs"));
        assertEquals(type, result.toString());
    }

    @Test
    public void withoutCharset() throws Exception {
        String type = "application/json";
        MediaType result = MediaType.valueOf(type);
        assertEquals("application", result.getType());
        assertEquals("json", result.getSubtype());
        assertEquals(type, result.toString());
    }

    @Test
    public void mimeMapping() throws Exception {
        String type = "json";
        MediaType result = MediaType.valueOf(type);
        assertEquals("application", result.getType());
        assertEquals("json", result.getSubtype());
        assertEquals("application/json", result.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidMime() throws Exception {
        String type = "invalid123";
        MediaType.valueOf(type);
    }

    @Test(expected = IllegalArgumentException.class)
    public void multipleSlash() throws Exception {
        String type = "invalid//123";
        MediaType.valueOf(type);
    }

    @Test(expected = IllegalArgumentException.class)
    public void multipleSlash_beginning() throws Exception {
        String type = "/invalid/123";
        MediaType.valueOf(type);
    }

    @Test(expected = IllegalArgumentException.class)
    public void multipleSlash_end() throws Exception {
        String type = "invalid/123/";
        MediaType.valueOf(type);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullMime() throws Exception {
        MediaType.valueOf(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyMime() throws Exception {
        MediaType.valueOf("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void whitespaceMime() throws Exception {
        MediaType.valueOf(" ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void forwardSlash() throws Exception {
        MediaType.valueOf("/");
    }

    @Test(expected = IllegalArgumentException.class)
    public void forwardSlashs() throws Exception {
        MediaType.valueOf("a////s");
    }

    @Test
    public void subtypeWildcard() throws Exception {
        String type = "text/*";
        MediaType result = MediaType.valueOf(type);
        assertEquals("text", result.getType());
        assertEquals("*", result.getSubtype());
    }

    @Test
    public void isCompatible_sameType() throws Exception {
        String type = "text/plain";
        MediaType mediaType = MediaType.valueOf(type);
        assertTrue(mediaType.isCompatible(MediaType.valueOf(type)));
    }

    @Test
    public void isCompatible_subtype_wildcard() throws Exception {
        String type = "text/plain";
        MediaType mediaType = MediaType.valueOf(type);
        assertTrue(mediaType.isCompatible(MediaType.valueOf("text/*")));
    }

    @Test
    public void isCompatible_all_wildcard() throws Exception {
        String type = "text/plain";
        MediaType mediaType = MediaType.valueOf(type);
        assertTrue(mediaType.isCompatible(MediaType.valueOf("*/*")));
    }

    @Test
    public void isCompatible_all_wildcard_input() throws Exception {
        String type = "*/*";
        MediaType mediaType = MediaType.valueOf(type);
        assertTrue(mediaType.isCompatible(MediaType.valueOf("text/plain")));
    }

    @Test
    public void isCompatible_subtype_wildcard_input() throws Exception {
        String type = "*/*";
        MediaType mediaType = MediaType.valueOf(type);
        assertTrue(mediaType.isCompatible(MediaType.valueOf("text/*")));
    }

}