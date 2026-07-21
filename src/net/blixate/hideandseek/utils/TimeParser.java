package net.blixate.hideandseek.utils;

import java.math.BigInteger;
import java.util.Locale;
/**
 * Parses timespan such as the String {@code "3d45m20s"} into a {@code long}, and can do so vice-versa:
 * convert a {@code long} into a parsable timespan.
 * <p><b>Written for Deadlight</b></p>
 * @author MisterBasic
 */
public class TimeParser {
	/** This enum is for internal use only. */
	public enum Timespan {
		YEAR('y', 1000L*60*60*24*365),
		DAY('d', 1000L*60*60*24),
		HOUR('h', 1000L*60*60),
		MINUTE('m', 1000L*60),
		SECOND('s', 1000L);
		
		char symbol;
		long mul;
		
		Timespan(char s, long m) {
			this.symbol = s;
			this.mul = m;
		}
		
		public long calculate(long input) {
			return input * mul;
		}
	}
	
	/** Format this {@code long} to a nice format, such as:
	 * {@code 29 days 15 minutes 1 second} */
	public static String toLongFormTime(long time) {
		String string = "";
		long amount = 0;
		long timeLeft = time;
		for(Timespan span : Timespan.values()) {
			amount = timeLeft / span.mul;
			if(amount > 0) {
				timeLeft -= amount * span.mul;
				string += amount + " " + span.name().toLowerCase(Locale.ENGLISH) + (amount > 1 ? "s " : " ");
			}
		}
		if(string.endsWith(" ")) {
			return string.substring(0, string.length()-1);
		}
		return string;
	}
	
	public static long parseTime(String time) {
		long result = 0;
		char c = '\0';
		String num = "";
		for(int idx = 0; idx < time.length(); idx++) {
			c = time.charAt(idx);
			// Check if this character is a number
			if(c >= 48 && c <= 57) {
				num += c;
			}else {
				BigInteger bigInt = new BigInteger(num);
				Timespan sym = null;
				for(Timespan span : Timespan.values()) {
					if(span.symbol == c) sym = span;
				}
				// Invalid symbol, probably throw an error or just continue
				if(sym == null) continue;
				result += bigInt.longValue() * sym.mul;
				num = "";
			}
		}
		/* If the number was too big, or something went wrong, convert it to the max value ALWAYS */
		if(result > Long.MAX_VALUE || result < 0) {
			return Long.MAX_VALUE;
		}
		return result;
	}
	
	/** Format this {@code long} to a nice format, such as:
	 * {@code 29d15m20s} */
	public static String toFancyTime(long time) {
		String s = "";
		long amount = 0;
		long i = time;
		for(Timespan span : Timespan.values()) {
			amount = i / span.mul;
			if(amount > 0) {
				i -= amount * span.mul;
				s += amount + "" + span.symbol;
			}
		}
		return s;
	}
	
	/** Turn this into a clock timer */
	public static String toClockTime(long time) {
		return toClockTime(time, Timespan.HOUR, Timespan.MINUTE, Timespan.SECOND);
	}
	
	/** Turn this into a clock timer */
	public static String toClockTime(long time, Timespan...timespans) {
		if(time < 0) {
			return "00:00";
		}
		String s = "";
		long amount = 0;
		long i = time;
		for(Timespan span : timespans) {
			amount = i / span.mul;
			if(amount > 0) {
				i -= amount * span.mul;
				s += (("" + amount).length() >= 2 ? amount : "0" + amount) + ":";
			}else {
				s += "00:";
			}
		}
		return s.substring(0, s.length()-1);
	}
	
	// a few utility methods
	/** Returns the MS representation of an amount of seconds */
	public static long seconds(long time) {
		return time * Timespan.SECOND.mul;
	}
	
	public static long minutes(long time) {
		return time * Timespan.MINUTE.mul;
	}
	
	public static long hours(long time) {
		return time * Timespan.HOUR.mul;
	}
	
	public static long days(long time) {
		return time * Timespan.DAY.mul;
	}
}
