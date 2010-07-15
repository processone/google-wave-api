// Copyright 2008 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.util;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

/**
 * Utilities related to StringMap, StringSet, and CollectionFactory.
 *
 * @author ohler@google.com (Christian Ohler)
 */
public class CollectionUtils {

  private CollectionUtils() {
  }

  public static final DataDomain<ReadableStringSet, StringSet> STRING_SET_DOMAIN =
      new DataDomain<ReadableStringSet, StringSet>() {
    public void compose(StringSet target, ReadableStringSet changes, ReadableStringSet base) {
      target.clear();
      target.addAll(base);
      target.addAll(changes);
    }

    public StringSet empty() {
      return createStringSet();
    }

    public ReadableStringSet readOnlyView(StringSet modifiable) {
      return modifiable;
    }
  };

  public static final DataDomain<ReadableStringMap<Object>, StringMap<Object>> STRING_MAP_DOMAIN =
      new DataDomain<ReadableStringMap<Object>, StringMap<Object>>() {
    public void compose(StringMap<Object> target, ReadableStringMap<Object>  changes,
        ReadableStringMap<Object> base) {
      target.clear();
      target.putAll(base);
      target.putAll(changes);
    }

    public StringMap<Object> empty() {
      return createStringMap();
    }

    public ReadableStringMap<Object> readOnlyView(StringMap<Object> modifiable) {
      return modifiable;
    }
  };

  @SuppressWarnings("unchecked")
  public static <T> DataDomain<StringMap<T>, StringMap<T>> stringMapDomain() {
    return (DataDomain) STRING_MAP_DOMAIN;
  }

  @SuppressWarnings("unchecked")
  public static <T> DataDomain<Set<T>, Set<T>> hashSetDomain() {
    return (DataDomain) HASH_SET_DOMAIN;
  }

  public static final DataDomain<Set<Object>, Set<Object>> HASH_SET_DOMAIN =
      new DataDomain<Set<Object>, Set<Object>>() {
    public void compose(Set<Object> target, Set<Object> changes, Set<Object> base) {
      target.clear();
      target.addAll(changes);
      target.addAll(base);
    }

    public Set<Object> empty() {
      return new HashSet<Object>();
    }

    public Set<Object> readOnlyView(Set<Object> modifiable) {
      return Collections.unmodifiableSet(modifiable);
    }
  };

  /**
   * An adapter that turns a java.util.Map<String, V> into a StringMap<V>.
   *
   * @author ohler@google.com (Christian Ohler)
   *
   * @param <V> type of values in the map
   */
  private static final class StringMapAdapter<V> implements StringMap<V> {
    private final Map<String, V> backend;

    private StringMapAdapter(Map<String, V> backend) {
      Preconditions.checkNotNull(backend, "Attempt to adapt a null map");
      this.backend = backend;
    }

    public void putAll(ReadableStringMap<V> pairsToAdd) {
      // TODO(ohler): check instanceof here and implement a fallback.
      backend.putAll(((StringMapAdapter<V>) pairsToAdd).backend);
    }

    public void putAll(Map<String, V> sourceMap) {
      Preconditions.checkArgument(!sourceMap.containsKey(null),
          "Source map must not contain a null key");
      backend.putAll(sourceMap);
    }

    public void clear() {
      backend.clear();
    }

    public void put(String key, V value) {
      Preconditions.checkNotNull(key, "StringMap cannot contain null keys");
      backend.put(key, value);
    }

    public void remove(String key) {
      Preconditions.checkNotNull(key, "StringMap cannot contain null keys");
      backend.remove(key);
    }

    public boolean containsKey(String key) {
      Preconditions.checkNotNull(key, "StringMap cannot contain null keys");
      return backend.containsKey(key);
    }

    public V getExisting(String key) {
      Preconditions.checkNotNull(key, "StringMap cannot contain null keys");
      if (!backend.containsKey(key)) {
        // Not using Preconditions.checkState to avoid unecessary string concatenation
        throw new IllegalStateException("getExisting: Key '" + key + "' is not in map");
      }
      return backend.get(key);
    }

    public V get(String key) {
      Preconditions.checkNotNull(key, "StringMap cannot contain null keys");
      return backend.get(key);
    }

    public V get(String key, V defaultValue) {
      Preconditions.checkNotNull(key, "StringMap cannot contain null keys");
      if (backend.containsKey(key)) {
        return backend.get(key);
      } else {
        return defaultValue;
      }
    }

    public boolean isEmpty() {
      return backend.isEmpty();
    }

    public void each(ProcV<? super V> callback) {
      for (Map.Entry<String, V> entry : backend.entrySet()) {
        callback.apply(entry.getKey(), entry.getValue());
      }
    }

    public void filter(EntryFilter<? super V> filter) {
      for (Iterator<Map.Entry<String, V>> iterator = backend.entrySet().iterator();
          iterator.hasNext();) {
        Map.Entry<String, V> entry = iterator.next();
        if (filter.apply(entry.getKey(), entry.getValue())) {
          // entry stays
        } else {
          iterator.remove();
        }
      }
    }

    public int countEntries() {
      return backend.size();
    }

    public String someKey() {
      return isEmpty() ? null : backend.keySet().iterator().next();
    }

    public ReadableStringSet keySet() {
      return new StringSetAdapter(backend.keySet());
    }

    public String toString() {
      return backend.toString();
    }

    // NOTE(patcoleman): equals() and hashCode() should not be implemented in this adaptor, as
    // they are unsupported in the javascript collections.
  }

  /**
   * An adapter that turns a java.util.Map<Double, V> into a NumberMap<V>.
   *
   * @param <V> type of values in the map
   */
  private static final class NumberMapAdapter<V> implements NumberMap<V> {
    private final Map<Double, V> backend;

    private NumberMapAdapter(Map<Double, V> backend) {
      Preconditions.checkNotNull(backend, "Attempt to adapt a null map");
      this.backend = backend;
    }

    public void putAll(ReadableNumberMap<V> pairsToAdd) {
      // TODO(ohler): check instanceof here and implement a fallback.
      backend.putAll(((NumberMapAdapter<V>) pairsToAdd).backend);
    }

    public void putAll(Map<Double, V> sourceMap) {
      backend.putAll(sourceMap);
    }

    public void clear() {
      backend.clear();
    }

    public void put(double key, V value) {
      backend.put(key, value);
    }

    public void remove(double key) {
      backend.remove(key);
    }

    public boolean containsKey(double key) {
      return backend.containsKey(key);
    }

    public V getExisting(double key) {
      assert backend.containsKey(key);
      return backend.get(key);
    }

    public V get(double key) {
      return backend.get(key);
    }

    public V get(double key, V defaultValue) {
      if (backend.containsKey(key)) {
        return backend.get(key);
      } else {
        return defaultValue;
      }
    }

    public boolean isEmpty() {
      return backend.isEmpty();
    }

    public void each(ProcV<V> callback) {
      for (Map.Entry<Double, V> entry : backend.entrySet()) {
        callback.apply(entry.getKey(), entry.getValue());
      }
    }

    public void filter(EntryFilter<V> filter) {
      for (Iterator<Map.Entry<Double, V>> iterator = backend.entrySet().iterator();
          iterator.hasNext();) {
        Map.Entry<Double, V> entry = iterator.next();
        if (filter.apply(entry.getKey(), entry.getValue())) {
          // entry stays
        } else {
          iterator.remove();
        }
      }
    }

    public int countEntries() {
      return backend.size();
    }

    @Override
    public String toString() {
      return backend.toString();
    }

    // NOTE(patcoleman): equals() and hashCode() should not be implemented in this adaptor, as
    // they are unsupported in the javascript collections.
  }


  /**
   * An adapter that turns a java.util.Map<Integer, V> into an IntMap<V>.
   *
   * @param <V> type of values in the map
   */
  private static final class IntMapAdapter<V> implements IntMap<V> {
    private final Map<Integer, V> backend;

    private IntMapAdapter(Map<Integer, V> backend) {
      Preconditions.checkNotNull(backend, "Attempt to adapt a null map");
      this.backend = backend;
    }

    public void putAll(ReadableIntMap<V> pairsToAdd) {
      // TODO(ohler): check instanceof here and implement a fallback.
      backend.putAll(((IntMapAdapter<V>) pairsToAdd).backend);
    }

    public void putAll(Map<Integer, V> sourceMap) {
      backend.putAll(sourceMap);
    }

    public void clear() {
      backend.clear();
    }

    public void put(int key, V value) {
      backend.put(key, value);
    }

    public void remove(int key) {
      backend.remove(key);
    }

    public boolean containsKey(int key) {
      return backend.containsKey(key);
    }

    public V getExisting(int key) {
      assert backend.containsKey(key);
      return backend.get(key);
    }

    public V get(int key) {
      return backend.get(key);
    }

    public V get(int key, V defaultValue) {
      if (backend.containsKey(key)) {
        return backend.get(key);
      } else {
        return defaultValue;
      }
    }

    public boolean isEmpty() {
      return backend.isEmpty();
    }

    public void each(ProcV<V> callback) {
      for (Map.Entry<Integer, V> entry : backend.entrySet()) {
        callback.apply(entry.getKey(), entry.getValue());
      }
    }

    public void filter(EntryFilter<V> filter) {
      for (Iterator<Map.Entry<Integer, V>> iterator = backend.entrySet().iterator();
          iterator.hasNext();) {
        Map.Entry<Integer, V> entry = iterator.next();
        if (filter.apply(entry.getKey(), entry.getValue())) {
          // entry stays
        } else {
          iterator.remove();
        }
      }
    }

    public int countEntries() {
      return backend.size();
    }

    @Override
    public String toString() {
      return backend.toString();
    }

    // NOTE(patcoleman): equals() and hashCode() should not be implemented in this adaptor, as
    // they are unsupported in the javascript collections.
  }

  /**
   * An adapter that turns a java.util.Set<String> into a StringSet.
   *
   * @author ohler@google.com (Christian Ohler)
   */
  private static class StringSetAdapter implements StringSet {
    private final Set<String> backend;

    private StringSetAdapter(Set<String> backend) {
      Preconditions.checkNotNull(backend, "Attempt to adapt a null set");
      this.backend = backend;
    }

    public void add(String s) {
      Preconditions.checkNotNull(s, "StringSet cannot contain null values");
      backend.add(s);
    }

    public void clear() {
      backend.clear();
    }

    public boolean contains(String s) {
      Preconditions.checkNotNull(s, "StringSet cannot contain null values");
      return backend.contains(s);
    }

    public void remove(String s) {
      Preconditions.checkNotNull(s, "StringSet cannot contain null values");
      backend.remove(s);
    }

    public boolean isEmpty() {
      return backend.isEmpty();
    }

    public void each(ReadableStringSet.Proc callback) {
      for (String s : backend) {
        callback.apply(s);
      }
    }

    public boolean isSubsetOf(Set<String> set) {
      return set.containsAll(backend);
    }

    public boolean isSubsetOf(final ReadableStringSet other) {
      for (String s : backend) {
        if (!other.contains(s)) {
          return false;
        }
      }
      return true;
    }

    public void addAll(ReadableStringSet set) {
      backend.addAll(((StringSetAdapter) set).backend);
    }

    public void removeAll(ReadableStringSet set) {
      backend.removeAll(((StringSetAdapter) set).backend);
    }

    public void filter(StringPredicate filter) {
      for (Iterator<String> iterator = backend.iterator(); iterator.hasNext();) {
        String x = iterator.next();
        if (filter.apply(x)) {
          // entry stays
        } else {
          iterator.remove();
        }
      }
    }

    public String someElement() {
      return isEmpty() ? null : backend.iterator().next();
    }

    public String toString() {
      return backend.toString();
    }

    public int countEntries() {
      return backend.size();
    }
  }

  /**
   * An adapter that wraps a {@link IdentityHashMap}, presenting it as an
   * {@link IdentitySet}.
   */
  private static class IdentitySetAdapter<T> implements IdentitySet<T> {
    private final Map<T, T> backend = new IdentityHashMap<T, T>();

    private IdentitySetAdapter() {
    }

    public void add(T x) {
      Preconditions.checkNotNull(x, "IdentitySet cannot contain null values");
      // Note: Boxed primitives, and String, are disallowed. There are special
      // purpose maps for those key types, and the equality semantics between
      // the boxed primitives of Javascript and Java are dubious at best.
      if (x instanceof String || x instanceof Integer || x instanceof Double || x instanceof Long
          || x instanceof Boolean) {
        throw new UnsupportedOperationException(
            "Should NOT use boxed primitives with IdentitySet");
      }

      backend.put(x, x);
    }

    public void clear() {
      backend.clear();
    }

    public boolean contains(T s) {
      Preconditions.checkNotNull(s, "IdentitySet cannot contain null values");
      return backend.containsKey(s);
    }

    public void remove(T s) {
      Preconditions.checkNotNull(s, "IdentitySet cannot contain null values");
      backend.remove(s);
    }

    public boolean isEmpty() {
      return backend.isEmpty();
    }

    public void each(Proc<? super T> procedure) {
      for (T s : backend.keySet()) {
        procedure.apply(s);
      }
    }

    public String toString() {
      return backend.toString();
    }

    public int countEntries() {
      return backend.size();
    }
  }

  private static class NumberPriorityQueueAdapter implements NumberPriorityQueue {
    private final Queue<Double> queue;

    private NumberPriorityQueueAdapter(Queue<Double> queue) {
      this.queue = queue;
    }

    public boolean offer(double e) {
      return queue.offer(e);
    }

    public double peek() {
      return queue.peek();
    }

    public double poll() {
      return queue.poll();
    }

    public int size() {
      return queue.size();
    }
  }

  /**
   * An adapter that wraps a java.util.IdentityHashMap<K, V> into an
   * IdentityMap<K, V>. Note that this is a simple map, so 'identity' is defined
   * by the hashCode/equals of K instances.
   *
   * @param <K> type of keys in the map.
   * @param <V> type of values in the map
   */
  private static class IdentityHashMapAdapter<K, V> implements IdentityMap<K, V> {
    private final Map<K, V> backend = new IdentityHashMap<K, V>();

    private IdentityHashMapAdapter() {
    }

    public V get(K key) {
      return backend.get(key);
    }

    public boolean has(K key) {
      return backend.containsKey(key);
    }

    public void put(K key, V value) {
      // Note: Boxed primitives, and String, are disallowed. See explanation in
      // IdentitySetAdapter.
      if (key instanceof String || key instanceof Integer || key instanceof Double
          || key instanceof Long || key instanceof Boolean) {
        throw new UnsupportedOperationException(
            "Should NOT use boxed primitives as key with identity map");
      }
      backend.put(key, value);
    }

    public void remove(K key) {
      removeAndReturn(key);
    }

    public V removeAndReturn(K key) {
      return backend.remove(key);
    }

    public void clear() {
      backend.clear();
    }

    public boolean isEmpty() {
      return backend.isEmpty();
    }

    public void each(ProcV<? super K, ? super V> proc) {
      for (Map.Entry<K, V> entry : backend.entrySet()) {
        proc.apply(entry.getKey(), entry.getValue());
      }
    }

    public <R> R reduce(R initial, Reduce<K, V, R> proc) {
      R reduction = initial;
      for (Map.Entry<K, V> entry : backend.entrySet()) {
        reduction = proc.apply(reduction, entry.getKey(), entry.getValue());
      }
      return reduction;
    }

    public String toString() {
      return backend.toString();
    }

    public int countEntries() {
      return backend.size();
    }

    // NOTE(patcoleman): equals() and hashCode() should not be implemented in this adaptor, as
    // they are unsupported in the javascript collections.
  }

  /**
   * An implementation of CollectionFactory based on java.util.HashSet and
   * java.util.HashMap.
   *
   * @author ohler@google.com (Christian Ohler)
   */
  private static class HashCollectionFactory implements CollectionFactory {
    public <V> StringMap<V> createStringMap() {
      return CollectionUtils.adaptStringMap(new HashMap<String, V>());
    }

    public <V> NumberMap<V> createNumberMap() {
      return CollectionUtils.adaptNumberMap(new HashMap<Double, V>());
    }

    public <V> IntMap<V> createIntMap() {
      return CollectionUtils.adaptIntMap(new HashMap<Integer, V>());
    }

    public StringSet createStringSet() {
      return CollectionUtils.adaptStringSet(new HashSet<String>());
    }

    public <T> IdentitySet<T> createIdentitySet() {
      return new IdentitySetAdapter<T>();
    }

    public <E> Queue<E> createQueue() {
      return new LinkedList<E>();
    }

    public NumberPriorityQueue createPriorityQueue() {
      return CollectionUtils.adaptNumberPriorityQueue(new PriorityQueue<Double>());
    }

    public <K, V> IdentityMap<K, V> createIdentityMap() {
      return new IdentityHashMapAdapter<K, V>();
    }
  }

  private static final HashCollectionFactory HASH_COLLECTION_FACTORY =
      new HashCollectionFactory();

  private static CollectionFactory defaultCollectionFactory = HASH_COLLECTION_FACTORY;

  /**
   * Implements a persistently empty string map that throws exceptions on
   * attempt to add keys.
   */
  private static final class EmptyStringMap<V> implements StringMap<V> {
    public void clear() {
      // Success as the map is already empty.
    }

    public void filter(StringMap.EntryFilter<? super V> filter) {
    }

    public void put(String key, V value) {
      throw new UnsupportedOperationException();
    }

    public void putAll(ReadableStringMap<V> pairsToAdd) {
      throw new UnsupportedOperationException();
    }

    public void putAll(Map<String, V> sourceMap) {
      throw new UnsupportedOperationException();
    }

    public void remove(String key) {
    }

    public boolean containsKey(String key) {
      return false;
    }

    public int countEntries() {
      return 0;
    }

    public void each(org.waveprotocol.wave.model.util.ReadableStringMap.ProcV<? super V> callback) {
    }

    public V get(String key, V defaultValue) {
      return null;
    }

    public V get(String key) {
      return null;
    }

    public V getExisting(String key) {
      throw new UnsupportedOperationException();
    }

    public boolean isEmpty() {
      return true;
    }

    public String someKey() {
      return null;
    }

    public ReadableStringSet keySet() {
      // TODO(danilatos/ohler): Implement an immutable EMPTY_SET
      return CollectionUtils.createStringSet();
    }
  }

  private static final EmptyStringMap<Object> EMPTY_MAP = new EmptyStringMap<Object>();

  //
  // Plain old collections.
  //

  /**
   * Creates an empty {@code HashSet}.
   */
  public static <E> HashSet<E> newHashSet() {
    return new HashSet<E>();
  }

  /**
   * Creates a {@code HashSet} instance containing the given elements.
   *
   * @param elements the elements that the set should contain
   * @return a newly created {@code HashSet} containing those elements.
   */
  public static <E> HashSet<E> newHashSet(E... elements) {
    int capacity = Math.max((int) (elements.length / .75f) + 1, 16);
    HashSet<E> set = new HashSet<E>(capacity);
    Collections.addAll(set, elements);
    return set;
  }

  /**
   * Creates a {@code HashSet} instance containing the given elements.
   *
   * @param elements the elements that the set should contain
   * @return a newly created {@code HashSet} containing those elements.
   */
  public static <E> HashSet<E> newHashSet(Collection<? extends E> elements) {
    return new HashSet<E>(elements);
  }

  /**
   * Creates an empty immutable set.
   *
   * @return a newly created set containing those elements.
   */
  public static <E> Set<E> immutableSet() {
    // TODO(anorth): optimise to a truly immutable set.
    return Collections.unmodifiableSet(CollectionUtils.<E>newHashSet());
  }
  /**
   * Creates an immutable set containing the given elements.
   *
   * @param elements the elements that the set should contain
   * @return a newly created set containing those elements.
   */
  public static <E> Set<E> immutableSet(Collection<? extends E> elements) {
    // TODO(anorth): optimise to a truly immutable set.
    return Collections.unmodifiableSet(newHashSet(elements));
  }

  /**
   * Creates an immutable set containing the given elements.
   *
   * @param elements the elements that the set should contain
   * @return a newly created set containing those elements.
   */
  public static <E> Set<E> immutableSet(E... elements) {
    // TODO(anorth): optimise to a truly immutable set.
    return Collections.unmodifiableSet(newHashSet(elements));
  }

  /** Creates an empty {@link HashMap}. */
  public static <K, V> HashMap<K, V> newHashMap() {
    return new HashMap<K, V>();
  }

  /**
   * Creates a {@link HashMap} containing the elements in the given map.
   */
  public static <K, V> HashMap<K, V> newHashMap(Map<? extends K, ? extends V> map) {
    return new HashMap<K, V>(map);
  }

  /** Creates a new immutable map with one entry. */
  public static <K, V> Map<K, V> immutableMap(K k1, V v1) {
    // TODO(anorth): optimise to a truly immutable map.
    return Collections.singletonMap(k1, v1);
  }

  /** Creates a new immutable map with the given entries. */
  public static <K, V> Map<K, V> immutableMap(K k1, V v1, K k2, V v2) {
    Map<K, V> map = newHashMap();
    map.put(k1, v1);
    map.put(k2, v2);
    return Collections.unmodifiableMap(map);
  }

  /** Creates a new, empty linked list. */
  public static <T> LinkedList<T> newLinkedList() {
    return new LinkedList<T>();
  }

  /** Creates a new linked list containing elements provided by an iterable. */
  public static <T> LinkedList<T> newLinkedList(Iterable<? extends T> elements) {
    LinkedList<T> list = newLinkedList();
    for (T e : elements) {
      list.add(e);
    }
    return list;
  }

  /** Creates a new linked list containing the provided elements. */
  public static <T> LinkedList<T> newLinkedList(T... elements) {
    return newLinkedList(Arrays.asList(elements));
  }

  /** Creates a new, empty array list. */
  public static <T> ArrayList<T> newArrayList() {
    return new ArrayList<T>();
  }

  /** Creates a new array list containing elements provided by an iterable. */
  public static <T> ArrayList<T> newArrayList(Iterable<? extends T> elements) {
    ArrayList<T> list = newArrayList();
    for (T e : elements) {
      list.add(e);
    }
    return list;
  }

  /** Creates a new array list containing the provided elements. */
  public static <T> ArrayList<T> newArrayList(T... elements) {
    return newArrayList(Arrays.asList(elements));
  }

  //
  // String-based collections.
  //

  /**
   * Sets the default collection factory.
   *
   * This is used in the GWT client initialization code to plug in the JSO-based
   * collection factory. There shouldn't be any need to call this from other
   * places.
   */
  public static void setDefaultCollectionFactory(CollectionFactory f) {
    defaultCollectionFactory = f;
  }

  /**
   * Returns a CollectionFactory based on HashSet and HashMap from java.util.
   *
   * Note: getCollectionFactory() is probably a better choice.
   */
  public static CollectionFactory getHashCollectionFactory() {
    return HASH_COLLECTION_FACTORY;
  }

  /**
   * Returns the default CollectionFactory.
   */
  public static CollectionFactory getCollectionFactory() {
    return defaultCollectionFactory;
  }

  /**
   * Creates a new StringMap using the default collection factory.
   */
  public static <V> StringMap<V> createStringMap() {
    return CollectionUtils.getCollectionFactory().createStringMap();
  }

  /**
   * @returns an immutable empty map object. Always reuses the same object, does
   *          not create new ones.
   */
  @SuppressWarnings("unchecked")
  public static <V> StringMap<V> emptyMap() {
    return (StringMap<V>) EMPTY_MAP;
  }

  /**
   * Creates a new NumberMap using the default collection factory.
   */
  public static <V> NumberMap<V> createNumberMap() {
    return CollectionUtils.getCollectionFactory().createNumberMap();
  }

  /**
   * Creates a new NumberMap using the default collection factory.
   */
  public static <V> IntMap<V> createIntMap() {
    return CollectionUtils.getCollectionFactory().createIntMap();
  }

  /**
   * Creates a new queue using the default collection factory.
   */
  public static <V> Queue<V> createQueue() {
    return CollectionUtils.getCollectionFactory().createQueue();
  }

  /**
   * Creates a new priority queue using the default collection factory.
   */
  public static NumberPriorityQueue createPriorityQueue() {
    return CollectionUtils.getCollectionFactory().createPriorityQueue();
  }

  /**
   * Creates a new IdentityMap using the default collection factory.
   */
  public static <K, V> IdentityMap<K, V> createIdentityMap() {
    return CollectionUtils.getCollectionFactory().createIdentityMap();
  }

  /**
   * Creates a new IdentitySet using the default collection factory.
   */
  public static <V> IdentitySet<V> createIdentitySet() {
    return CollectionUtils.getCollectionFactory().createIdentitySet();
  }

  /**
   * Creates a new StringSet using the default collection factory.
   */
  public static StringSet createStringSet() {
    return getCollectionFactory().createStringSet();
  }

  public static <V> StringMap<V> copyStringMap(ReadableStringMap<V> m) {
    StringMap<V> copy = createStringMap();
    copy.putAll(m);
    return copy;
  }

  public static StringSet copyStringSet(StringSet s) {
    StringSet copy = createStringSet();
    copy.addAll(s);
    return copy;
  }

  /**
   * Adds all entries from the source map to the target map.
   *
   * @return the target map, for convenience
   */
  public static <V, M extends Map<String, V>> M copyToJavaMap(ReadableStringMap<V> source,
      final M target) {
    source.each(new StringMap.ProcV<V>() {
      public void apply(String key, V value) {
        target.put(key, value);
      }
    });
    return target;
  }

  /**
   * Adds all entries from the source map to the target map. NOTE(patcoleman):
   * please only call from assertions/testing code. Ideally everything should be
   * ignorant of the java.util.Map implementations as the collection API here
   * becomes more useful.
   *
   * @return java.util.Map version of our IdentityMap
   */
  public static <K, V> Map<K, V> copyToJavaIdentityMapForTesting(IdentityMap<K, V> source) {
    final Map<K, V> result = new IdentityHashMap<K, V>();
    source.each(new IdentityMap.ProcV<K, V>() {
      public void apply(K key, V value) {
        result.put(key, value);
      }
    });
    return result;
  }

  /**
   * Creates a new java set with the same contents as the source StringSet.
   */
  public static <V> Map<String, V> newJavaMap(ReadableStringMap<V> source) {
    return copyToJavaMap(source, new HashMap<String, V>());
  }

  /**
   * Adds all elements from the source set to the target collection.
   *
   * @return the target collection, for convenience
   */
  public static <C extends Collection<String>> C copyToJavaCollection(
      ReadableStringSet source, final C target) {
    source.each(new StringSet.Proc() {
      public void apply(String element) {
        target.add(element);
      }
    });
    return target;
  }

  /**
   * Adds all values from the source map to the target collection.
   *
   * @return the target collection, for convenience
   */
  public static <T, C extends Collection<T>> C copyValuesToJavaCollection(
      ReadableStringMap<T> source, final C target) {
    source.each(new StringMap.ProcV<T>() {
      public void apply(String key, T value) {
        target.add(value);
      }
    });
    return target;
  }

  /**
   * Creates a new java set with the same contents as the source StringSet.
   */
  public static Set<String> newJavaSet(ReadableStringSet source) {
    return copyToJavaCollection(source, new HashSet<String>());
  }

  /**
   * Creates a new java list with the same contents as the source StringSet.
   */
  public static List<String> newJavaList(ReadableStringSet source) {
    return copyToJavaCollection(source, new ArrayList<String>());
  }

  /**
   * Creates a new java list with the same contents as the values of the source
   * StringMap.
   */
  public static <T> List<T> newJavaList(ReadableStringMap<T> source) {
    return copyValuesToJavaCollection(source, new ArrayList<T>());
  }

  /**
   * Returns a StringMap view of the specified map.
   */
  public static <V> StringMap<V> adaptStringMap(Map<String, V> a) {
    return new StringMapAdapter<V>(a);
  }

  /**
   * Returns a StringMap view of the specified map.
   */
  public static <V> NumberMap<V> adaptNumberMap(Map<Double, V> a) {
    return new NumberMapAdapter<V>(a);
  }

  /**
   * Returns a StringMap view of the specified map.
   */
  public static <V> IntMap<V> adaptIntMap(Map<Integer, V> a) {
    return new IntMapAdapter<V>(a);
  }

  /**
   * Returns a StringSet view of the specified set.
   */
  public static StringSet adaptStringSet(Set<String> a) {
    return new StringSetAdapter(a);
  }

  /**
   * Returns a NumberPriorityQueue adaptor of a regular java.util.PriorityQueue
   */
  public static NumberPriorityQueue adaptNumberPriorityQueue(PriorityQueue<Double> priorityQueue) {
    return new NumberPriorityQueueAdapter(priorityQueue);
  }

  /**
   * Returns a StringSet copy of the specified set.
   */
  public static StringSet newStringSet(Set<String> a) {
    StringSet s = createStringSet();
    for (String value : a) {
      s.add(value);
    }
    return s;
  }

  /**
   * Returns a StringSet consisting of the specified values, removing duplicates
   */
  public static StringSet newStringSet(String... values) {
    StringSet s = createStringSet();
    for (String value : values) {
      s.add(value);
    }
    return s;
  }

  /**
   * Joins an array of strings with the given separator
   */
  public static String join(char separator, String first, String... rest) {
    StringBuilder ret = new StringBuilder(first);
    for (int i = 0; i < rest.length; i++) {
      ret.append(separator);
      ret.append(rest[i]);
    }
    return ret.toString();
  }


  /**
   * Joins an array of strings with the given separator
   */
  public static String join(char separator, String... parts) {
    StringBuilder ret = new StringBuilder();
    if (parts.length > 0) {
      ret.append(parts[0]);
    }
    for (int i = 1; i < parts.length; i++) {
      ret.append(separator);
      ret.append(parts[i]);
    }
    return ret.toString();
  }

  /**
   * Joins an array of strings.
   */
  public static String join(String... parts) {
    StringBuilder ret = new StringBuilder();
    if (parts.length > 0) {
      ret.append(parts[0]);
    }
    for (int i = 1; i < parts.length; i++) {
      ret.append(parts[i]);
    }
    return ret.toString();
  }

  public static String repeat(char component, int repeat) {
    Preconditions.checkArgument(repeat >= 0, "Cannot have negative repeat");
    char[] chars = new char[repeat];
    Arrays.fill(chars, component);
    return String.valueOf(chars);
  }
}
