package net.osmand.reviews;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class ReviewJsonCodecTest {
    private static Object[] noReviews() {
        return new Object[]{
                "noReviews", new ArrayList<Review>()
        };
    }

    private static Object[] multipleReviews() throws URISyntaxException {
        return new Object[]{
                "multipleReviews",
                Arrays.asList(
                        new Review(
                                "yhjkE6T9Xkq-ZXIGsBPJtexR9ISOSljxqXRcGcBBPw0bUmmTl-OFSDI1ZP4btkNk8knMWajZpAHwzdPQHFyScw",
                                "Vintage cocktails made with ingredients that are no longer available and have been recreated from scratch. Amazing barrel-aged cocktails.",
                                100,
                                "enigal",
                                LocalDate.of(2025, Month.JULY, 8),
                                new URI("https://mangrove.reviews/list?signature=yhjkE6T9Xkq-ZXIGsBPJtexR9ISOSljxqXRcGcBBPw0bUmmTl-OFSDI1ZP4btkNk8knMWajZpAHwzdPQHFyScw")),
                        new Review(
                                "test1",
                                "I could not find the place at all",
                                40,
                                "some rando",
                                LocalDate.of(2025, Month.JULY, 22),
                                new URI("https://blog.mmakowski.com"))
                )};

    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> testCases() throws URISyntaxException {
        return Arrays.asList(noReviews(), multipleReviews());
    }

    private final Iterable<Review> inputReviews;

    public ReviewJsonCodecTest(String ignoredName, Iterable<Review> reviews) {
        inputReviews = reviews;
    }

    @Test
    public void testEncodeDecodeRoundtrip() {
        ReviewJsonCodec codec = new ReviewJsonCodec();
        assertEquals(inputReviews, codec.fromJson(codec.toJson(inputReviews)));
    }
}
