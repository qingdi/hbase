/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hbase.io.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.apache.hadoop.classification.InterfaceAudience;

import com.google.common.base.Preconditions;

/*
 * It seems like as soon as somebody sets himself to the task of creating VInt encoding, his mind
 * blanks out for a split-second and he starts the work by wrapping it in the most convoluted
 * interface he can come up with. Custom streams that allocate memory, DataOutput that is only used
 * to write single bytes... We operate on simple streams. Thus, we are going to have a simple
 * implementation copy-pasted from protobuf Coded*Stream.
 */
@InterfaceAudience.Private
public class StreamUtils {

  public static void writeRawVInt32(OutputStream output, int value) throws IOException {
    assert value >= 0;
    while (true) {
      if ((value & ~0x7F) == 0) {
        output.write(value);
        return;
      } else {
        output.write((value & 0x7F) | 0x80);
        value >>>= 7;
      }
    }
  }

  public static int readRawVarint32(InputStream input) throws IOException {
    byte tmp = (byte) input.read();
    if (tmp >= 0) {
      return tmp;
    }
    int result = tmp & 0x7f;
    if ((tmp = (byte) input.read()) >= 0) {
      result |= tmp << 7;
    } else {
      result |= (tmp & 0x7f) << 7;
      if ((tmp = (byte) input.read()) >= 0) {
        result |= tmp << 14;
      } else {
        result |= (tmp & 0x7f) << 14;
        if ((tmp = (byte) input.read()) >= 0) {
          result |= tmp << 21;
        } else {
          result |= (tmp & 0x7f) << 21;
          result |= (tmp = (byte) input.read()) << 28;
          if (tmp < 0) {
            // Discard upper 32 bits.
            for (int i = 0; i < 5; i++) {
              if (input.read() >= 0) {
                return result;
              }
            }
            throw new IOException("Malformed varint");
          }
        }
      }
    }
    return result;
  }

  public static int readRawVarint32(ByteBuffer input) throws IOException {
    byte tmp = input.get();
    if (tmp >= 0) {
      return tmp;
    }
    int result = tmp & 0x7f;
    if ((tmp = input.get()) >= 0) {
      result |= tmp << 7;
    } else {
      result |= (tmp & 0x7f) << 7;
      if ((tmp = input.get()) >= 0) {
        result |= tmp << 14;
      } else {
        result |= (tmp & 0x7f) << 14;
        if ((tmp = input.get()) >= 0) {
          result |= tmp << 21;
        } else {
          result |= (tmp & 0x7f) << 21;
          result |= (tmp = input.get()) << 28;
          if (tmp < 0) {
            // Discard upper 32 bits.
            for (int i = 0; i < 5; i++) {
              if (input.get() >= 0) {
                return result;
              }
            }
            throw new IOException("Malformed varint");
          }
        }
      }
    }
    return result;
  }

  public static short toShort(byte hi, byte lo) {
    short s = (short) (((hi & 0xFF) << 8) | (lo & 0xFF));
    Preconditions.checkArgument(s >= 0);
    return s;
  }

  public static void writeShort(OutputStream out, short v) throws IOException {
    Preconditions.checkArgument(v >= 0);
    out.write((byte) (0xff & (v >> 8)));
    out.write((byte) (0xff & v));
  }
}