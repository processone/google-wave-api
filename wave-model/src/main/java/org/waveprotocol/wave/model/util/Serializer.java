// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.util;


/**
 * Encodes and decodes values to and from strings.
 *
*
 * @param <T> value type
 */
public interface Serializer<T> {
  /**
   * A serializer for string values.
   */
  public final static Serializer<String> STRING = new Serializer<String>() {
    public String fromString(String s) {
      return fromString(s, null);
    }

    public String fromString(String s, String defaultValue) {
      return s != null ? s : defaultValue;
    }

    public String toString(String x) {
      return x;
    }
  };

  /**
   * A serializer for long values.
   */
  public final static Serializer<Long> LONG = new Serializer<Long>() {
    public Long fromString(String s) {
      return fromString(s, null);
    }

    public Long fromString(String s, Long defaultValue) {
      return (s != null)
          ? Long.valueOf(Long.parseLong(s))
          : defaultValue;
    }

    public String toString(Long x) {
      return (x != null) ? x.toString() : null;
    }
  };

  /**
   * A serializer for integer values.
   */
  public final static Serializer<Integer> INTEGER = new Serializer<Integer>() {
    public Integer fromString(String s) {
      return fromString(s, null);
    }

    public Integer fromString(String s, Integer defaultValue) {
      return (s != null)
          ? Integer.valueOf(Integer.parseInt(s))
          : defaultValue;
    }

    public String toString(Integer x) {
      return (x != null) ? x.toString() : null;
    }
  };

  /**
   * A serializer for boolean values.
   */
  public final static Serializer<Boolean> BOOLEAN = new Serializer<Boolean>() {
    public Boolean fromString(String s) {
      return fromString(s, null);
    }

    public Boolean fromString(String s, Boolean defaultValue) {
      return (s != null)
          ? Boolean.valueOf(Boolean.parseBoolean(s))
          : defaultValue;
    }

    public String toString(Boolean x) {
      return (x != null) ? x.toString() : null;
    }
  };

  /**
   * A serializer for double values.
   */
  public final static Serializer<Double> DOUBLE = new Serializer<Double>() {
    public Double fromString(String s) {
      return fromString(s, null);
    }

    public Double fromString(String s, Double defaultValue) {
      return (s != null)
          ? Double.valueOf(Double.parseDouble(s))
          : defaultValue;
    }

    public String toString(Double x) {
      return (x != null) ? x.toString() : null;
    }
  };

  /**
   * Skeleton support for serlialization of enums. Concrete classes can be
   * created by constructing an instance and passing the class of the enum to
   * the constructor.
   *
   * @param <E> The actual type of the enum.
   */
  // TODO(user): This was made non-final so it could be overriden by the
  // AttachmentDocumentWrapper. Switch it back to final, and find a better
  // way to get the desired behaviour.
  public static class EnumSerializer<E extends Enum<E>> implements Serializer<E> {
    private final Class<E> enumClass;

    public EnumSerializer(Class<E> enumClass) {
      this.enumClass = enumClass;
    }

    public E fromString(String s) {
      return fromString(s, null);
    }

    public E fromString(String s, E defaultValue) {
      return (s != null) ? E.valueOf(enumClass, s) : defaultValue;
    }

    public String toString(E x) {
      return (x != null) ? x.name() : null;
    }
  }

  /**
   * Encodes a value as a string. If the value is null, the result will be null.
   *
   * @param x value to encode
   * @return string representation of {@code x}, or null.
   */
  String toString(T x);

  /**
   * Decodes a value from a string. If the string is null, the result will be
   * null.
   *
   * @param s string representation of a value
   * @return value represented by {@code s}, or null.
   */
  T fromString(String s);

  /**
   * Decodes a value from a string. If the string is null, the provided default
   * value will be returned.
   *
   * @param s string representation of a value
   * @param defaultValue value to return if s is null.
   * @return value represented by {@code s}, or defaultValue.
   */
  T fromString(String s, T defaultValue);
}
