package io.joshworks.snappy.it.util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Created by Josh Gontijo on 10/26/17.
 */
public class Utils {

    public static void closeStream(InputStream inputStream) {
        try {
            if (inputStream != null) {
                char[] buffer = new char[1024];
                try (Reader in = new InputStreamReader(inputStream, "UTF-8")) {
                    int read = 0;
                    while ((read = in.read(buffer)) > 0) {
                        //do nothing
                    }
                }
                inputStream.close();
            }

        } catch (Exception ex) {

        }
    }

    public static String toString(InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is, "UTF-8").useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
