// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document;

import org.waveprotocol.wave.model.util.ReadableStringMap;

/**
 * An object that represents an interval of an AnnotationSet.
 *
 * An interval is a maximal range of an AnnotationSet in which all items have
 * the same annotations.  Intervals are always nonempty.
 *
 * @author ohler@google.com (Christian Ohler)
 *
 * @param <V> the type of values in the AnnotationSet
 */
public interface AnnotationInterval<V> {
  /** The index of the first item of this interval. */
  int start();

  /** The index beyond the last item of this interval. */
  int end();

  /** The length of this interval. */
  int length();

  /**
   * The annotations in this interval.
   */
  ReadableStringMap<V> annotations();

  /**
   * The annotations in this interval whose values differ from the previous
   * interval.
   *
   * If this interval is the first interval returned during an iteration,
   * the return value of this method is defined as follows: If the iteration
   * started at index 0, it will contain the same entries as the return value of
   * annotations().  If the iteration started at an index greater than 0, it
   * will contain the differences from the annotations at the index prior to
   * the start of the iteration.
   *
   * This may throw an UnsupportedOperationException if this AnnotationInterval
   * was not returned by a left-to-right iteration.
   */
  ReadableStringMap<V> diffFromLeft();
}
