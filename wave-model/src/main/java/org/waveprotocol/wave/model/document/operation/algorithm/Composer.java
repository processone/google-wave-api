/**
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.waveprotocol.wave.model.document.operation.algorithm;

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.BufferedDocInitialization;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.DocOpCursor;
import org.waveprotocol.wave.model.document.operation.EvaluatingDocOpCursor;
import org.waveprotocol.wave.model.document.operation.impl.AnnotationBoundaryMapImpl;
import org.waveprotocol.wave.model.document.operation.impl.AttributesUpdateImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.operation.OperationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A utility class for composing document operations.
 *
 * TODO: Make detection of illegal compositions more thorough.
 */
public final class Composer {

  private abstract class AnnotationQueue {

    private final List<AnnotationBoundaryMap> events = new ArrayList<AnnotationBoundaryMap>();

    /**
     * Do not use this directly.  This is only here for subclasses to override.
     */
    abstract void unqueue(AnnotationBoundaryMap map);

    /**
     * Register a change in annotations at the current location in the mutations
     * being processed as part of a transformation.
     *
     * @param map The annotation transition information.
     */
    final void queue(AnnotationBoundaryMap map) {
      events.add(map);
    }

    /**
     * Flushes any queued annotation events.
     */
    final void flush() {
      for (AnnotationBoundaryMap map : events) {
        unqueue(map);
      }
      events.clear();
    }

  }

  private static class ComposeException extends RuntimeException {

    ComposeException(String message) {
      super(message);
    }

  }

  private static abstract class Target implements DocOpCursor {

    abstract boolean isPostTarget();

  }

  private abstract class PreTarget extends Target {

    public final void deleteCharacters(String chars) {
      preAnnotationQueue.flush();
      normalizer.deleteCharacters(chars);
    }

    public final void deleteElementStart(String type, Attributes attrs) {
      preAnnotationQueue.flush();
      normalizer.deleteElementStart(type, attrs);
    }

    public final void deleteElementEnd() {
      preAnnotationQueue.flush();
      normalizer.deleteElementEnd();
    }

    public final void annotationBoundary(AnnotationBoundaryMap map) {
      preAnnotationQueue.queue(map);
    }

    final boolean isPostTarget() {
      return false;
    }

  }

  private abstract class PostTarget extends Target {

    public final void characters(String chars) {
      postAnnotationQueue.flush();
      normalizer.characters(chars);
    }

    public final void elementStart(String type, Attributes attrs) {
      postAnnotationQueue.flush();
      normalizer.elementStart(type, attrs);
    }

    public final void elementEnd() {
      postAnnotationQueue.flush();
      normalizer.elementEnd();
    }

    public final void annotationBoundary(AnnotationBoundaryMap map) {
      postAnnotationQueue.queue(map);
    }

    final boolean isPostTarget() {
      return true;
    }

  }

  private final class DefaultPreTarget extends PreTarget {

    public void retain(int itemCount) {
      target = new RetainPostTarget(itemCount);
    }

    public void characters(String chars) {
      target = new CharactersPostTarget(chars);
    }

    public void elementStart(String type, Attributes attrs) {
      target = new ElementStartPostTarget(type, attrs);
    }

    public void elementEnd() {
      target = new ElementEndPostTarget();
    }

    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      target = new ReplaceAttributesPostTarget(oldAttrs, newAttrs);
    }

    public void updateAttributes(AttributesUpdate attrUpdate) {
      target = new UpdateAttributesPostTarget(attrUpdate);
    }

  }

  private final class RetainPreTarget extends PreTarget {

    private int itemCount;

    RetainPreTarget(int itemCount) {
      this.itemCount = itemCount;
    }

    public void retain(int itemCount) {
      flushAnnotations();
      if (itemCount <= this.itemCount) {
        normalizer.retain(itemCount);
        cancelRetain(itemCount);
      } else {
        normalizer.retain(this.itemCount);
        target = new RetainPostTarget(itemCount - this.itemCount);
      }
    }

    public void characters(String chars) {
      flushAnnotations();
      if (chars.length() <= itemCount) {
        normalizer.characters(chars);
        cancelRetain(chars.length());
      } else {
        normalizer.characters(chars.substring(0, itemCount));
        target = new CharactersPostTarget(chars.substring(itemCount));
      }
    }

    public void elementStart(String type, Attributes attrs) {
      flushAnnotations();
      normalizer.elementStart(type, attrs);
      cancelRetain(1);
    }

    public void elementEnd() {
      flushAnnotations();
      normalizer.elementEnd();
      cancelRetain(1);
    }

    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      flushAnnotations();
      normalizer.replaceAttributes(oldAttrs, newAttrs);
      cancelRetain(1);
    }

    public void updateAttributes(AttributesUpdate attrsUpdate) {
      flushAnnotations();
      normalizer.updateAttributes(attrsUpdate);
      cancelRetain(1);
    }

    private void cancelRetain(int size) {
      if (size < itemCount) {
        itemCount -= size;
      } else {
        target = defaultTarget;
      }
    }

  }

  private final class DeleteCharactersPreTarget extends PreTarget {

    private String chars;

    DeleteCharactersPreTarget(String chars) {
      this.chars = chars;
    }

    public void retain(int itemCount) {
      flushAnnotations();
      if (itemCount <= chars.length()) {
        normalizer.deleteCharacters(chars.substring(0, itemCount));
        cancelDeleteCharacters(itemCount);
      } else {
        normalizer.deleteCharacters(chars);
        target = new RetainPostTarget(itemCount - chars.length());
      }
    }

    public void characters(String chars) {
      flushAnnotations();
      if (chars.length() <= this.chars.length()) {
        cancelDeleteCharacters(chars.length());
      } else {
        target = new CharactersPostTarget(chars.substring(this.chars.length()));
      }
    }

    public void elementStart(String type, Attributes attrs) {
      throw new ComposeException("Illegal composition");
    }

    public void elementEnd() {
      throw new ComposeException("Illegal composition");
    }

    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      throw new ComposeException("Illegal composition");
    }

    public void updateAttributes(AttributesUpdate attrUpdate) {
      throw new ComposeException("Illegal composition");
    }

    private void cancelDeleteCharacters(int size) {
      if (size < chars.length()) {
        chars = chars.substring(size);
      } else {
        target = defaultTarget;
      }
    }

  }

  private final class RetainPostTarget extends PostTarget {

    private int itemCount;

    RetainPostTarget(int itemCount) {
      this.itemCount = itemCount;
    }

    public void retain(int itemCount) {
      flushAnnotations();
      if (itemCount <= this.itemCount) {
        normalizer.retain(itemCount);
        cancelRetain(itemCount);
      } else {
        normalizer.retain(this.itemCount);
        target = new RetainPreTarget(itemCount - this.itemCount);
      }
    }

    public void deleteCharacters(String chars) {
      flushAnnotations();
      if (chars.length() <= itemCount) {
        normalizer.deleteCharacters(chars);
        cancelRetain(chars.length());
      } else {
        normalizer.deleteCharacters(chars.substring(0, itemCount));
        target = new DeleteCharactersPreTarget(chars.substring(itemCount));
      }
    }

    public void deleteElementStart(String type, Attributes attrs) {
      flushAnnotations();
      normalizer.deleteElementStart(type, attrs);
      cancelRetain(1);
    }

    public void deleteElementEnd() {
      flushAnnotations();
      normalizer.deleteElementEnd();
      cancelRetain(1);
    }

    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      flushAnnotations();
      normalizer.replaceAttributes(oldAttrs, newAttrs);
      cancelRetain(1);
    }

    public void updateAttributes(AttributesUpdate attrUpdate) {
      flushAnnotations();
      normalizer.updateAttributes(attrUpdate);
      cancelRetain(1);
    }

    private void cancelRetain(int size) {
      if (size < itemCount) {
        itemCount -= size;
      } else {
        target = defaultTarget;
      }
    }

  }

  private final class CharactersPostTarget extends PostTarget {

    private String chars;

    CharactersPostTarget(String chars) {
      this.chars = chars;
    }

    public void retain(int itemCount) {
      flushAnnotations();
      if (itemCount <= chars.length()) {
        normalizer.characters(chars.substring(0, itemCount));
        cancelCharacters(itemCount);
      } else {
        normalizer.characters(chars);
        target = new RetainPreTarget(itemCount - chars.length());
      }
    }

    public void deleteCharacters(String chars) {
      flushAnnotations();
      if (chars.length() <= this.chars.length()) {
        cancelCharacters(chars.length());
      } else {
        target = new DeleteCharactersPreTarget(chars.substring(this.chars.length()));
      }
    }

    public void deleteElementStart(String type, Attributes attrs) {
      throw new ComposeException("Illegal composition");
    }

    public void deleteElementEnd() {
      throw new ComposeException("Illegal composition");
    }

    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      throw new ComposeException("Illegal composition");
    }

    public void updateAttributes(AttributesUpdate attrUpdate) {
      throw new ComposeException("Illegal composition");
    }

    private void cancelCharacters(int size) {
      if (size < chars.length()) {
        chars = chars.substring(size);
      } else {
        target = defaultTarget;
      }
    }

  }

  private final class ElementStartPostTarget extends PostTarget {

    private String type;
    private Attributes attrs;

    ElementStartPostTarget(String type, Attributes attrs) {
      this.type = type;
      this.attrs = attrs;
    }

    public void retain(int itemCount) {
      flushAnnotations();
      normalizer.elementStart(type, attrs);
      if (itemCount > 1) {
        target = new RetainPreTarget(itemCount - 1);
      } else {
        target = defaultTarget;
      }
    }

    public void deleteCharacters(String chars) {
      throw new ComposeException("Illegal composition");
    }

    public void deleteElementStart(String type, Attributes attrs) {
      flushAnnotations();
      target = defaultTarget;
    }

    public void deleteElementEnd() {
      throw new ComposeException("Illegal composition");
    }

    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      flushAnnotations();
      normalizer.elementStart(type, newAttrs);
      target = defaultTarget;
    }

    public void updateAttributes(AttributesUpdate attrUpdate) {
      flushAnnotations();
      normalizer.elementStart(type, attrs.updateWith(attrUpdate));
      target = defaultTarget;
    }

  }

  private final class ElementEndPostTarget extends PostTarget {

    public void retain(int itemCount) {
      flushAnnotations();
      normalizer.elementEnd();
      if (itemCount > 1) {
        target = new RetainPreTarget(itemCount - 1);
      } else {
        target = defaultTarget;
      }
    }

    public void deleteCharacters(String chars) {
      throw new ComposeException("Illegal composition");
    }

    public void deleteElementStart(String type, Attributes attrs) {
      throw new ComposeException("Illegal composition");
    }

    public void deleteElementEnd() {
      flushAnnotations();
      target = defaultTarget;
    }

    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      throw new ComposeException("Illegal composition");
    }

    public void updateAttributes(AttributesUpdate attrUpdate) {
      throw new ComposeException("Illegal composition");
    }

  }

  private final class ReplaceAttributesPostTarget extends PostTarget {

    Attributes oldAttrs;
    Attributes newAttrs;

    ReplaceAttributesPostTarget(Attributes oldAttrs, Attributes newAttrs) {
      this.oldAttrs = oldAttrs;
      this.newAttrs = newAttrs;
    }

    public void retain(int itemCount) {
      flushAnnotations();
      normalizer.replaceAttributes(oldAttrs, newAttrs);
      if (itemCount > 1) {
        target = new RetainPreTarget(itemCount - 1);
      } else {
        target = defaultTarget;
      }
    }

    public void deleteCharacters(String chars) {
      throw new ComposeException("Illegal composition");
    }

    public void deleteElementStart(String type, Attributes attrs) {
      flushAnnotations();
      normalizer.deleteElementStart(type, oldAttrs);
      target = defaultTarget;
    }

    public void deleteElementEnd() {
      throw new ComposeException("Illegal composition");
    }

    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      flushAnnotations();
      normalizer.replaceAttributes(this.oldAttrs, newAttrs);
      target = defaultTarget;
    }

    public void updateAttributes(AttributesUpdate attrUpdate) {
      flushAnnotations();
      normalizer.replaceAttributes(oldAttrs, newAttrs.updateWith(attrUpdate));
      target = defaultTarget;
    }

  }

  private final class UpdateAttributesPostTarget extends PostTarget {

    AttributesUpdate attrUpdate;

    UpdateAttributesPostTarget(AttributesUpdate attrUpdate) {
      this.attrUpdate = attrUpdate;
    }

    public void retain(int itemCount) {
      flushAnnotations();
      normalizer.updateAttributes(attrUpdate);
      if (itemCount > 1) {
        target = new RetainPreTarget(itemCount - 1);
      } else {
        target = defaultTarget;
      }
    }

    public void deleteCharacters(String chars) {
      throw new ComposeException("Illegal composition");
    }

    public void deleteElementStart(String type, Attributes attrs) {
      flushAnnotations();
      normalizer.deleteElementStart(type, attrs.updateWith(invertUpdate(attrUpdate)));
      target = defaultTarget;
    }

    public void deleteElementEnd() {
      throw new ComposeException("Illegal composition");
    }

    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      flushAnnotations();
      normalizer.replaceAttributes(oldAttrs.updateWith(invertUpdate(attrUpdate)), newAttrs);
      target = defaultTarget;
    }

    public void updateAttributes(AttributesUpdate attrUpdate) {
      flushAnnotations();
      normalizer.updateAttributes(this.attrUpdate.composeWith(attrUpdate));
      target = defaultTarget;
    }

  }

  private final class FinisherPostTarget extends PostTarget {

    public void retain(int itemCount) {
      throw new ComposeException("Illegal composition");
    }

    public void deleteCharacters(String chars) {
      throw new ComposeException("Illegal composition");
    }

    public void deleteElementStart(String type, Attributes attrs) {
      throw new ComposeException("Illegal composition");
    }

    public void deleteElementEnd() {
      throw new ComposeException("Illegal composition");
    }

    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      throw new ComposeException("Illegal composition");
    }

    public void updateAttributes(AttributesUpdate attrUpdate) {
      throw new ComposeException("Illegal composition");
    }

  }

  /**
   * The currently active annotations in the first operation. This maps keys to
   * pairs representing the old annotation value and the new annotation value.
   */
  private final Map<String, ValueUpdate> preAnnotations =
      new HashMap<String, ValueUpdate>();

  /**
   * The currently active annotations in the second operation. This maps keys to
   * pairs representing the old annotation value and the new annotation value.
   */
  private final Map<String, ValueUpdate> postAnnotations =
      new HashMap<String, ValueUpdate>();

  private final AnnotationQueue preAnnotationQueue = new AnnotationQueue() {

    @Override
    void unqueue(AnnotationBoundaryMap map) {
      // TODO: This seems pretty awkward. Perhaps we should give
      // AnnotationBoundaryMapImpl an easier builder to use.
      List<String> endKeys = new ArrayList<String>();
      List<String> changeKeys = new ArrayList<String>();
      List<String> changeOldValues = new ArrayList<String>();
      List<String> changeNewValues = new ArrayList<String>();
      for (int i = 0; i < map.endSize(); ++i) {
        String key = map.getEndKey(i);
        ValueUpdate postValues = postAnnotations.get(key);
        if (postValues != null) {
          changeKeys.add(key);
          changeOldValues.add(postValues.oldValue);
          changeNewValues.add(postValues.newValue);
        } else {
          endKeys.add(key);
        }
        preAnnotations.remove(key);
      }
      for (int i = 0; i < map.changeSize(); ++i) {
        String key = map.getChangeKey(i);
        String oldValue = map.getOldValue(i);
        String newValue = map.getNewValue(i);
        ValueUpdate postValues = postAnnotations.get(key);
        changeKeys.add(key);
        changeOldValues.add(oldValue);
        if (postValues != null) {
          changeNewValues.add(postValues.newValue);
        } else {
          changeNewValues.add(newValue);
        }
        preAnnotations.put(key, new ValueUpdate(oldValue, newValue));
      }
      normalizer.annotationBoundary(new AnnotationBoundaryMapImpl(
          endKeys.toArray(new String[0]),
          changeKeys.toArray(new String[0]),
          changeOldValues.toArray(new String[0]),
          changeNewValues.toArray(new String[0])));
    }

  };

  private final AnnotationQueue postAnnotationQueue = new AnnotationQueue() {

    @Override
    void unqueue(AnnotationBoundaryMap map) {
      // TODO: This seems pretty awkward. Perhaps we should give
      // AnnotationBoundaryMapImpl an easier builder to use.
      List<String> endKeys = new ArrayList<String>();
      List<String> changeKeys = new ArrayList<String>();
      List<String> changeOldValues = new ArrayList<String>();
      List<String> changeNewValues = new ArrayList<String>();
      for (int i = 0; i < map.endSize(); ++i) {
        String key = map.getEndKey(i);
        ValueUpdate preValues = preAnnotations.get(key);
        if (preValues != null) {
          changeKeys.add(key);
          changeOldValues.add(preValues.oldValue);
          changeNewValues.add(preValues.newValue);
        } else {
          endKeys.add(key);
        }
        postAnnotations.remove(key);
      }
      for (int i = 0; i < map.changeSize(); ++i) {
        String key = map.getChangeKey(i);
        String oldValue = map.getOldValue(i);
        String newValue = map.getNewValue(i);
        ValueUpdate preValues = preAnnotations.get(key);
        changeKeys.add(key);
        changeNewValues.add(newValue);
        if (preValues != null) {
          changeOldValues.add(preValues.oldValue);
        } else {
          changeOldValues.add(oldValue);
        }
        postAnnotations.put(key, new ValueUpdate(oldValue, newValue));
      }
      normalizer.annotationBoundary(new AnnotationBoundaryMapImpl(
          endKeys.toArray(new String[0]),
          changeKeys.toArray(new String[0]),
          changeOldValues.toArray(new String[0]),
          changeNewValues.toArray(new String[0])));
    }

  };

  private final EvaluatingDocOpCursor<BufferedDocOp> normalizer =
      OperationNormalizer.createNormalizer(new DocOpBuffer());

  private final Target defaultTarget = new DefaultPreTarget();

  private Target target;

  private Composer() {}

  private BufferedDocOp composeOperations(BufferedDocOp op1, BufferedDocOp op2)
      throws OperationException {
    target = defaultTarget;
    int op1Index = 0;
    int op2Index = 0;
    while (op1Index < op1.size()) {
      op1.applyComponent(op1Index++, target);
      while (target.isPostTarget()) {
        if (op2Index >= op2.size()) {
          throw new OperationException("Document size mismatch: "
              + "op1 resulting length=" + DocOpUtil.resultingDocumentLength(op1)
              + ", op2 initial length=" + DocOpUtil.initialDocumentLength(op2));
        }
        op2.applyComponent(op2Index++, target);
      }
    }
    if (op2Index < op2.size()) {
      target = new FinisherPostTarget();
      while (op2Index < op2.size()) {
        op2.applyComponent(op2Index++, target);
      }
    }
    flushAnnotations();
    return normalizer.finish();
  }

  /**
   * Returns the composition of two operations.
   *
   * @param op1 the first operation
   * @param op2 the second operation
   * @return the result of the composition
   * @throws OperationException if applying op1 followed by op2 would be invalid
   */
  public static BufferedDocOp compose(BufferedDocOp op1, BufferedDocOp op2)
      throws OperationException {
    try {
      return new Composer().composeOperations(op1, op2);
    } catch (ComposeException e) {
      throw new OperationException(e.getMessage());
    }
  }

  /**
   * Returns the composition of an initialization and an operation.
   *
   * This overload of the method returns a more specific subtype than
   * the other one.
   *
   * @param op1 the first operation
   * @param op2 the second operation
   * @return the result of the composition
   * @throws OperationException if applying op1 followed by op2 would be invalid
   */
  public static BufferedDocInitialization compose(BufferedDocInitialization op1, BufferedDocOp op2)
      throws OperationException {
    return DocOpUtil.asInitialization(compose((BufferedDocOp) op1, op2));
  }

  /**
   * Compose operations.
   *
   * TODO: Rewrite to have proper exceptions-throwing.
   *
   * @param operations an iterator through the operations to compose
   * @return the result of the composition
   */
  // TODO: DocOpCollector's API is flawed; it should throw OperationException, and so should this.
  public static BufferedDocOp compose(Iterable<BufferedDocOp> operations) {
    DocOpCollector collector = new DocOpCollector();
    for (BufferedDocOp operation : operations) {
      collector.add(operation);
    }
    return collector.composeAll();
  }

  private void flushAnnotations() {
    preAnnotationQueue.flush();
    postAnnotationQueue.flush();
  }

  private static AttributesUpdate invertUpdate(AttributesUpdate attrUpdate) {
    AttributesUpdate inverseUpdate = new AttributesUpdateImpl();
    // TODO: This is a little silly. We should do this a better way.
    for (int i = 0; i < attrUpdate.changeSize(); ++i) {
      inverseUpdate = inverseUpdate.composeWith(new AttributesUpdateImpl(
          attrUpdate.getChangeKey(i), attrUpdate.getNewValue(i), attrUpdate.getOldValue(i)));
    }
    return inverseUpdate;
  }

}
