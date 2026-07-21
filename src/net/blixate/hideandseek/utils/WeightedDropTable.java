package net.blixate.hideandseek.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

/**
 * <p>Drop Table with established <b>weights</b></p>
 * <p>When {@code pick()} is called, a random element is picked. Elements with higher
 * weights are picked more often, while elements with lower weights have a lower chance
 * of being picked. This is compared to the total weights of all elements, so you can make up how your weights work.
 * Elements with equal weights should have an equal chance of being picked.</p>
 */
public class WeightedDropTable<T> implements DropTable<T> {

	public static class WeightedElement<K> {
		private K element;
		private int weight;
		
		private WeightedElement(K element) {
			this.element = element;
		}
		
		private WeightedElement(K element, int weight) {
			this(element);
			this.weight = weight;
		}
		
		public int getWeight() {
			return weight;
		}
		
		public K getElement() {
			return element;
		}
	}
	
	private ArrayList<WeightedElement<T>> pool;
	
	private Random random;
	
	
	public WeightedDropTable() {
		random = new Random();
		pool = new ArrayList<>();
	}
	
	public WeightedDropTable(T[] elements) {
		this();
		for(int i = 0; i < elements.length; i++) {
			pool.add(new WeightedElement<T>(elements[i]));
		}
	}
	
	public WeightedDropTable(List<T> elements) {
		this();
		for(int i = 0; i < elements.size(); i++) {
			pool.add(new WeightedElement<T>(elements.get(i)));
		}
	}
	
	public void setWeight(int index, int weight) {
		if(index >= pool.size() && index < 0) {
			throw new ArrayIndexOutOfBoundsException("Element does not exist!");
		}
		pool.get(index).weight = weight;
	}
	
	public void setElement(int index, T element) {
		if(index >= pool.size() && index < 0) {
			throw new ArrayIndexOutOfBoundsException("Element does not exist!");
		}
		pool.get(index).element = element;
	}
	
	public void addElement(T element, int weight) {
		pool.add(new WeightedElement<T>(element, weight));
	}
	
	public int getTotalWeight() {
		// I couldn't think of a better way lol
		// Long-term performance may be affected, but for now it's fine.
		// If we wanted to upgrade performance without rewriting, just cache this result
		// until an element is added/removed
		int total = 0;
		for(int i = 0; i < pool.size(); i++) {
			total += pool.get(i).weight;
		}
		return total;
	}
	
	public T pick(Random random) {
		int totalWeight = getTotalWeight();
		int randint = random.nextInt(totalWeight);
		int weightSum = 0;
		for(int i = 0; i < pool.size(); i++) {
			WeightedElement<T> element = pool.get(i);
			weightSum += element.weight;
			if(randint < weightSum) {
				return element.element;
			}
		}
		return null;
	}
	
	@Override
	public T pick() {
		int totalWeight = getTotalWeight();
		int randint = random.nextInt(totalWeight);
		int weightSum = 0;
		for(int i = 0; i < pool.size(); i++) {
			WeightedElement<T> element = pool.get(i);
			weightSum += element.weight;
			if(randint < weightSum) {
				return element.element;
			}
		}
		return null;
	}
	
	public static <E> WeightedDropTable<E> fromHashMap(HashMap<E, Integer> hashmap) {
		WeightedDropTable<E> table = new WeightedDropTable<>();
		for(Entry<E, Integer> element : hashmap.entrySet()) {
			table.addElement(element.getKey(), element.getValue());
		}
		return table;
	}

	public int size() {
		return pool.size();
	}

	public T get(int i) {
		return pool.get(i).element;
	}
	
	public int getWeight(int i) {
		return pool.get(i).weight;
	}

}
