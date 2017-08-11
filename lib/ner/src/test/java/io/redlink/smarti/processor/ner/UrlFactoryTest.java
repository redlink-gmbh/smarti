package io.redlink.smarti.processor.ner;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.regex.Pattern;

/**
 * @author Thomas Kurz (thomas.kurz@redlink.co)
 * @since 26.07.17.
 */
public class UrlFactoryTest {

    @Test
    public void testRegexPattern() {
        Pattern p = new UrlFactory().getRegexes(null, null).get(0).getPattern();

        String url1 = "http://foo.com/blah_blah/";
        String url2 = "https://www.example.com/foo/?bar=baz&inga=42&quux";
        String wrong_url = "http://foo.bar?q=Spaces should be encoded";

        Assert.assertTrue(p.matcher(url1).matches());
        Assert.assertTrue(p.matcher(url2).matches());
        Assert.assertFalse(p.matcher(wrong_url).matches());
    }

}