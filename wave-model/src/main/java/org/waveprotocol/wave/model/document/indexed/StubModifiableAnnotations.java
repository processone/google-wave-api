// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.indexed;

import org.waveprotocol.wave.model.document.AnnotationCursor;
import org.waveprotocol.wave.model.document.AnnotationInterval;
import org.waveprotocol.wave.model.document.RangedAnnotation;
import org.waveprotocol.wave.model.document.util.GenericAnnotationCursor;
import org.waveprotocol.wave.model.document.util.GenericAnnotationIntervalIterable;
import org.waveprotocol.wave.model.document.util.GenericRangedAnnotationIterable;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringMap;
import org.waveprotocol.wave.model.util.ReadableStringSet;

/**
 * Blank implementation for when we want to ignore annotations.
 *
 * The implementation does keep track of size, and enforces many constraints.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class StubModifiableAnnotations<V> implements RawAnnotationSet<V> {

  private int size = 0;

  private boolean modifying = false;

  public int size() {
    return size;
  }

  public void begin(boolean needReverseOp) {
    assert !modifying : "Can't make nested modification";
    modifying = true;
  }

  public void finish() {
    assert modifying : "Can't finish non existent modification";
    modifying = false;
  }

  public void delete(int deleteSize) {
    assert modifying : "Can't make change unless during modification";
    size -= deleteSize;
  }

  public void endAnnotation(String key) {
    assert modifying : "Can't make change unless during modification";
  }

  public void insert(int insertSize) {
    assert modifying : "Can't make change unless during modification";
    size += insertSize;
  }

  public void skip(int skipSize) {
    assert modifying : "Can't make change unless during modification";
  }

  public void startAnnotation(String key, V value) {
    assert modifying : "Can't make change unless during modification";
  }

  public V getAnnotation(int start, String key) {
    return null;
  }

  public int firstAnnotationChange(int start, int end, String key, V fromValue) {
    return -1;
  }

  public int lastAnnotationChange(int start, int end, String key, V fromValue) {
    return -1;
  }

  public AnnotationCursor annotationCursor(int start, int end, ReadableStringSet keys) {
    if (keys == null) {
      keys = CollectionUtils.createStringSet();
    }
    return new GenericAnnotationCursor<V>(this, start, end, keys);
  }

  public void forEachAnnotationAt(int location, ReadableStringMap.ProcV<V> callback) {
    // don't call callback, no annotations
  }

  public Iterable<AnnotationInterval<V>> annotationIntervals(int start, int end,
      ReadableStringSet keys) {
    if (keys == null) {
      keys = CollectionUtils.createStringSet();
    }
    return new GenericAnnotationIntervalIterable<V>(this, start, end, keys);
  }

  public Iterable<RangedAnnotation<V>> rangedAnnotations(int start, int end,
      ReadableStringSet keys) {
    if (keys == null) {
      keys = CollectionUtils.createStringSet();
    }
    return new GenericRangedAnnotationIterable<V>(this, start, end, keys);
  }

  public String getInherited(String key) {
    return null;
  }

  public ReadableStringSet knownKeys() {
    return CollectionUtils.createStringSet();
  }

  public ReadableStringSet knownKeysLive() {
    return CollectionUtils.createStringSet();
  }
}
