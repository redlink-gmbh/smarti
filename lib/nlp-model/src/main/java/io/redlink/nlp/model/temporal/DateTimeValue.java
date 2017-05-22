package io.redlink.nlp.model.temporal;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class DateTimeValue {

    
    private Temporal start, end;

    private boolean instant;

    public DateTimeValue(){}
    
    /**
     * Creates an instant with the parsed temporal value
     * @param instant
     */
    public DateTimeValue(Temporal instant) {
        this.start = instant;
        this.instant = true;
    }
    /**
     * Creates an interval for the parsed temporal values
     * @param start the start of the interval or <code>null</code> if open
     * @param end the end of the interval or <code>null</code> if open
     */
    public DateTimeValue(Temporal start, Temporal end){
        this.start = start;
        this.end = end;
        this.instant = false;
    }
    
    /**
     * The start of the date-range. If instants, this is the same as {@link #getEnd()}
     * @return the start of the date-range
     */
    public Temporal getStart() {
        return start;
    }

    /**
     * Setter for the value of the token.
     */
    public void setStart(Temporal start) {
        this.start = start;
    }

    /**
     * The end of the date-range. If instants, this is the same as {@link #getStart()}
     * @return the end of the date-range
     */
    public Temporal getEnd() {
        return end;
    }

    public void setEnd(Temporal end) {
        this.end = end;
    }

    @JsonIgnore
    public boolean isOpenInterval() {
        return isInterval() && (start == null || end == null);
    }

    @JsonIgnore
    public boolean isInterval() {
        return !isInstant();
    }

    public boolean isInstant() {
        return instant;
    }

    public void setInstant(boolean instant) {
        this.instant = instant;
    }
    
    @Override
    public String toString() {
        if (isInstant()) {
            return "DateTimeVale [" +
                    "instant=" + getStart() + "]";
        } else {
            return "DateToken [" +
                    "interval" + (isOpenInterval()?" (open)":"") +
                    ", start=" + getStart() +
                    ", end=" + getEnd() + "]";
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (instant ? 1231 : 1237);
        result = prime * result + ((start == null) ? 0 : start.hashCode());
        result = prime * result + ((end == null) ? 0 : end.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DateTimeValue other = (DateTimeValue) obj;
        if (instant != other.instant)
            return false;
        if (end == null) {
            if (other.end != null)
                return false;
        } else if (!end.equals(other.end))
            return false;
        if (start == null) {
            if (other.start != null)
                return false;
        } else if (!start.equals(other.start))
            return false;
        return true;
    }

    

}
