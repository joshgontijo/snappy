package io.joshworks.snappy.http.body;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;

import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

public class MultiPartBody implements Iterable<Part> {

    private final FormData formData;

    public MultiPartBody(HttpServerExchange exchange) {
        this.formData = exchange.getAttachment(FormDataParser.FORM_DATA);
    }

    public Part part(String partName) {
        return formData == null ? new Part() : new Part(formData.getFirst(partName), partName);
    }

    public List<Part> parts(String partName) {
        List<Part> parts = new ArrayList<>();
        if (formData == null) {
            return parts;
        }
        Deque<FormData.FormValue> formValues = formData.get(partName);
        formValues.forEach(p -> parts.add(new Part(p, partName)));
        return parts;
    }

    public List<Part> parts() {
        List<Part> parts = new ArrayList<>();
        if (formData == null) {
            return parts;
        }
        Iterator<String> iterator = formData.iterator();
        while (iterator.hasNext()) {
            String next = iterator.next();
            Deque<FormData.FormValue> formValues = formData.get(next);
            for (FormData.FormValue value : formValues) {
                parts.add(new Part(value, next));
            }
        }
        return parts;
    }

    public List<String> partNames() {
        List<String> names = new ArrayList<>();
        if (formData == null) {
            return names;
        }
        formData.iterator().forEachRemaining(names::add);
        return names;
    }

    @Override
    public Iterator<Part> iterator() {
        return parts().iterator();
    }
}
