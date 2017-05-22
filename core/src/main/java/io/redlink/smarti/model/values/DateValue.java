/*
 * Copyright (c) 2016 - 2017 Redlink GmbH
 */

package io.redlink.smarti.model.values;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.redlink.smarti.model.Token;

import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;

/**
 * {@link Token#getValue()} used to represent a {@link Token.Type#Date} .
 * Date values provide the {@link Date} as well as the precision ({@link Grain}).
 * 
 * @author Rupert Westenthaler
 *
 */
public class DateValue {
    
    /**
     * Time grains. Higher ordinal means longer duration
     * @author Rupert Westenthaler
     *
     */
    public static enum Grain {
        millisecond(ChronoUnit.MILLIS, Calendar.MILLISECOND),
        second(ChronoUnit.SECONDS, Calendar.SECOND),
        minute(ChronoUnit.MINUTES, Calendar.MINUTE),
        hour(ChronoUnit.HOURS, Calendar.HOUR_OF_DAY),
        day(ChronoUnit.DAYS, Calendar.DAY_OF_MONTH),
        week(ChronoUnit.WEEKS, Calendar.WEEK_OF_YEAR),
        month(ChronoUnit.MONTHS, Calendar.MONTH),
        //quarter(null),
        year(ChronoUnit.YEARS, Calendar.YEAR);
        
        private final ChronoUnit chronoUnit;
        private final int dateField;

        Grain(ChronoUnit chronoUnit, int dateField){
            this.chronoUnit = chronoUnit;
            this.dateField = dateField;
        }
        
        public ChronoUnit getChronoUnit() {
            return chronoUnit;
        }

        public int getDateField() {
            return dateField;
        }
    }
    
    @JsonFormat(shape= JsonFormat.Shape.STRING, timezone="UTC")
    private Date date;
    
    private Grain grain = Grain.second;

    public DateValue() {
    }

    public DateValue(Date date) {
        this(date, Grain.second);
    }

    public DateValue(Date date, Grain grain) {
        this.date = date;
        this.grain = grain;
    }

    public final Date getDate() {
        return date;
    }
    public final void setDate(Date date) {
        this.date = date;
    }
    public final Grain getGrain() {
        return grain;
    }
    public final void setGrain(Grain grain) {
        this.grain = grain;
    }
    
    @Override
    public String toString() {
        return "date [date=" + date + ", grain=" + grain + "]";
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((date == null) ? 0 : date.hashCode());
        result = prime * result + ((grain == null) ? 0 : grain.hashCode());
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
        DateValue other = (DateValue) obj;
        if (date == null) {
            if (other.date != null)
                return false;
        } else if (!date.equals(other.date))
            return false;
        if (grain != other.grain)
            return false;
        return true;
    }
    
    
    
}
