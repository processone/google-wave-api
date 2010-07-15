// Copyright 2008 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.AnnotationCursor;
import org.waveprotocol.wave.model.document.AnnotationInterval;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.RangedAnnotation;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;
import org.waveprotocol.wave.model.util.ReadableStringSet;

import java.util.List;
import java.util.Map;

/**
 * Handy delegating implementation.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class MutableDocumentProxy<N, E extends N, T extends N>
    implements MutableDocument<N, E, T> {

  public static class DocumentProxy extends MutableDocumentProxy<Doc.N, Doc.E, Doc.T>
      implements Document {

    public  DocumentProxy() {
      super();
    }

    public DocumentProxy(Document delegate, String noDelegateErrorMessage) {
      super(delegate, noDelegateErrorMessage);
    }

    @SuppressWarnings("unchecked") // Adapter
    public DocumentProxy(MutableDocument delegate, String noDelegateErrorMessage) {
      super(delegate, noDelegateErrorMessage);
      Preconditions.checkArgument(delegate.getDocumentElement() instanceof Doc.E,
          "Incompatibel delegate type - must be of the Doc.* variety of nodes");
    }
  }

  private final String noDelegateErrorMessage;

  private MutableDocument<N, E, T> delegate;

  /**
   * Constructor with a null delegate and default error message.
   */
  public MutableDocumentProxy() {
    this(null, "delegate document is not set");
  }

  /**
   * @param delegate initial delegate
   * @param noDelegateErrorMessage error message when the delegate is null
   */
  public MutableDocumentProxy(MutableDocument<N, E, T> delegate, String noDelegateErrorMessage) {
    this.delegate = delegate;
    this.noDelegateErrorMessage = noDelegateErrorMessage;
  }

  /**
   * Sets the delegate to a new object
   *
   * @param newDelegate
   */
  protected void setDelegate(MutableDocument<N, E, T> newDelegate) {
    this.delegate = newDelegate;
  }

  /**
   * @return true if this proxy currently has a delegate.
   */
  protected boolean hasDelegate() {
    return delegate != null;
  }

  /**
   * Retrieves the delegate for immediate use.
   * Fails if there is no delegate.
   *
   * @return the current delegate
   */
  protected MutableDocument<N, E, T> getDelegate() {
    if (!hasDelegate()) {
      throw new IllegalStateException("MutableDocumentProxy: " + noDelegateErrorMessage);
    }
    return delegate;
  }

  public void with(Action actionToRunWithDocument) {
    getDelegate().with(actionToRunWithDocument);
  }

  public <V> V with(Method<V> methodToRunWithDocument) {
    return getDelegate().with(methodToRunWithDocument);
  }

  // Mostly eclipse generated delegate methods below.

  public AnnotationCursor annotationCursor(int start, int end, ReadableStringSet keys) {
    return getDelegate().annotationCursor(start, end, keys);
  }

  public Iterable<AnnotationInterval<String>> annotationIntervals(int start, int end,
      ReadableStringSet keys) {
    return getDelegate().annotationIntervals(start, end, keys);
  }

  public E appendXml(XmlStringBuilder xml) {
    return getDelegate().appendXml(xml);
  }

  public E asElement(N node) {
    return getDelegate().asElement(node);
  }

  public T asText(N node) {
    return getDelegate().asText(node);
  }

  public E createChildElement(E parent, String tag, Map<String, String> attributes) {
    return getDelegate().createChildElement(parent, tag, attributes);
  }

  public E createElement(Point<N> point, String tag, Map<String, String> attributes) {
    return getDelegate().createElement(point, tag, attributes);
  }

  public void deleteNode(E element) {
    getDelegate().deleteNode(element);
  }

  public Range deleteRange(int start, int end) {
    return getDelegate().deleteRange(start, end);
  }

  public PointRange<N> deleteRange(Point<N> start, Point<N> end) {
    return getDelegate().deleteRange(start, end);
  }

  public void emptyElement(E element) {
    getDelegate().emptyElement(element);
  }

  public int firstAnnotationChange(int start, int end, String key, String fromValue) {
    return getDelegate().firstAnnotationChange(start, end, key, fromValue);
  }

  public void forEachAnnotationAt(int location, ProcV<String> callback) {
    getDelegate().forEachAnnotationAt(location, callback);
  }

  public String getAnnotation(int location, String key) {
    return getDelegate().getAnnotation(location, key);
  }

  public String getAttribute(E element, String name) {
    return getDelegate().getAttribute(element, name);
  }

  public Map<String, String> getAttributes(E element) {
    return getDelegate().getAttributes(element);
  }

  public String getData(T textNode) {
    return getDelegate().getData(textNode);
  }

  public E getDocumentElement() {
    return getDelegate().getDocumentElement();
  }

  public N getFirstChild(N node) {
    return getDelegate().getFirstChild(node);
  }

  public N getLastChild(N node) {
    return getDelegate().getLastChild(node);
  }

  public int getLength(T textNode) {
    return getDelegate().getLength(textNode);
  }

  public int getLocation(N node) {
    return getDelegate().getLocation(node);
  }

  public int getLocation(Point<N> point) {
    return getDelegate().getLocation(point);
  }

  public N getNextSibling(N node) {
    return getDelegate().getNextSibling(node);
  }

  public short getNodeType(N node) {
    return getDelegate().getNodeType(node);
  }

  public E getParentElement(N node) {
    return getDelegate().getParentElement(node);
  }

  public N getPreviousSibling(N node) {
    return getDelegate().getPreviousSibling(node);
  }

  public String getTagName(E element) {
    return getDelegate().getTagName(element);
  }

  public void insertText(int location, String text) {
    getDelegate().insertText(location, text);
  }

  public void insertText(Point<N> point, String text) {
    getDelegate().insertText(point, text);
  }

  public E insertXml(Point<N> point, XmlStringBuilder xml) {
    return getDelegate().insertXml(point, xml);
  }
  
  public boolean isSameNode(N node, N other) {
    return getDelegate().isSameNode(node, other);
  }

  public int lastAnnotationChange(int start, int end, String key, String fromValue) {
    return getDelegate().lastAnnotationChange(start, end, key, fromValue);
  }

  public Point<N> locate(int location) {
    return getDelegate().locate(location);
  }

  public Iterable<RangedAnnotation<String>> rangedAnnotations(int start, int end,
      ReadableStringSet keys) {
    return getDelegate().rangedAnnotations(start, end, keys);
  }

  public void resetAnnotation(int start, int end, String key, String value) {
    getDelegate().resetAnnotation(start, end, key, value);
  }

  @Deprecated
  public void resetAnnotationsInRange(int rangeStart, int rangeEnd, String key,
      List<org.waveprotocol.wave.model.document.MutableAnnotationSet.RangedValue<String>> values) {
    getDelegate().resetAnnotationsInRange(rangeStart, rangeEnd, key, values);
  }

  public void setAnnotation(int start, int end, String key, String value) {
    getDelegate().setAnnotation(start, end, key, value);
  }

  public void setElementAttribute(E element, String name, String value) {
    getDelegate().setElementAttribute(element, name, value);
  }

  public void setElementAttributes(E element, Attributes attrs) {
    getDelegate().setElementAttributes(element, attrs);
  }

  public int size() {
    return getDelegate().size();
  }

  public void hackConsume(Nindo op) {
    getDelegate().hackConsume(op);
  }

  public DocInitialization toInitialization() {
    return getDelegate().toInitialization();
  }

  public void updateElementAttributes(E element, Map<String, String> attrs) {
    getDelegate().updateElementAttributes(element, attrs);
  }

  public ReadableStringSet knownKeys() {
    return getDelegate().knownKeys();
  }

  public String toXmlString() {
    return getDelegate().toXmlString();
  }
}
