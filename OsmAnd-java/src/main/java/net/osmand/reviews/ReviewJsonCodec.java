package net.osmand.reviews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Encodes/Decodes a collection of {@link Review}s as JSON.
 */
public final class ReviewJsonCodec {
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public String toJson(Iterable<Review> reviews) {
        JSONArray array = new JSONArray();
        for (Review review : reviews) {
            array.put(toJson(review));
        }
        return array.toString();
    }

    public Iterable<Review> fromJson(String json) {
        JSONArray array = new JSONArray(json);
        List<Review> reviews = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            reviews.add(fromJson(object));
        }
        return reviews;
    }

    private JSONObject toJson(Review review) {
        JSONObject json = new JSONObject();
        json.put("id", review.id());
        if (review.opinion() != null) json.put("opinion", review.opinion());
        json.put("rating", review.rating());
        json.put("author", review.author());
        json.put("date", review.date().format(formatter));
        if (review.link() != null) json.put("link", review.link().toString());
        return json;
    }

    private Review fromJson(JSONObject object) {
        checkFieldsPresent(object, "id", "rating", "author", "date");
        String id = object.getString("id");
        String opinion = object.has("opinion") ? object.getString("opinion") : null;
        int rating = object.getInt("rating");
        String author = object.getString("author");
        LocalDate date = LocalDate.parse(object.getString("date"), formatter);
        URI link = object.has("link") ? URI.create(object.getString("link")) : null;
        return new Review(id, opinion, rating, author, date, link);
    }

    private void checkFieldsPresent(JSONObject object, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (!object.has(fieldName)) {
                throw new IllegalArgumentException("missing required Review field: " + fieldName + "; object=" + object);
            }
        }
    }
}
