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
        String url2 = "Ein beispiel: https://www.example.com/foo/?bar=baz&inga=42&quux";
        String wrong_url1 = "khttp://foo.bar?q=Spaces should be encoded";
        String wrong_url2 = "Das ist keine URL khttp://foo.bar?q=Spaces should be encoded";
        Assert.assertTrue(p.matcher(url1).find());
        Assert.assertTrue(p.matcher(url2).find());
        Assert.assertFalse(p.matcher(wrong_url1).find());
        Assert.assertFalse(p.matcher(wrong_url2).find());
    }

}