package io.redlink.nlp.model.temporal;

import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;

import static org.apache.commons.lang3.time.DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT;

/**
 *
 * Date/Time values provide the {@link Date} as well as the precision ({@link Grain}).
 * 
 * @author Rupert Westenthaler
 *
 */
public class Temporal {
    
    /**
     * Time grains. Higher ordinal means longer duration
     * @author Rupert Westenthaler
     *
     */
    public enum Grain {
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
    
    private Date date;
    
    private Grain grain = Grain.second;

    public Temporal() {
    }

    public Temporal(Date date) {
        this(date, Grain.second);
    }

    public Temporal(Date date, Grain grain) {
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
        return ISO_DATETIME_TIME_ZONE_FORMAT.format(date) + "(grain=" + grain + ")";
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
        final Temporal other = (Temporal) obj;
        if (date == null) {
            if (other.date != null)
                return false;
        } else if (!date.equals(other.date))
            return false;
        return grain == other.grain;
    }
    
    
    
}
