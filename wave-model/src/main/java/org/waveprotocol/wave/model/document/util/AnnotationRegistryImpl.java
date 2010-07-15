// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.AnnotationBehaviour;
import org.waveprotocol.wave.model.document.AnnotationMutationHandler;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.StringMap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * StringMap based implementation
 *
 * @author danilatos@google.com (Daniel Danilatos)
 *
 */
public class AnnotationRegistryImpl implements AnnotationRegistry {

  private final StringMap<AnnotationMutationHandler> handlers = CollectionUtils.createStringMap();
  private final StringMap<AnnotationBehaviour> behaviours = CollectionUtils.createStringMap();

  private final List<AnnotationRegistryImpl> children =
      new ArrayList<AnnotationRegistryImpl>();

  public AnnotationRegistryImpl(AnnotationRegistryImpl copyFrom) {
    if (copyFrom != null) {
      handlers.putAll(copyFrom.handlers);
      behaviours.putAll(copyFrom.behaviours);
    }
  }

  /**
   * Create a copy of this registry for further extension.
   *
   * TODO(user): Consider more efficient propagation of changes to children.
   */
  public AnnotationRegistryImpl createExtension() {
    AnnotationRegistryImpl child = new AnnotationRegistryImpl(this);
    children.add(child);
    return child;
  }

  public void registerHandler(String prefix, AnnotationMutationHandler handler) {
    Util.validatePrefix(prefix);
    handlers.put(prefix, handler);
    for (AnnotationRegistryImpl child : children) {
      child.registerHandler(prefix, handler);
    }
  }

  public void registerBehaviour(String prefix, AnnotationBehaviour behaviour) {
    Util.validatePrefix(prefix);
    behaviours.put(prefix, behaviour);
    for (AnnotationRegistryImpl child : children) {
      child.registerBehaviour(prefix, behaviour);
    }
  }

  public Iterator<AnnotationMutationHandler> getHandlers(final String prefix) {
    final String parts = prefix + "/";

    return new Iterator<AnnotationMutationHandler>() {
      int fromIndex = -1;
      AnnotationMutationHandler next;

      {
        getNext();
      }

      public boolean hasNext() {
        return next != null;
      }

      public AnnotationMutationHandler next() {
        AnnotationMutationHandler ret = next;
        getNext();
        return ret;
      }

      private void getNext() {
        while ((fromIndex = parts.indexOf('/', fromIndex + 1)) != -1) {
          AnnotationMutationHandler handler = handlers.get(parts.substring(0, fromIndex));
          if (handler != null) {
            next = handler;
            return;
          }
        }
        next = null;
      }

      public void remove() {
        throw new UnsupportedOperationException("getHandlers iterator: remove");
      }
    };
  }

  public Iterator<AnnotationBehaviour> getBehaviours(final String prefix) {
    final String parts = prefix + "/";

    return new Iterator<AnnotationBehaviour>() {
      int fromIndex = -1;
      AnnotationBehaviour next;

      {
        getNext();
      }

      public boolean hasNext() {
        return next != null;
      }

      public AnnotationBehaviour next() {
        AnnotationBehaviour ret = next;
        getNext();
        return ret;
      }

      private void getNext() {
        while ((fromIndex = parts.indexOf('/', fromIndex + 1)) != -1) {
          AnnotationBehaviour behaviour = behaviours.get(parts.substring(0, fromIndex));
          if (behaviour != null) {
            next = behaviour;
            return;
          }
        }
        next = null;
      }

      public void remove() {
        throw new UnsupportedOperationException("getBehaviours iterator: remove");
      }
    };
  }
}
