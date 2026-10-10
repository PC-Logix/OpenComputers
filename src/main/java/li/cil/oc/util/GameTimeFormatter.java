package li.cil.oc.util;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public final class GameTimeFormatter {
    // Game dates are always formatted in English, regardless of server locale.
    private static final String[] WEEK_DAYS = {
        "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    };
    private static final String[] SHORT_WEEK_DAYS = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
    private static final String[] MONTHS = {
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    };
    private static final String[] SHORT_MONTHS = {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    private GameTimeFormatter() {
    }

    public record DateTime(int year, int month, int day, int weekDay, int yearDay,
                           int hour, int minute, int second) {
    }

    public static DateTime parse(double time) {
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis((long) (time * 1000));
        return new DateTime(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.DAY_OF_WEEK),
            calendar.get(Calendar.DAY_OF_YEAR),
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            calendar.get(Calendar.SECOND));
    }

    // See http://www.cplusplus.com/reference/ctime/strftime/
    public static String format(String pattern, DateTime time) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < pattern.length(); i++) {
            char character = pattern.charAt(i);
            if (character == '%' && i + 1 < pattern.length()) {
                char specifier = pattern.charAt(++i);
                switch (specifier) {
                    case 'a' -> result.append(SHORT_WEEK_DAYS[time.weekDay - 1]);
                    case 'A' -> result.append(WEEK_DAYS[time.weekDay - 1]);
                    case 'b', 'h' -> result.append(SHORT_MONTHS[time.month - 1]);
                    case 'B' -> result.append(MONTHS[time.month - 1]);
                    case 'c' -> result.append(format("%a %b %e %H:%M:%S %Y", time));
                    case 'C' -> result.append(number(time.year / 100, 2, '0'));
                    case 'd' -> result.append(number(time.day, 2, '0'));
                    case 'D', 'x' -> result.append(format("%m/%d/%y", time));
                    case 'e' -> result.append(number(time.day, 2, ' '));
                    case 'F' -> result.append(format("%Y-%m-%d", time));
                    case 'H' -> result.append(number(time.hour, 2, '0'));
                    case 'I' -> result.append(number((time.hour + 11) % 12 + 1, 2, '0'));
                    case 'j' -> result.append(number(time.yearDay, 3, '0'));
                    case 'm' -> result.append(number(time.month, 2, '0'));
                    case 'M' -> result.append(number(time.minute, 2, '0'));
                    case 'n' -> result.append('\n');
                    case 'p' -> result.append(time.hour < 12 ? "AM" : "PM");
                    case 'r' -> result.append(format("%I:%M:%S %p", time));
                    case 'R' -> result.append(format("%H:%M", time));
                    case 'S' -> result.append(number(time.second, 2, '0'));
                    case 't' -> result.append('\t');
                    case 'T', 'X' -> result.append(format("%H:%M:%S", time));
                    case 'w' -> result.append(time.weekDay - 1);
                    case 'y' -> result.append(number(time.year % 100, 2, '0'));
                    case 'Y' -> result.append(number(time.year, 4, '0'));
                    case '%' -> result.append('%');
                    default -> {
                    }
                }
            } else {
                result.append(character);
            }
        }
        return result.toString();
    }

    public static int mktime(int year, int month, int day, int hour, int minute, int second) {
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        return (int) (calendar.getTimeInMillis() / 1000);
    }

    private static String number(int value, int width, char pad) {
        return String.format(Locale.ROOT, "%" + (pad == '0' ? "0" : "") + width + "d", value);
    }
}
