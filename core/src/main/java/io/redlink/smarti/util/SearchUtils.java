package io.redlink.smarti.util;

import static io.redlink.smarti.util.StringUtils.hasLetterOrDigit;
import static org.apache.commons.lang3.StringUtils.isAlphanumeric;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNumeric;

public final class SearchUtils {

    private SearchUtils() {
        throw new IllegalStateException("Do not try to create instances of this Util class by reflection :(");
    }

    /**
     * Creates a SolrQuery with boosts for the parsed search words <ul>
     * <li> <code>\\s+</code> is used as tokenizer
     * <li> all tokens are query escaped.
     * <li> for numeric tokens a term with <code>{token}^4</code> is added to the query
     * <li> for tokens with at lest a single letter or digit <ul>
     * <li> a term with <code>{token}^4</code> is added to the query
     * <li> if the tokens ends with Letter or Digit a term with <code>{token}*^2</code> is added to the query
     * <li> if the token is alphanumeric  a term with <code>*{token}*</code> (without boost) is added to the query
     * </ul>
     * <li> it the parsed text does not contain any letter or query it is query escaped and added as is to the query
     * </ul>
     * @param searchText the search text
     * @return the value of the solr query representing the parsed search text
     */
    public static String createSearchWordQuery(String searchText){
        if(isBlank(searchText)){
            return null;
        } else if(!hasLetterOrDigit(searchText)){
            return escapeQueryChars(searchText);
        } else {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for(String token : searchText.split("\\s+")){
                if(hasLetterOrDigit(token)){
                    if(first){
                        first = false;
                    } else {
                        sb.append(" ");
                    }
                    if(isNumeric(token)){
                        sb.append(token).append("^4");
                    } else {
                        String escapedToken = escapeQueryChars(token);
                        sb.append(escapedToken).append("^4");
                        if(Character.isLetterOrDigit(token.charAt(token.length()-1))){
                            sb.append(' ').append(escapedToken).append("*^2");
                        }
                        if(isAlphanumeric(token)){
                            sb.append(" *").append(escapedToken).append('*');
                        }
                    }
                } //else igonre
            }
            return sb.toString();
        }
    }
    
    /*
     * NOTE: Copied from Solr ClientUtils (to avoid dependency)
     */
    private static String escapeQueryChars(String s) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);
        // These characters are part of the query syntax and must be escaped
        if (c == '\\' || c == '+' || c == '-' || c == '!'  || c == '(' || c == ')' || c == ':'
          || c == '^' || c == '[' || c == ']' || c == '\"' || c == '{' || c == '}' || c == '~'
          || c == '*' || c == '?' || c == '|' || c == '&'  || c == ';' || c == '/'
          || Character.isWhitespace(c)) {
          sb.append('\\');
        }
        sb.append(c);
      }
      return sb.toString();
    }

}
