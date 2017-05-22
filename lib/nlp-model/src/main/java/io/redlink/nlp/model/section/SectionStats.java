package io.redlink.nlp.model.section;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.redlink.nlp.model.Section;

/**
 * Provides statistics for a {@link Section}
 * @author Rupert Westenthaler
 *
 */
public class SectionStats {
    
    private int numChars = -1;
    private int numAlpha = -1;
    private int numDigit = -1;
    private int numWhitespace = -1;

    public SectionStats() {
        super();
    }
    
    public SectionStats(int count, int alpha, int digit, int whitespace) {
        setNumChars(count);
        setNumAlpha(alpha);
        setNumDigit(digit);
        setNumWhitespace(whitespace);
    }
    
    /**
     * Getter for the number of characters contained in a section. This might
     * be different to the start/end of the section in case of surrogate pairs
     * are present in the section    
     * @return the number of chars or <code>-1</code> if not known
     */
    public int getNumChars() {
        return numChars;
    }
    
    public void setNumChars(int numChars) {
        this.numChars = numChars;
    }
    
    /**
     * Getter for the number of {@link Character#isAlphabetic(int) alphabetic}
     * characters contained in this section    
     * @return the number of alphabetic chars or <code>-1</code> if not known
     */
    public int getNumAlpha() {
        return numAlpha;
    }
    
    public void setNumAlpha(int numAlpha) {
        this.numAlpha = numAlpha < 0 ? -1 : numAlpha;
    }
    /**
     * Getter for the number of {@link Character#isDigit(int) digit}
     * characters contained in this section    
     * @return the number of digit chars or <code>-1</code> if not known
     */
    public int getNumDigit() {
        return numDigit;
    }
    public void setNumDigit(int numDigit) {
        this.numDigit = numDigit < 0 ? -1 : numDigit;
    }
    /**
     * Getter for the number of {@link Character#isWhitespace(int) whitespace}
     * characters contained in this section    
     * @return the number of whitespace chars or <code>-1</code> if not known
     */
    public int getNumWhitespace() {
        return numWhitespace;
    }
    
    public void setNumWhitespace(int numWhitespace) {
        this.numWhitespace = numWhitespace < 0 ? -1 : numWhitespace;
    }
    /**
     * The number of alphabetic and digit chars.
     * @return the number of alphabetic and digit chars or <code>-1</code> if not known
     */
    @JsonIgnore
    public int getNumAlphaAndDigit(){
        return numAlpha < 0 && numDigit < 0 ? -1 : 
            (numAlpha < 0 ? 0 : numAlpha) + (numDigit < 0 ? 0 : numDigit);
    }
    /**
     * The number of other chars in this section
     * @return the number of none alphabetic, digit nor whitespace chars in this section or
     * <code>-1</code> if not known
     */
    @JsonIgnore
    public int getNumOther(){
        if(numChars < 0){
            return -1;
        }
        int alphaAndDigit = getNumAlphaAndDigit();
        if(alphaAndDigit < 0){
            return -1;
        }
        return numChars - alphaAndDigit;
    }
    /**
     * The fraction of alphabetic chars within the section
     * @return the fraction or <code>-1</code> if not known. <code>0</code> if the
     * section is an empty string.
     */
    @JsonIgnore
    public float getFractionAlpha(){
        return numAlpha < 0 || numChars < 0 ? -1f : numChars == 0 ? 0 : numAlpha/(float)numChars;
    }
    /**
     * The fraction of digit chars within the section
     * @return the fraction or <code>-1</code> if not known. <code>0</code> if the
     * section is an empty string.
     */
    @JsonIgnore
    public float getFractionDigit(){
        return numDigit < 0 || numChars < 0 ? -1f : numChars == 0 ? 0 : numDigit/(float)numChars;
    }
    /**
     * The fraction of whitespace chars within the section
     * @return the fraction or <code>-1</code> if not known. <code>0</code> if the
     * section is an empty string.
     */
    @JsonIgnore
    public float getFractionWhitespace(){
        return numWhitespace < 0 || numChars < 0 ? -1f : numChars == 0 ? 0 : numWhitespace/(float)numChars;
    }

    @Override
    public String toString() {
        return "stats[chars=" + numChars + ", alpha=" + numAlpha + ", digit=" + numDigit
                + ", ws=" + numWhitespace + "]";
    }
    
    
    
}
