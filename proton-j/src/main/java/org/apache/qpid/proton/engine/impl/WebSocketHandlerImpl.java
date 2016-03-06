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
import org.apache.qpid.proton.engine.WebSocketHeader;

import javax.naming.InsufficientResourcesException;
import java.io.*;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.*;

public class WebSocketHandlerImpl implements WebSocketHandler
{
    private WebSocketUpgrade _webSocketUpgrade = null;

    protected WebSocketUpgrade createWebSocketUpgrade(
            String hostName,
            String webSocketPath,
            int webSocketPort,
            String webSocketProtocol,
            Map<String, String> additionalHeaders)
    {
        return new WebSocketUpgrade(hostName, webSocketPath, webSocketPort, webSocketProtocol, additionalHeaders);
    }

    @Override
    public String createUpgradeRequest(
            String hostName,
            String webSocketPath,
            int webSocketPort,
            String webSocketProtocol,
            Map<String, String> additionalHeaders)
    {
        _webSocketUpgrade = createWebSocketUpgrade(hostName, webSocketPath, webSocketPort, webSocketProtocol, additionalHeaders);
        return _webSocketUpgrade.createUpgradeRequest();
    }

    @Override
    public void createPong(ByteBuffer ping, ByteBuffer pong)
    {
        if ((ping == null) && (pong == null))
        {
            throw new IllegalArgumentException("input parameter cannot be null");
        }

        if (ping.capacity() > pong.capacity())
        {
            throw new IllegalArgumentException("insufficient output buffer size");
        }

        if (ping.remaining() > 0)
        {
            pong.clear();
            byte[] buffer = new byte[ping.remaining()];
            buffer = ping.array();
            buffer[0] = (byte) 138;
            pong.put(buffer);
        }
    }

    @Override
    public Boolean validateUpgradeReply(ByteBuffer buffer)
    {
        Boolean retVal = false;
        if (_webSocketUpgrade != null)
        {
            int size = buffer.remaining();
            if (size > 0)
            {
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                buffer.compact();

                retVal = _webSocketUpgrade.validateUpgradeReply(data);
                _webSocketUpgrade = null;
            }
        }
        return retVal;
    }

    @Override
    public void wrapBuffer(ByteBuffer srcBuffer, ByteBuffer dstBuffer)
    {
        if ((srcBuffer != null) && (dstBuffer != null) && srcBuffer.remaining() > 0)
        {
//            // We always send final WebSocket frame
//            // RFC: Indicates that this is the final fragment in a message.
//            final byte FINBIT_SET = (byte) 0x80;
//
//            // We always send binary message (AMQP)
//            // RFC: %x2 denotes a binary frame
//            final byte OPCODE_BINARY = 0x2;

            // We always send masked data
            // RFC: "client MUST mask all frames that it sends to the server"
//            final byte MASKBIT_SET = (byte) 0x80;
            final byte[] MASKING_KEY = createRandomMaskingKey();

            // Minimum header length is 6
//            final byte MIN_HEADER_LENGTH = 6;

            // Get data length
            final int DATA_LENGTH = srcBuffer.remaining();

            // Auto growing buffer for the WS frame, initialized to minimum size
            ByteArrayOutputStream webSocketFrame = new ByteArrayOutputStream(WebSocketHeader.MIN_HEADER_LENGTH + DATA_LENGTH);

            // Create the first byte
            // We always send final WebSocket frame
            // We always send binary message (AMQP)
            byte firstByte = (byte) (WebSocketHeader.FINBIT_MASK | WebSocketHeader.OPCODE_BINARY);
            webSocketFrame.write(firstByte);

            // Create the second byte
            // RFC: "client MUST mask all frames that it sends to the server"
            byte secondByte = WebSocketHeader.MASKBIT_MASK;

            // RFC: The length of the "Payload data", in bytes: if 0-125, that is the payload length.
            if (DATA_LENGTH < 126)
            {
                secondByte = (byte) (secondByte | DATA_LENGTH);
                webSocketFrame.write(secondByte);
            }
            // RFC: If 126, the following 2 bytes interpreted as a 16-bit unsigned integer are the payload length
            else if (DATA_LENGTH <=  65535)
            {
                // Create payload byte
                secondByte = (byte) (secondByte | 126);
                webSocketFrame.write(secondByte);

                // Create extended length bytes
                webSocketFrame.write((byte) (DATA_LENGTH >>> 8));
                webSocketFrame.write((byte) (DATA_LENGTH));
            }
            // RFC: If 127, the following 8 bytes interpreted as a 64-bit unsigned integer (the most significant bit MUST be 0) are the payload length.
            // No need for "else if" because if it is longer than what 8 byte length can hold... or bets are off anyway
            else
            {
                secondByte = (byte) (secondByte | 127);
                webSocketFrame.write(secondByte);

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
            for (int i = 0; i < DATA_LENGTH; i++)
            {
                byte nextByte = srcBuffer.get();
                nextByte ^= MASKING_KEY[i % 4];
                webSocketFrame.write(nextByte);
            }

            // Copy frame to destination buffer
            dstBuffer.clear();
            dstBuffer.put(webSocketFrame.toByteArray());
        }
    }

    @Override
    public WebSocketMessageType unwrapBuffer(ByteBuffer srcBuffer, ByteBuffer dstBuffer)
    {
        WebSocketMessageType retVal = WebSocketMessageType.WEB_SOCKET_MESSAGE_TYPE_EMPTY;

        if (srcBuffer.limit() > 1)
        {
//            final byte OPCODE_SET = (byte) 0x0f;
//            final byte OPCODE_BINARY = 0x2;
//            final byte OPCODE_PING = 0x9;
//            final byte MASKBIT_SET = (byte) 0x80;
//            final byte PAYLOAD_SET = (byte) 0x7f;

            // Read the first byte
            byte firstByte = srcBuffer.get();
            // Get and check the opcode
            byte opcode = (byte) (firstByte & WebSocketHeader.OPCODE_MASK);

            // Read the second byte
            byte secondByte = srcBuffer.get();
            byte maskBit = (byte) (secondByte & WebSocketHeader.MASKBIT_MASK);
            byte payloadLength = (byte) (secondByte & WebSocketHeader.PAYLOAD_MASK);

            long finalPayloadLength = 0;
            if (payloadLength < 126)
            {
                finalPayloadLength = payloadLength;
            }
            else if (payloadLength == 126)
            {
                // Check if we have enough bytes to read
                if (srcBuffer.limit() > 3)
                {
                    finalPayloadLength = srcBuffer.getShort();
                }
                else
                {
                    return WebSocketMessageType.WEB_SOCKET_MESSAGE_TYPE_INVALID_LENGTH;
                }
            }
            else if (payloadLength == 127)
            {
                // Check if we have enough bytes to read
                if (srcBuffer.limit() > 9)
                {
                    finalPayloadLength = srcBuffer.getLong();
                }
                else
                {
                    return WebSocketMessageType.WEB_SOCKET_MESSAGE_TYPE_INVALID_LENGTH;
                }
            }

            // Now we have read all the headers, let's validate the message and prepare the return buffer
            srcBuffer.compact();
            srcBuffer.flip();

            if (opcode == WebSocketHeader.OPCODE_BINARY)
            {
                return WebSocketMessageType.WEB_SOCKET_MESSAGE_TYPE_AMQP;
            }
            else if (opcode == WebSocketHeader.OPCODE_PING)
            {
                return WebSocketMessageType.WEB_SOCKET_MESSAGE_TYPE_PING;
            }
            else
            {
                return WebSocketMessageType.WEB_SOCKET_MESSAGE_TYPE_INVALID;
            }
        }

        return retVal;
    }

    private static byte[] createRandomMaskingKey()
    {
        final byte[] maskingKey = new byte[4];
        Random random = new SecureRandom();
        random.nextBytes(maskingKey);
        return maskingKey;
    }
}
