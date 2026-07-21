package net.blixate.hideandseek.utils;

import java.util.Random;

/** Interface to create drop tables.
 * 
 * <p>If used on it's own, all items in this table have an equal chance of appearing.</p>
 */
public interface DropTable<T> {
	/** Use a pre-defined Random Number Generator to pick an element. */
	public T pick(Random random);
	/** Pick an element. */
	public T pick();
}