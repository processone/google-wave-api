// Copyright 2008 Google Inc. All Rights Reserved

package org.waveprotocol.wave.model.document;

import org.waveprotocol.wave.model.document.indexed.Locator;
import org.waveprotocol.wave.model.document.indexed.SimpleXmlParser;
import org.waveprotocol.wave.model.document.indexed.SimpleXmlParser.ItemType;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.operation.Nindo.Builder;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.util.AnnotationBuilder;
import org.waveprotocol.wave.model.document.util.Annotations;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.PointRange;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.operation.OperationSequencer;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ReadableStringMap;
import org.waveprotocol.wave.model.util.ReadableStringSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the MutableDocument interface.
 *
 * TODO(danilatos): Change the implementation to act directly on the WaveDoc,
 * and sourcing ops. This is more efficient than converting points to locators,
 * which will just be wastefully converted back again internally in the WaveDoc.
 * There's a lot of innefficiencies now, which we can clear up, but ok for now
 * because they'll exercise our new code.
 *
*
 * @author danilatos@google.com (Daniel Danilatos)
 */
@SuppressWarnings("deprecation")
public class MutableDocumentImpl<N, E extends N, T extends N> implements MutableDocument<N,E,T> {

  protected final OperationSequencer<Nindo> sequencer;

  private final ReadableWDocument<N, E, T> doc;

  /**
   * @param sequencer
   * @param document
   */
  public MutableDocumentImpl(OperationSequencer<Nindo> sequencer,
      ReadableWDocument<N, E, T> document) {
    this.sequencer = sequencer;
    this.doc  = document;
  }

  protected void begin() {
    sequencer.begin();
  }

  protected void end() {
    sequencer.end();
  }

  private void consume(Builder builder) {
    sequencer.consume(builder.build());
  }

  private void consume(Nindo op) {
    sequencer.consume(op);
  }

  /**
   * Deletes a content element. Executes the delete in this document, and
   * places the caret in the deleted element's spot. Finally triggers
   * a single operation event matching the executed delete.
   *
   * @param element the element to delete
   */
  public void deleteNode(E element) {
    try {
      begin();

      // Compute caret location at the element we are deleting
      Point<N> after = Point.after(doc, element);

      consume(deleteElement(element, at(Locator.before(doc, element))));

    } finally {
      end();
    }
  }


  /**
   * Deletes all children of a content element. Executes the delete in
   * this document, and places the caret in the emptied element. Finally
   * triggers a single operation event matching the executed delete.
   *
   * @param element the element to delete
   */
  public void emptyElement(E element) {
    try {
      begin();

      // Compute caret location inside the element we are emptying
      Point<N> start = Point.start(doc, element);

      // Construct and apply delete operation
      consume(emptyElement(element, at(Locator.start(doc, element))));

    } finally {
      end();
    }
  }

  public void insertText(int location, String text) {
    Preconditions.checkPositionIndex(location, size());

    // TODO(danilatos): Get the schema constraints from the document
    // and use the corresponding permitted characters from there.
    // text = getPermittedCharactersForPoint(point).convertString(text);

    try {
      begin();
      consume(insertText(text, at(location)));
    } finally {
      end();
    }
  }

  /**
   * Inserts text at the given point. Triggers a single operation event matching the
   * executed insertion.
   *
   * @param point Point at which to insert text
   * @param text Text to insert
   */
  public void insertText(Point<N> point, String text) {
    Point.checkPoint(this, point, "MutableDocumentImpl.insertText");
    insertText(doc.getLocation(point), text);
  }

  public E appendXml(XmlStringBuilder xml) {
    return insertXml(Point.<N>end(doc.getDocumentElement()), xml);
  }

  /**
   * Parses the xml and appends it to an existing builder.
   *
   * NOTE(user): Find a better place for this utility method. Where's the
   * lowest level intersection of XmlStringBuilder and Nindo.Builder?
   *
   * @param xml
   * @param builder
   */
  public static void appendXmlToBuilder(XmlStringBuilder xml, Builder builder) {
    SimpleXmlParser parser = new SimpleXmlParser(xml.toString());

    while (parser.getCurrentType() != ItemType.END) {
      parser.next();
      switch (parser.getCurrentType()) {
        case TEXT:
          builder.characters(parser.getText());
          break;
        case START_ELEMENT:
          builder.elementStart(parser.getTagName(), new AttributesImpl(parser.getAttributes()));
          break;
        case END_ELEMENT:
          builder.elementEnd();
          break;
      }
    }
  }

  public E insertXml(Point<N> point, XmlStringBuilder xml) {
    Point.checkPoint(this, point, "MutableDocumentImpl.insertXml");

    // TODO(danilatos): Get rid of inefficient insertXml method?

    try {
      begin();

      int where = doc.getLocation(point);
      Builder builder = at(where);

      appendXmlToBuilder(xml, builder);

      consume(builder);

      Point<N> newPoint = doc.locate(where);
      if (!newPoint.isInTextNode()) {
        return doc.asElement(newPoint.getNodeAfter());
      } else {
        return null;
      }
    } finally {
      end();
    }
  }

  public Range deleteRange(int start, int end) {
    Preconditions.checkPositionIndexes(start, end, size());
    // TODO(davidbyttow/danilatos): Handle this more efficiently.
    PointRange<N> range = deleteRange(doc.locate(start), doc.locate(end));
    return new Range(doc.getLocation(range.getFirst()), doc.getLocation(range.getSecond()));
  }

  /**
   * Deletes a content range. Executes the delete in this document, and
   * places the caret in the deleted element's spot. Finally triggers
   * a single operation event matching the executed delete.
   *
   * @param start
   * @param end
   * @return The point where it would place the cursor
   */
  public PointRange<N> deleteRange(Point<N> start, Point<N> end) {
    Point.checkPoint(this, start, "MutableDocumentImpl.deleteRange start point");
    Point.checkPoint(this, end, "MutableDocumentImpl.deleteRange end point");

    int startLocation = doc.getLocation(start);
    int endLocation = doc.getLocation(end);

    if (startLocation > endLocation) {
      throw new IllegalArgumentException("MutableDocumentImpl.deleteRange: start is after end");
    }

    if (startLocation == endLocation) {
      return new PointRange<N>(start, end);
    }

    try {
      begin();

      // TODO(danilatos): Re-implement by traversing through and building just
      // one large mutation, including joins where appropriate.

      // TODO(danilatos): Add some "find common parent" functionality to the
      // lookup tree, where it can be implemented more efficiently than here.

      // The start point we want to return will always be dependent on
      // the original start location int value.
      // The end point, however, might change, so store it here.
      Point<N> newEndPoint = null;

      if (doc.isSameNode(
          Point.enclosingElement(this, start.getContainer()),
          Point.enclosingElement(this, end.getContainer()))) {
        // simple case, the two points are in the same element
        consume(deleteRangeInternal(startLocation, endLocation));
        // in this case, the end point == start point
        newEndPoint = doc.locate(startLocation);
      } else {
        // complicated case, the two points cross element boundaries
        E startEl = Point.enclosingElement(doc, start.getContainer());
        E endEl = Point.enclosingElement(doc, end.getContainer());

        // TODO(danilatos): Check if GWT's LinkedHashSet works and use that
        // instead of an array. This should be ok for now, I don't expect the
        // list to grow more than about size 3 in practice, so contains
        // should be fast.
        List<E> startAncestors = new ArrayList<E>();
        for (E el = startEl; el != null; el = doc.getParentElement(el)) {
          startAncestors.add(el);
        }

        // traverse up, to the midpoint, deleting content on the left of the end point
        E rightEl, prevRightEl;
        // this is our index into the left-side ancestors where we will start deletions on
        // that side. it's minus-one because the last one is the common ancestor, which is
        // handled in this right-side-handling loop.
        int commonAncestorIndexMinusOne = -2;
        boolean needToDoLeftmostDeleteSeparately = true;
        for (rightEl = endEl, prevRightEl = endEl; ; rightEl = doc.getParentElement(rightEl)) {
          boolean atCommonAncestor = startAncestors.contains(rightEl);

          int s; // start of this deletion section
          if (atCommonAncestor) {
            // If at common ancestor of start and end points
            commonAncestorIndexMinusOne = startAncestors.indexOf(rightEl) - 1;
            // If there are ranges on the left of the common ancestor
            needToDoLeftmostDeleteSeparately = commonAncestorIndexMinusOne >= 0;
            s = needToDoLeftmostDeleteSeparately
                // if there are, this is a range in the middle
                ? Locator.after(doc, startAncestors.get(commonAncestorIndexMinusOne))
                // otherwise it's right at the start
                : startLocation;
          } else {
            // If we're not at the common ancestor, it's a lot simpler.
            s = Locator.start(doc, rightEl);
          }
          int e = rightEl == endEl ? endLocation : Locator.before(doc, prevRightEl);
          consume(deleteRangeInternal(s, e));

          // if this is the first iteration, save the new end point after we've deleted
          // the rightmost sub-range of content
          if (newEndPoint == null) {
            newEndPoint = doc.locate(s);
          }

          prevRightEl = rightEl;

          if (atCommonAncestor) {
            break;
          }
        }

        assert commonAncestorIndexMinusOne != -2;

        // traverse down, deleting content on the right of the start point.
        // as well as it being minus-one, we also skip i == 0, because that
        // needs different logic.
        for (int i = commonAncestorIndexMinusOne; i > 0; i--) {
          consume(deleteRangeInternal(
              Locator.after(doc, startAncestors.get(i - 1)),
              Locator.end(doc, startAncestors.get(i))));
        }

        // here is said different logic
        if (needToDoLeftmostDeleteSeparately) {
          // leftmost delete
          consume(deleteRangeInternal(startLocation, Locator.end(doc, startEl)));
        }
      }

      // Maybe place caret where delete took place
      Point<N> startPoint = doc.locate(startLocation);

      // Our useful return value
      return new PointRange<N>(startPoint, newEndPoint);

    } finally {
      end();
    }
  }

  public void setElementAttribute(E element, String name, String value) {
    String currentValue = getAttribute(element, name);
    if ((value == null && currentValue == null) || (value != null && value.equals(currentValue))) {
      // Redundant.  Do nothing, no operation.
      return;
    }
    try {
      begin();
      consume(setAttribute(name, value, at(Locator.before(doc, element))));
    } finally {
      end();
    }
  }

  public void setElementAttributes(E element, Attributes attrs) {
    Preconditions.checkArgument(element != getDocumentElement(), "Cannot touch root element");
    try {
      begin();
      consume(setAttributes(attrs, at(Locator.before(doc, element))));
    } finally {
      end();
    }
  }

  public void updateElementAttributes(E element, Map<String, String> attrs) {
    Preconditions.checkArgument(element != getDocumentElement(), "Cannot touch root element");
    try {
      begin();
      consume(updateAttributes(attrs, at(Locator.before(doc, element))));
    } finally {
      end();
    }
  }

  public E createChildElement(E parent, String tag, Map<String, String> attrs) {
    return createElement(Point.<N>end(parent), tag, attrs);
  }

  public E createElement(Point<N> point, String tagName, Map<String, String> attributes) {
    // TODO(danilatos): Validate point is in document. indexed doc should throw an exception
    // when calling getLocation anyway.
    Preconditions.checkNotNull(tagName, "createElement: tagName must not be null");
    Point.checkPoint(this, point, "MutableDocumentImpl.createElement");
    try {
      begin();

      int location = doc.getLocation(point);
      consume(createElement(tagName, new AttributesImpl(attributes), at(location)));

      Point<N> result = doc.locate(location);

      return doc.asElement(result.isInTextNode()
          ? doc.getNextSibling(result.getContainer()) : result.getNodeAfter());
    } finally {
      end();
    }
  }

  public void setAnnotation(int start, int end, String key, String value) {
    Annotations.checkPersistentKey(key);
    Preconditions.checkPositionIndexes(start, end, doc.size());
    if (start == end) {
      return;
    }
    try {
      begin();
      consume(Nindo.setAnnotation(start, end, key, value));
    } finally {
      end();
    }

  }

  public void resetAnnotation(int start, int end, String key, String value) {
    Annotations.checkPersistentKey(key);
    Preconditions.checkPositionIndexes(start, end, doc.size());
    try {
      begin();
      Builder b = new Builder();
      if (start > 0) {
        b.startAnnotation(key, null);
        b.skip(start);
      }
      if (start != end) {
        b.startAnnotation(key, value);
        b.skip(end - start);
      }
      if (doc.size() != end) {
        b.startAnnotation(key, null);
        b.skip(doc.size() - end);
      }
      b.endAnnotation(key);
      consume(b.build());
    } finally {
      end();
    }
  }

  @SuppressWarnings("deprecation")
  @Deprecated
  public void resetAnnotationsInRange(int rangeStart, int rangeEnd, String key,
      List<RangedValue<String>> values) {
    if (rangeStart == rangeEnd) {
      // Nothing to do
      return;
    }

    Preconditions.checkPositionIndexes(rangeStart, rangeEnd, doc.size());
    AnnotationBuilder<N, E, T> ab = new AnnotationBuilder<N, E, T>(doc, rangeStart, rangeEnd, key);

    for (RangedValue<String> annotation : values) {
      int start = annotation.start;
      int end = annotation.end;

      Preconditions.checkPositionIndexesInRange(ab.getCurrentPos(), start, end, rangeEnd);

      if (start == end) {
        // nothing to do
        continue;
      }

      ab.setUpTo(null, start);
      ab.setUpTo(annotation.value, end);
    }

    ab.setUpTo(null, rangeEnd);

    if (ab.getDirty()) {
      try {
        begin();
        consume(ab.build());
      } finally {
        end();
      }
    }
  }

  // Helper DSL methods for building operations
  // at() creates a builder with cursor at the given location
  // Each other method does something convenient and returns the given builder

  private Builder at(int location) {
    Builder b = new Builder();
    if (location > 0) {
      b.skip(location);
    }
    return b;
  }

  private Builder createElement(String tagName, Attributes attributes, Builder builder) {
    builder.elementStart(tagName, attributes);
    builder.elementEnd();
    return builder;
  }

  private Builder insertText(String text, Builder builder) {
    builder.characters(text);
    return builder;
  }

  private Builder deleteElement(E element, Builder builder) {
    builder.deleteElementStart();
    emptyElement(element, builder);
    builder.deleteElementEnd();
    return builder;
  }

  private Builder emptyElement(E element, Builder builder) {
    for (N node = doc.getFirstChild(element); node != null; node = doc.getNextSibling(node)) {
      E elChild = doc.asElement(node);
      if (elChild != null) {
        deleteElement(elChild, builder);
      } else {
        builder.deleteCharacters(doc.getData(doc.asText(node)).length());
      }
    }
    return builder;
  }

  private Builder setAttribute(String name, String value, Builder builder) {
    builder.updateAttributes(Collections.singletonMap(name, value));
    return builder;
  }

  private Builder setAttributes(Attributes attrs, Builder builder) {
    builder.replaceAttributes(attrs);
    return builder;
  }

  private Builder updateAttributes(Map<String, String> attrs, Builder builder) {
    builder.updateAttributes(attrs);
    return builder;
  }

  private Builder deleteRangeInternal(int startLocation, int endLocation) {
    // TODO(danilatos): Delete this method when deleting is redone efficiently

    Builder builder = at(startLocation);
    Point<N> start = doc.locate(startLocation);
    Point<N> end = doc.locate(endLocation);

    assert doc.isSameNode(
        Point.enclosingElement(doc, start.getContainer()),
        Point.enclosingElement(doc, end.getContainer()))
        : "Range must be within a single element";

    N node;
    if (start.isInTextNode()) {
      if (doc.isSameNode(start.getContainer(), end.getContainer())) {
        int size = end.getTextOffset() - start.getTextOffset();
        if (size > 0) {
          builder.deleteCharacters(size);
        }
        return builder;
      } else {
        int size = doc.getLength(doc.asText(start.getContainer())) - start.getTextOffset();
        node = doc.getNextSibling(start.getContainer());
        if (size > 0) {
          builder.deleteCharacters(size);
        }
      }
    } else {
      node = start.getNodeAfter();
    }

    N stop;
    if (end.isInTextNode()) {
      stop = end.getContainer();
    } else {
      stop = end.getNodeAfter();
    }

    while (node != stop) {
      N next = doc.getNextSibling(node);
      T text = doc.asText(node);
      if (text != null) {
        builder.deleteCharacters(doc.getData(text).length());
      } else {
        deleteElement(doc.asElement(node), builder);
      }
      node = next;
    }

    if (end.isInTextNode()) {
      int size = end.getTextOffset();
      if (size > 0) {
        builder.deleteCharacters(size);
      }
    }

    return builder;
  }

  public void with(Action actionToRunWithDocument) {
    actionToRunWithDocument.exec(this);
  }

  public <V> V with(Method<V> methodToRunWithDocument) {
    return methodToRunWithDocument.exec(this);
  }

  public void hackConsume(Nindo op) {
    begin();
    try {
      consume(op);
    } finally {
      end();
    }
  }

  // Eclipse-generated delegator methods

  public int size() {
    return doc.size();
  }

  public E asElement(N node) {
    return doc.asElement(node);
  }

  public T asText(N node) {
    return doc.asText(node);
  }

  public Map<String, String> getAttributes(E element) {
    return doc.getAttributes(element);
  }

  public String getData(T textNode) {
    return doc.getData(textNode);
  }

  public E getDocumentElement() {
    return doc.getDocumentElement();
  }

  public N getFirstChild(N node) {
    return doc.getFirstChild(node);
  }

  public N getLastChild(N node) {
    return doc.getLastChild(node);
  }

  public int getLength(T textNode) {
    return doc.getLength(textNode);
  }

  public N getNextSibling(N node) {
    return doc.getNextSibling(node);
  }

  public short getNodeType(N node) {
    return doc.getNodeType(node);
  }

  public E getParentElement(N node) {
    return doc.getParentElement(node);
  }

  public N getPreviousSibling(N node) {
    return doc.getPreviousSibling(node);
  }

  public String getTagName(E element) {
    return doc.getTagName(element);
  }

  public boolean isSameNode(N node, N other) {
    return doc.isSameNode(node, other);
  }

  public String getAttribute(E element, String name) {
    return doc.getAttribute(element, name);
  }

  public int firstAnnotationChange(int start, int end, String key, String fromValue) {
    return doc.firstAnnotationChange(start, end, key, fromValue);
  }

  public int lastAnnotationChange(int start, int end, String key, String fromValue) {
    return doc.lastAnnotationChange(start, end, key, fromValue);
  }

  public String getAnnotation(int start, String key) {
    return doc.getAnnotation(start, key);
  }

  public AnnotationCursor annotationCursor(int start, int end, ReadableStringSet keys) {
    return doc.annotationCursor(start, end, keys);
  }

  public Point<N> locate(int location) {
    return doc.locate(location);
  }

  public int getLocation(N node) {
    return doc.getLocation(node);
  }

  public int getLocation(Point<N> point) {
    return doc.getLocation(point);
  }

  public void forEachAnnotationAt(int location, ReadableStringMap.ProcV<String> callback) {
    doc.forEachAnnotationAt(location, callback);
  }

  public Iterable<AnnotationInterval<String>> annotationIntervals(int start, int end,
      ReadableStringSet keys) {
    return doc.annotationIntervals(start, end, keys);
  }

  public Iterable<RangedAnnotation<String>> rangedAnnotations(int start, int end,
      ReadableStringSet keys) {
    return doc.rangedAnnotations(start, end, keys);
  }

  public ReadableStringSet knownKeys() {
    return doc.knownKeys();
  }

  public DocInitialization toInitialization() {
    return doc.toInitialization();
  }

  public String toXmlString() {
    return doc.toXmlString();
  }

  public String toString() {
    return doc.toString();
  }
}
