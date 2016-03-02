/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.qpid.proton.engine.impl;

import org.apache.qpid.proton.engine.WebSocketHandler;

import java.io.*;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Random;

public class WebSocketHandlerImpl implements WebSocketHandler
{
    @Override
    public String createUpgradeRequest(
            String hostName,
            String webSocketPath,
            int webSocketPort,
            String webSocketProtocol,
            Map<String, String> additionalHeaders)

    {
        WebSocketUpgradeRequest webSocketUpgradeRequest = new WebSocketUpgradeRequest(hostName, webSocketPath, webSocketPort, webSocketProtocol, additionalHeaders);
        return webSocketUpgradeRequest.createUpgradeRequest();
    }

    @Override
    public Boolean validateUpgradeReply(ByteBuffer buffer) {
        int size = buffer.remaining();
        if (size > 0)
        {
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            buffer.compact();
            return true;
        }
        return false;
    }

    @Override
    public void wrapBuffer(ByteBuffer srcBuffer, ByteBuffer dstBuffer) {
        //  +---------------------------------------------------------------+
        //  0                   1                   2                   3   |
        //  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 |
        //  +-+-+-+-+-------+-+-------------+-------------------------------+
        //  |F|R|R|R| opcode|M| Payload len |   Extended payload length     |
        //  |I|S|S|S|  (4)  |A|     (7)     |            (16/64)            |
        //  |N|V|V|V|       |S|             |  (if payload len==126/127)    |
        //  | |1|2|3|       |K|             |                               |
        //  +-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - +
        //  |     Extended payload length continued, if payload len == 127  |
        //  + - - - - - - - - - - - - - - - +-------------------------------+
        //  |                               | Masking-key, if MASK set to 1 |
        //  +-------------------------------+-------------------------------+
        //  | Masking-key (continued)       |          Payload Data         |
        //  +-------------------------------- - - - - - - - - - - - - - - - +
        //  :                     Payload Data continued ...                :
        //  + - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - +
        //  |                     Payload Data continued ...                |
        //  +---------------------------------------------------------------+
        if (srcBuffer.limit() > 0)
        {
            // We always send final WebSocket frame
            // RFC: Indicates that this is the final fragment in a message.
            final byte FINBIT_SET = (byte) 0x80;

            // We always send binary message (AMQP)
            // RFC: %x2 denotes a binary frame
            final byte OPCODE_BINARY = 0x2;

            // We always send masked data
            // RFC: "client MUST mask all frames that it sends to the server"
            final byte MASKBIT_SET = (byte) 0x80;
            final byte[] MASKING_KEY = createRandomMaskingKey();

            // Minimum header length is 6
            final byte MIN_HEADER_LENGTH = 6;

            // Get data length
            final int DATA_LENGTH = srcBuffer.remaining();

            // Auto growing buffer for the WS frame, initialized to minimum size
            ByteArrayOutputStream webSocketFrame = new ByteArrayOutputStream(MIN_HEADER_LENGTH + DATA_LENGTH);

            // Create the first byte
            byte firstByte = (byte) (FINBIT_SET | OPCODE_BINARY);
            webSocketFrame.write(firstByte);

            // Create the second byte
            // RFC: The length of the "Payload data", in bytes: if 0-125, that is the payload length.
            if (DATA_LENGTH < 126) {
                // RFC: "client MUST mask all frames that it sends to the server"
                byte payloadLength = (byte) (DATA_LENGTH | MASKBIT_SET);
                webSocketFrame.write(payloadLength);
            }
            // RFC: If 126, the following 2 bytes interpreted as a 16-bit unsigned integer are the payload length
            else if (DATA_LENGTH <=  65535) {
                // Create payload byte
                byte payloadLength = (byte) (126 | MASKBIT_SET);
                webSocketFrame.write(payloadLength);

                // Create extended length bytes
                byte[] extendedLengthBytes = ByteBuffer.allocate(4).putInt(DATA_LENGTH).array();
                webSocketFrame.write(extendedLengthBytes[2]);
                webSocketFrame.write(extendedLengthBytes[3]);
            }
            // RFC: If 127, the following 8 bytes interpreted as a 64-bit unsigned integer (the most significant bit MUST be 0) are the payload length.
            // No need for if because if it is longer than what 8 byte length can hold... or bets are off anyway
            else {
                byte payloadLength = (byte) (127 | MASKBIT_SET);
                webSocketFrame.write(payloadLength);

                // ByteBuffer length stored in an integer so the max length we can have is 4 bytes
                // In this case the first four bytes are always zero
                webSocketFrame.write(0);
                webSocketFrame.write(0);
                webSocketFrame.write(0);
                webSocketFrame.write(0);

                // Create the least significant 4 bytes
                webSocketFrame.write((byte) (DATA_LENGTH >>> 24));
                webSocketFrame.write((byte) (DATA_LENGTH >>> 16));
                webSocketFrame.write((byte) (DATA_LENGTH >>> 8));
                webSocketFrame.write((byte) (DATA_LENGTH));
            }

            // Write mask
            webSocketFrame.write(MASKING_KEY[0]);
            webSocketFrame.write(MASKING_KEY[1]);
            webSocketFrame.write(MASKING_KEY[2]);
            webSocketFrame.write(MASKING_KEY[3]);

            // Write masked data
            for (int i = 0; i < DATA_LENGTH; i++) {
                byte nextByte = srcBuffer.get();
                nextByte ^= MASKING_KEY[i % 4];
                webSocketFrame.write(nextByte);
            }

            // Copy frame to destination buffer
            dstBuffer.put(webSocketFrame.toByteArray());
        }
    }

    @Override
    public void unwrapBuffer(ByteBuffer buffer) {
        //  +---------------------------------------------------------------+
        //  0                   1                   2                   3   |
        //  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 |
        //  +-+-+-+-+-------+-+-------------+-------------------------------+
        //  |F|R|R|R| opcode|M| Payload len |   Extended payload length     |
        //  |I|S|S|S|  (4)  |A|     (7)     |            (16/64)            |
        //  |N|V|V|V|       |S|             |  (if payload len==126/127)    |
        //  | |1|2|3|       |K|             |                               |
        //  +-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - +
        //  |     Extended payload length continued, if payload len == 127  |
        //  + - - - - - - - - - - - - - - - +-------------------------------+
        //  |                               | Masking-key, if MASK set to 1 |
        //  +-------------------------------+-------------------------------+
        //  | Masking-key (continued)       |          Payload Data         |
        //  +-------------------------------- - - - - - - - - - - - - - - - +
        //  :                     Payload Data continued ...                :
        //  + - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - +
        //  |                     Payload Data continued ...                |
        //  +---------------------------------------------------------------+
        if (buffer.limit() > 0) {
            byte firstByte = buffer.get();
            byte secondByte = buffer.get();
            byte length = (byte) (secondByte & 0x7f);
        }



//        if (buffer.limit() == 0) {
//            return;
//        }
//        else {
//            byte firstByte = buffer.get();
//            byte finBit = (byte) (firstByte & 0x80);
//            byte opcode = (byte) (firstByte & 0x0f);
//
//            byte secondByte = buffer.get();
//            byte maskBit = (byte) (secondByte & 0x80);
//            byte length = (byte) (secondByte & 0x7f);
//
//            long payload_length = 0;
//
//            if (length < 126) {
//                payload_length = length;
//            } else if (length == 126) {
//                payload_length = buffer.getShort();
//            } else if (length == 127) {
//                // Does work up to MAX_VALUE of long (2^63-1) after that minus values are returned.
//                // However frames with such a high payload length are vastly unrealistic.
//                // TODO: add Limit for WebSocket Payload Length.
//                payload_length = buffer.getLong();
//            }
//            buffer.compact();
//            buffer.flip();
//        }
    }

    @Override
    public void createPong(ByteBuffer srcBuffer, ByteBuffer dstBuffer) {

    }

    private static byte[] createRandomMaskingKey()
    {
        final byte[] maskingKey = new byte[4];
        Random random = new SecureRandom();
        random.nextBytes(maskingKey);
        return maskingKey;
    }
}
