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
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.EvaluatingDocOpCursor;
import org.waveprotocol.wave.model.document.operation.impl.AttributesUpdateImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;

/**
 * A reverser of document operations.
 *
 * @param <T> the type that the <code>finish()</code> method returns
 */
public final class DocOpInverter<T> implements EvaluatingDocOpCursor<T> {

  private final EvaluatingDocOpCursor<T> target;

  public DocOpInverter(EvaluatingDocOpCursor<T> target) {
    this.target = target;
  }

  public T finish() {
    return target.finish();
  }

  public void retain(int itemCount) {
    target.retain(itemCount);
  }

  public void characters(String chars) {
    target.deleteCharacters(chars);
  }

  public void elementStart(String type, Attributes attrs) {
    target.deleteElementStart(type, attrs);
  }

  public void elementEnd() {
    target.deleteElementEnd();
  }

  public void deleteCharacters(String chars) {
    target.characters(chars);
  }

  public void deleteElementStart(String type, Attributes attrs) {
    target.elementStart(type, attrs);
  }

  public void deleteElementEnd() {
    target.elementEnd();
  }

  public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
    target.replaceAttributes(newAttrs, oldAttrs);
  }

  public void updateAttributes(AttributesUpdate attrUpdate) {
    AttributesUpdate update = new AttributesUpdateImpl();
    // TODO: This is a little silly. We should do this a better way.
    for (int i = 0; i < attrUpdate.changeSize(); ++i) {
      update = update.composeWith(new AttributesUpdateImpl(attrUpdate.getChangeKey(i),
          attrUpdate.getNewValue(i), attrUpdate.getOldValue(i)));
    }
    target.updateAttributes(update);
  }

  public void annotationBoundary(final AnnotationBoundaryMap map) {
    // Warning: Performing multiple reversals can cause multiple wrappers to be created.
    // TODO: Maybe we should change this so that this issue doesn't occur.
    target.annotationBoundary(new AnnotationBoundaryMap() {

      public int changeSize() {
        return map.changeSize();
      }

      public String getChangeKey(int changeIndex) {
        return map.getChangeKey(changeIndex);
      }

      public String getOldValue(int changeIndex) {
        return map.getNewValue(changeIndex);
      }

      public String getNewValue(int changeIndex) {
        return map.getOldValue(changeIndex);
      }

      public int endSize() {
        return map.endSize();
      }

      public String getEndKey(int endIndex) {
        return map.getEndKey(endIndex);
      }

    });
  }

  public static BufferedDocOp invert(DocOp input) {
    DocOpInverter<BufferedDocOp> inverter = new DocOpInverter<BufferedDocOp>(new DocOpBuffer());
    input.apply(inverter);
    return inverter.finish();
  }

}
