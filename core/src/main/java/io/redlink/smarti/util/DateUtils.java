/*
 * Copyright 2017 Redlink GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.redlink.smarti.util;

import io.redlink.smarti.model.values.DateValue;
import io.redlink.smarti.model.values.DateValue.Grain;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public class DateUtils {
    
    private static Logger log = LoggerFactory.getLogger(DateUtils.class);

    private static ThreadLocal<DateFormat> dateFormat = new ThreadLocal<DateFormat>(){
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXX", Locale.GERMAN);

        }
    };
    
    /**
     * Returns the earliest/latest {@link Date} based on the {@link DateValue#getDate()}
     * and {@link DateValue#getGrain()}.
     * @param dateValue the date value
     * @return the earliest possible start in {@link Pair#getLeft()} and the latest
     * possible end in {@link Pair#getRight()}. If the parsed {@link DateValue} does
     * not have a {@link DateValue#getGrain()} the left and right date will be the
     * value of {@link DateValue#getDate()}
     * @throws NullPointerException if the parsed {@link DateValue} or
     * {@link DateValue#getDate()} are <code>null</code>
     */
    public static Pair<Date, Date> getDateRange(DateValue dateValue){
        if(dateValue.getGrain() == null){
            return new ImmutablePair<>(dateValue.getDate(), dateValue.getDate());
        } else {
            Instant inst = dateValue.getDate().toInstant();
            Instant startInst = inst.truncatedTo(dateValue.getGrain().getChronoUnit());
            Instant endInst = startInst.plus(1,dateValue.getGrain().getChronoUnit());
            return new ImmutablePair<>(Date.from(startInst), Date.from(endInst));
        }
    }
    
    public static Date getDate(DateValue dateValue, LocalTime defaultTime){
        Grain grain = dateValue.getGrain();
        if(grain != null && grain.ordinal() >= Grain.day.ordinal()){
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(dateValue.getDate());
            cal.set(Calendar.HOUR_OF_DAY, defaultTime.getHour());
            cal.set(Calendar.MINUTE, defaultTime.getMinute());
            cal.set(Calendar.SECOND, defaultTime.getSecond());
            cal.set(Calendar.MILLISECOND, defaultTime.getSecond());
            return cal.getTime();
        } else {
            return dateValue.getDate();
        }
    }
    
    /**
     * Tries to convert the parsed value to a Date. Directly supports
     * {@link DateValue}, {@link Date}, {@link Instant} and {@link Calendar}.
     * For other types it tries to parse the Date from the string value of
     * the parsed object.
     * @param value the value
     * @return the {@link Date} or <code>null</code> if <code>null</code> was
     * parsed or the parsed object could not be parsed.
     */
    public static Date toDate(Object value) {
        if(value == null){
            return null;
        } else if(value instanceof DateValue){
            return ((DateValue)value).getDate();
        } else if (value instanceof Date) {
            return (Date) value;
        } else if(value instanceof Calendar){
            return ((Calendar)value).getTime();
        } else if(value instanceof Instant){
            return Date.from((Instant)value);
        } else {
            try {
                return dateFormat.get().parse(String.valueOf(value));
            } catch (ParseException e) {
                log.warn("Could not parse date '{}' from token: {}", value, e.getMessage());
                return null;
            }
        }
    }

    
    public static void main(String[] args) {
        DateValue dv = new DateValue();
        dv.setDate(new Date());
        dv.setGrain(Grain.minute);
        System.out.println(new SimpleDateFormat().format(getDate(dv, LocalTime.of(8, 0))));
    }
    
}
