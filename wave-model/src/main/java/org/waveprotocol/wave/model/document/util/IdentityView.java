// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.Map;

/**
 * Convenience class, designed for subclassing to reduce boilerplate from
 * delegation.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 *
 * @param <N>
 * @param <E>
 * @param <T>
 */
public class IdentityView<N, E extends N, T extends N>
    implements ReadableDocumentView<N, E, T> {

  protected final ReadableDocument<N, E, T> inner;

  protected IdentityView(ReadableDocument<N, E, T> inner) {
    Preconditions.checkNotNull(inner, "IdentityView (or subclass): " +
        "Inner document may not be null!");
    this.inner = inner;
  }

  public E asElement(N node) {
    return inner.asElement(node);
  }

  public T asText(N node) {
    return inner.asText(node);
  }

  public String getAttribute(E element, String name) {
    return inner.getAttribute(element, name);
  }

  public Map<String, String> getAttributes(E element) {
    return inner.getAttributes(element);
  }

  public String getData(T textNode) {
    return inner.getData(textNode);
  }

  public E getDocumentElement() {
    return inner.getDocumentElement();
  }

  public int getLength(T textNode) {
    return inner.getLength(textNode);
  }

  public short getNodeType(N node) {
    return inner.getNodeType(node);
  }

  public String getTagName(E element) {
    return inner.getTagName(element);
  }

  public boolean isSameNode(N node, N other) {
    return inner.isSameNode(node, other);
  }

  public N getFirstChild(N node) {
    return inner.getFirstChild(node);
  }

  public N getLastChild(N node) {
    return inner.getLastChild(node);
  }

  public N getNextSibling(N node) {
    return inner.getNextSibling(node);
  }

  public E getParentElement(N node) {
    return inner.getParentElement(node);
  }

  public N getPreviousSibling(N node) {
    return inner.getPreviousSibling(node);
  }

  public N getVisibleNode(N node) {
    return node;
  }

  public N getVisibleNodeFirst(N node) {
    return node;
  }

  public N getVisibleNodeLast(N node) {
    return node;
  }

  public N getVisibleNodeNext(N node) {
    return node;
  }

  public N getVisibleNodePrevious(N node) {
    return node;
  }

  public void onBeforeFilter(Point<N> at) {
    // do nothing.
  }
}
