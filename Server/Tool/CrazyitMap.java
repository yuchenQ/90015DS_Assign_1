package Server.Tool;

import java.util.*;

public class CrazyitMap<K, V> {
	/*---------------------------------------------------------------*/
	public Map<K, V> map;

	/*---------------------------------------------------------------*/
	public CrazyitMap() {
		// 创建一个线程安全的HashMap
		this.map = Collections.synchronizedMap(new HashMap<K, V>());
	}

	/*---------------------------------------------------------------*/
	// 根据Value来删除指定项
	public synchronized void removeByValue(Object value) {
		for (Object key : map.keySet()) {

			if (map.get(key) == value) {
				map.remove(key);
				break;
			}
		}
	}

	/*---------------------------------------------------------------*/
	// 获取所有Value组成的Set集合
	public synchronized Set<V> valueSet() {
		Set<V> results = new HashSet<V>();
		// 将map中所有的value添加到results集合中
		map.forEach((key, value) -> results.add(value));
		return results;
	}

	/*---------------------------------------------------------------*/
	// 根据value查找key
	public synchronized K getKeyByValue(V val) {
		// 遍历所有value组成的集合
		for (K key : map.keySet()) {
			// 如果指定的key对应的value与被搜索的value相同，则返回对应的key
			if (map.get(key) == val || map.get(key).equals(val)) {
				return key;
			}
		}
		return null;
	}

	/*---------------------------------------------------------------*/
	// 实现put方法，该方法不允许value重复
	public synchronized V put(K key, V value) {
		// 遍历所有key组成的集合
		for (V val : valueSet()) {
			// 如果某个value与试图放入集合的value相同
			if (val.equals(value) && val.hashCode() == value.hashCode()) {
				throw new RuntimeException("-- CrazyitMap 不允许有重复的value! ");
			}
		}
		return map.put(key, value);
	}
}
