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
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Random;

public class WebSocketHandlerImpl implements WebSocketHandler
{
//    @Override
//    public String createUpgradeRequest(String host, String path, String port, String protocol, Map<String, String> additionalHeaders)
//    {
//        return createUpgradeRequestEcho();
//        String host = "echo.websocket.org:80"; // 168.61.54.255
//        String path = "";

//        String host = "zolvargahub.azure-devices.net"; // 168.61.54.255
//        String path = "/$iothub/websocket";
//        String host = "iot-sdks-test.azure-devices.net"; // 168.61.54.255
//        String path = "/$iothub/websocket";
//
//        String key = "mQzPElOHKd+RwPyWnWOJiQ==";
//        String endOfLine = "\r\n";
//        StringBuilder stringBuilder = new StringBuilder()
//                .append("GET /").append(path).append(" HTTP/1.1").append(endOfLine)
//                .append("Connection: Upgrade").append(endOfLine)
//                .append("Upgrade: websocket").append(endOfLine)
//                .append("Sec-WebSocket-Key: ").append(key).append(endOfLine)
//                .append("Sec-WebSocket-Version: 13").append(endOfLine)
//                .append("Sec-WebSocket-Protocol: AMQPWSB10").append(endOfLine)
//                .append("Host: ").append(host)
//                .append(endOfLine)
//                .append(endOfLine)
//                ;
//
//        String upgradeRequest = stringBuilder.toString();
//        return upgradeRequest;
//
//    }

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
        if (srcBuffer.limit() == 0) {
            return;
        }
        else {
            byte[] data = new byte[srcBuffer.remaining()];
            srcBuffer.get(data);
            Boolean masking = true;
            byte OPCODE_TEXT = 0x1;
            byte OPCODE_BINARY = 0x2;

            int headerLength = 2; // This is just an assumed headerLength, as we use a ByteArrayOutputStream
            if (masking) {
                headerLength += 4;
            }
            ByteArrayOutputStream frame = new ByteArrayOutputStream(data.length + headerLength);

            byte fin = (byte) 0x80;
            byte startByte = (byte) (fin | OPCODE_BINARY);
            frame.write(startByte);
            int length = data.length;
            int length_field = 0;

            if (length < 126) {
                if (masking) {
                    length = 0x80 | length;
                }
                frame.write((byte) length);
            } else if (length <= 65535) {
                length_field = 126;
                if (masking) {
                    length_field = 0x80 | length_field;
                }
                frame.write((byte) length_field);
                byte[] lengthBytes = intToByteArray(length);
                frame.write(lengthBytes[2]);
                frame.write(lengthBytes[3]);
            } else {
                length_field = 127;
                if (masking) {
                    length_field = 0x80 | length_field;
                }
                frame.write((byte) length_field);
                // Since an integer occupies just 4 bytes we fill the 4 leading length bytes with zero
                try {
                    frame.write(new byte[]{0x0, 0x0, 0x0, 0x0});
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    frame.write(intToByteArray(length));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            byte[] mask = null;
            if (masking) {
                mask = generateMask();
                try {
                    frame.write(mask);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                for (int i = 0; i < data.length; i++) {
                    data[i] ^= mask[i % 4];
                }
            }

            try {
                frame.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
            byte[] b = frame.toByteArray();

            dstBuffer.put(b);
        }
    }

    @Override
    public void unwrapBuffer(ByteBuffer buffer) {
        if (buffer.limit() == 0) {
            return;
        }
        else {
            byte firstByte = buffer.get();
            byte finBit = (byte) (firstByte & 0x80);
            byte opcode = (byte) (firstByte & 0x0f);

            byte secondByte = buffer.get();
            byte maskBit = (byte) (secondByte & 0x80);
            byte length = (byte) (secondByte & 0x7f);

            long payload_length = 0;

            if (length < 126) {
                payload_length = length;
            } else if (length == 126) {
                payload_length = buffer.getShort();
            } else if (length == 127) {
                // Does work up to MAX_VALUE of long (2^63-1) after that minus values are returned.
                // However frames with such a high payload length are vastly unrealistic.
                // TODO: add Limit for WebSocket Payload Length.
                payload_length = buffer.getLong();
            }
            buffer.compact();
            buffer.flip();
        }
    }

    @Override
    public void createPong(ByteBuffer srcBuffer, ByteBuffer dstBuffer) {

    }

    private static byte[] generateMask()
    {
        final byte[] mask = new byte[4];
        Random random = new SecureRandom();
        random.nextBytes(mask);
        return mask;
    }

    private static byte[] intToByteArray(int number)
    {
        byte[] bytes = ByteBuffer.allocate(4).putInt(number).array();
        return bytes;
    }

}
