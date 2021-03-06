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

package org.waveprotocol.wave.model.id;

import org.waveprotocol.wave.model.id.IdSerialiser.RuntimeInvalidIdException;
import org.waveprotocol.wave.model.util.Preconditions;

/**
 * A globally-unique wavelet identifier.
 *
 * A wavelet name is a tuple of a wave identifier and a wavelet identifier.
 *
 * @author anorth@google.com (Alex North)
 */
public class WaveletName implements Comparable<WaveletName> {
  public final WaveId waveId;
  public final WaveletId waveletId;

  /** Constructs a wavelet name for a wave id and wavelet id. */
  public static WaveletName of(WaveId waveId, WaveletId waveletId) {
    return new WaveletName(waveId, waveletId);
  }

  /**
   * Constructs a wavelet name for a serialised wave id and wavelet id.
   *
   * @throws RuntimeInvalidIdException if either string is invalid
   */
  public static WaveletName of(String waveId, String waveletId) throws RuntimeInvalidIdException {
    return new WaveletName(WaveId.deserialise(waveId), WaveletId.deserialise(waveletId));
  }

  /** Private constructor to allow future instance optimisation. */
  private WaveletName(WaveId waveId, WaveletId waveletId) {
    if (waveId == null || waveletId == null) {
      Preconditions.nullPointer("Cannot create WaveletName with null value in [waveId:"
          + waveId + "] [waveletId:" + waveletId + "]");
    }
    this.waveId = waveId;
    this.waveletId = waveletId;
  }

  @Override
  public String toString() {
    return waveId + "/" + waveletId;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof WaveletName) {
      WaveletName o = (WaveletName) other;
      return waveId.equals(o.waveId) && waveletId.equals(o.waveletId);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return waveId.hashCode() * 37 + waveletId.hashCode();
  }

  @Override
  public int compareTo(WaveletName o) {
    return waveId.equals(o.waveId) ? waveletId.compareTo(o.waveletId)
                                   : waveId.compareTo(o.waveId);
  }
}
