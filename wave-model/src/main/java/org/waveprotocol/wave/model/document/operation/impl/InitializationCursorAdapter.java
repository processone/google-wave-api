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

package org.waveprotocol.wave.model.document.operation.impl;

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.DocInitializationCursor;
import org.waveprotocol.wave.model.document.operation.DocOpCursor;

public class InitializationCursorAdapter implements DocOpCursor {

  private final DocInitializationCursor inner;

  public InitializationCursorAdapter(DocInitializationCursor inner) {
    this.inner = inner;
  }

  public void annotationBoundary(AnnotationBoundaryMap map) {
    inner.annotationBoundary(map);
  }

  public void characters(String chars) {
    inner.characters(chars);
  }

  public void elementEnd() {
    inner.elementEnd();
  }

  public void elementStart(String type, Attributes attrs) {
    inner.elementStart(type, attrs);
  }

  public void deleteCharacters(String chars) {
    throw new UnsupportedOperationException("deleteCharacters");
  }

  public void deleteElementEnd() {
    throw new UnsupportedOperationException("deleteElementEnd");
  }

  public void deleteElementStart(String type, Attributes attrs) {
    throw new UnsupportedOperationException("deleteElementStart");
  }

  public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
    throw new UnsupportedOperationException("replaceAttributes");
  }

  public void retain(int itemCount) {
    throw new UnsupportedOperationException("retain");
  }

  public void updateAttributes(AttributesUpdate attrUpdate) {
    throw new UnsupportedOperationException("updateAttributes");
  }


}
