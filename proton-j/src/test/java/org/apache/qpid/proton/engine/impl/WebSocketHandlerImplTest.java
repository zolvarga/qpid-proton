/*
 *
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
 *
*/
package org.apache.qpid.proton.engine.impl;

import org.apache.qpid.proton.engine.WebSocketHandler;
import org.apache.qpid.proton.engine.WebSocketHeader;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class WebSocketHandlerImplTest
{
    @Test
    public void testCreateUpgradeRequest()
    {
        String hostName = "host_XXX";
        String webSocketPath = "path1/path2";
        int webSocketPort = 1234567890;
        String webSocketProtocol = "subprotocol_name";
        Map<String, String> additionalHeaders = new HashMap<String, String>();
        additionalHeaders.put("header1", "content1");
        additionalHeaders.put("header2", "content2");
        additionalHeaders.put("header3", "content3");

        WebSocketHandlerImpl webSocketHandler = new WebSocketHandlerImpl();
        WebSocketHandlerImpl spyWebSocketHandler = spy(webSocketHandler);

        WebSocketUpgrade mockWebSocketUpgrade = mock(WebSocketUpgrade.class);

        doReturn(mockWebSocketUpgrade).when(spyWebSocketHandler).createWebSocketUpgrade(
                hostName,
                webSocketPath,
                webSocketPort,
                webSocketProtocol,
                additionalHeaders
        );
        spyWebSocketHandler.createUpgradeRequest(
                hostName,
                webSocketPath,
                webSocketPort,
                webSocketProtocol,
                additionalHeaders
        );
        verify(spyWebSocketHandler, times(1)).createWebSocketUpgrade(
                hostName,
                webSocketPath,
                webSocketPort,
                webSocketProtocol,
                additionalHeaders
        );
        verify(mockWebSocketUpgrade, times(1)).createUpgradeRequest();
    }

    @Test
    public void testCreatePong()
    {
        WebSocketHandlerImpl webSocketHandler = new WebSocketHandlerImpl();

        ByteBuffer ping = ByteBuffer.allocate(10);
        ByteBuffer pong = ByteBuffer.allocate(10);

        byte[] buffer = new byte[10];
        buffer[0] = WebSocketHeader.FINBIT_MASK | WebSocketHeader.OPCODE_PING;
        ping.put(buffer);
        ping.flip();
        webSocketHandler.createPong(ping, pong);

        int actual = pong.array()[0];
        int expected = WebSocketHeader.FINBIT_MASK | WebSocketHeader.OPCODE_PONG;

        assertEquals(actual, expected);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreatePong_ping_null()
    {
        WebSocketHandlerImpl webSocketHandler = new WebSocketHandlerImpl();

        ByteBuffer pong = ByteBuffer.allocate(10);

        webSocketHandler.createPong(null, pong);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreatePong_pong_null()
    {
        WebSocketHandlerImpl webSocketHandler = new WebSocketHandlerImpl();

        ByteBuffer ping = ByteBuffer.allocate(10);

        webSocketHandler.createPong(ping, null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testCreatePong_pong_capacity_insufficient()
    {
        WebSocketHandlerImpl webSocketHandler = new WebSocketHandlerImpl();

        ByteBuffer ping = ByteBuffer.allocate(10);
        ByteBuffer pong = ByteBuffer.allocate(9);

        webSocketHandler.createPong(ping, pong);
    }

    @Test
    public void testCreatePong_ping_no_remaining()
    {
        WebSocketHandlerImpl webSocketHandler = new WebSocketHandlerImpl();

        ByteBuffer ping = ByteBuffer.allocate(10);
        ByteBuffer pong = ByteBuffer.allocate(10);

        ping.flip();

        webSocketHandler.createPong(ping, pong);

        assertEquals(0, pong.limit());
        assertEquals(0, pong.position());
    }

    @Test
    public void testValidateUpgradeReply()
    {
        String hostName = "host_XXX";
        String webSocketPath = "path1/path2";
        int webSocketPort = 1234567890;
        String webSocketProtocol = "subprotocol_name";
        Map<String, String> additionalHeaders = new HashMap<String, String>();
        additionalHeaders.put("header1", "content1");
        additionalHeaders.put("header2", "content2");
        additionalHeaders.put("header3", "content3");

        ByteBuffer buffer = ByteBuffer.allocate(10);
        byte[] data = new byte[buffer.remaining()];

        WebSocketHandlerImpl webSocketHandler = new WebSocketHandlerImpl();
        WebSocketHandlerImpl spyWebSocketHandler = spy(webSocketHandler);

        WebSocketUpgrade mockWebSocketUpgrade = mock(WebSocketUpgrade.class);

        doReturn(mockWebSocketUpgrade).when(spyWebSocketHandler).createWebSocketUpgrade(
                hostName,
                webSocketPath,
                webSocketPort,
                webSocketProtocol,
                additionalHeaders
        );
        doReturn(true).when(mockWebSocketUpgrade).validateUpgradeReply(data);

        spyWebSocketHandler.createUpgradeRequest(
                hostName,
                webSocketPath,
                webSocketPort,
                webSocketProtocol,
                additionalHeaders
        );
        assertTrue(spyWebSocketHandler.validateUpgradeReply(buffer));
        assertFalse(mockWebSocketUpgrade == null);
        verify(mockWebSocketUpgrade, times(1)).validateUpgradeReply(data);
    }

    @Test
    public void testValidateUpgradeReply_no_remaining()
    {
        String hostName = "host_XXX";
        String webSocketPath = "path1/path2";
        int webSocketPort = 1234567890;
        String webSocketProtocol = "subprotocol_name";
        Map<String, String> additionalHeaders = new HashMap<String, String>();
        additionalHeaders.put("header1", "content1");
        additionalHeaders.put("header2", "content2");
        additionalHeaders.put("header3", "content3");

        ByteBuffer buffer = ByteBuffer.allocate(10);
        byte[] data = new byte[buffer.remaining()];
        buffer.limit(0);

        WebSocketHandlerImpl webSocketHandler = new WebSocketHandlerImpl();
        WebSocketHandlerImpl spyWebSocketHandler = spy(webSocketHandler);

        WebSocketUpgrade mockWebSocketUpgrade = mock(WebSocketUpgrade.class);

        doReturn(mockWebSocketUpgrade).when(spyWebSocketHandler).createWebSocketUpgrade(
                hostName,
                webSocketPath,
                webSocketPort,
                webSocketProtocol,
                additionalHeaders
        );
        doReturn(true).when(mockWebSocketUpgrade).validateUpgradeReply(data);

        spyWebSocketHandler.createUpgradeRequest(
                hostName,
                webSocketPath,
                webSocketPort,
                webSocketProtocol,
                additionalHeaders
        );
        assertFalse(spyWebSocketHandler.validateUpgradeReply(buffer));
        verify(mockWebSocketUpgrade, times(0)).validateUpgradeReply(data);
    }

    @Test
    public void testValidateUpgradeReply_websocketupgrade_null()
    {
        ByteBuffer buffer = ByteBuffer.allocate(10);
        byte[] data = new byte[buffer.remaining()];

        WebSocketHandlerImpl webSocketHandler = new WebSocketHandlerImpl();
        WebSocketHandlerImpl spyWebSocketHandler = spy(webSocketHandler);

        WebSocketUpgrade mockWebSocketUpgrade = mock(WebSocketUpgrade.class);

        assertFalse(spyWebSocketHandler.validateUpgradeReply(buffer));
        verify(mockWebSocketUpgrade, times(0)).validateUpgradeReply(data);
    }

    @Test
    public void testWrapBuffer_short_payload()
    {
        WebSocketHandlerImpl webSocketHandler = new WebSocketHandlerImpl();
        WebSocketHandlerImpl spyWebSocketHandler = spy(webSocketHandler);

        int messageLength = 10;

        byte[] maskingKey = new byte[] { 0x01, 0x02, 0x03, 0x04};

        byte[] data = new byte[messageLength];
        Random random = new SecureRandom();
        random.nextBytes(data);

        ByteBuffer srcBuffer = ByteBuffer.allocate(messageLength + WebSocketHeader.MIN_HEADER_LENGTH_MASKED);
        ByteBuffer dstBuffer = ByteBuffer.allocate(messageLength + WebSocketHeader.MIN_HEADER_LENGTH_MASKED);
        srcBuffer.put(data);
        srcBuffer.flip();

        int expectedHeaderSize = WebSocketHeader.MIN_HEADER_LENGTH_MASKED;
        byte[] expected = new byte[dstBuffer.capacity()];
        expected[0] = (byte) (WebSocketHeader.FINBIT_MASK | WebSocketHeader.OPCODE_BINARY);
        expected[1] = (byte) (WebSocketHeader.MASKBIT_MASK | srcBuffer.limit());
        expected[2] = maskingKey[0];
        expected[3] = maskingKey[1];
        expected[4] = maskingKey[2];
        expected[5] = maskingKey[3];

        for (int i = 0; i < srcBuffer.limit(); i++)
        {
            byte nextByte = srcBuffer.get();
            nextByte ^= maskingKey[i % 4];
            expected[i + 6] = nextByte;
        }
        srcBuffer.flip();

        doReturn(maskingKey).when(spyWebSocketHandler).createRandomMaskingKey();

        spyWebSocketHandler.wrapBuffer(srcBuffer, dstBuffer);
        dstBuffer.flip();

        byte[] actual = dstBuffer.array();

        assertEquals("invalid content length", srcBuffer.limit() + expectedHeaderSize, dstBuffer.limit());

        assertEquals("first byte mismatch", expected[0], actual[0]);
        assertEquals("second byte mismatch", expected[1], actual[1]);

        assertEquals("masking key mismatch 1", maskingKey[0], actual[2]);
        assertEquals("masking key mismatch 2", maskingKey[1], actual[3]);
        assertEquals("masking key mismatch 3", maskingKey[2], actual[4]);
        assertEquals("masking key mismatch 4", maskingKey[3], actual[5]);

        assertTrue(Arrays.equals(expected, actual));
    }

    @Test
    public void testWrapBuffer_short_payload_max()
    {
        WebSocketHandlerImpl webSocketHandler = new WebSocketHandlerImpl();
        WebSocketHandlerImpl spyWebSocketHandler = spy(webSocketHandler);

        int messageLength = 125;

        byte[] maskingKey = new byte[] { 0x01, 0x02, 0x03, 0x04};

        byte[] data = new byte[messageLength];
        Random random = new SecureRandom();
        random.nextBytes(data);

        ByteBuffer srcBuffer = ByteBuffer.allocate(messageLength + WebSocketHeader.MIN_HEADER_LENGTH_MASKED);
        ByteBuffer dstBuffer = ByteBuffer.allocate(messageLength + WebSocketHeader.MIN_HEADER_LENGTH_MASKED);
        srcBuffer.put(data);
        srcBuffer.flip();

        int expectedHeaderSize = WebSocketHeader.MIN_HEADER_LENGTH_MASKED;
        byte[] expected = new byte[dstBuffer.capacity()];
        expected[0] = (byte) (WebSocketHeader.FINBIT_MASK | WebSocketHeader.OPCODE_BINARY);
        expected[1] = (byte) (WebSocketHeader.MASKBIT_MASK | srcBuffer.limit());
        expected[2] = maskingKey[0];
        expected[3] = maskingKey[1];
        expected[4] = maskingKey[2];
        expected[5] = maskingKey[3];

        for (int i = 0; i < srcBuffer.limit(); i++)
        {
            byte nextByte = srcBuffer.get();
            nextByte ^= maskingKey[i % 4];
            expected[i + 6] = nextByte;
        }
        srcBuffer.flip();

        doReturn(maskingKey).when(spyWebSocketHandler).createRandomMaskingKey();

        spyWebSocketHandler.wrapBuffer(srcBuffer, dstBuffer);
        dstBuffer.flip();

        byte[] actual = dstBuffer.array();

        assertEquals("invalid content length", srcBuffer.limit() + expectedHeaderSize, dstBuffer.limit());

        assertEquals("first byte mismatch", expected[0], actual[0]);
        assertEquals("second byte mismatch", expected[1], actual[1]);

        assertEquals("masking key mismatch 1", maskingKey[0], actual[2]);
        assertEquals("masking key mismatch 2", maskingKey[1], actual[3]);
        assertEquals("masking key mismatch 3", maskingKey[2], actual[4]);
        assertEquals("masking key mismatch 4", maskingKey[3], actual[5]);

        assertTrue(Arrays.equals(expected, actual));
    }

    @Test
    public void testWrapBuffer_medium_payload()
    {
        WebSocketHandlerImpl webSocketHandler = new WebSocketHandlerImpl();
        WebSocketHandlerImpl spyWebSocketHandler = spy(webSocketHandler);

        int messageLength = 126;

        byte[] maskingKey = new byte[] { 0x01, 0x02, 0x03, 0x04};

        byte[] data = new byte[messageLength];
        Random random = new SecureRandom();
        random.nextBytes(data);

        ByteBuffer srcBuffer = ByteBuffer.allocate(messageLength + WebSocketHeader.MED_HEADER_LENGTH);
        ByteBuffer dstBuffer = ByteBuffer.allocate(messageLength + WebSocketHeader.MED_HEADER_LENGTH);
        srcBuffer.put(data);
        srcBuffer.flip();

        int expectedHeaderSize = WebSocketHeader.MED_HEADER_LENGTH;
        byte[] expected = new byte[dstBuffer.capacity()];
        expected[0] = (byte) (WebSocketHeader.FINBIT_MASK | WebSocketHeader.OPCODE_BINARY);
        expected[1] = (byte) (WebSocketHeader.MASKBIT_MASK | 126);

        expected[2] = (byte) (messageLength >> 8);
        expected[3] = (byte) (messageLength);

        expected[4] = maskingKey[0];
        expected[5] = maskingKey[1];
        expected[6] = maskingKey[2];
        expected[7] = maskingKey[3];

        for (int i = 0; i < srcBuffer.limit(); i++)
        {
            byte nextByte = srcBuffer.get();
            nextByte ^= maskingKey[i % 4];
            expected[i + 8] = nextByte;
        }
        srcBuffer.flip();

        doReturn(maskingKey).when(spyWebSocketHandler).createRandomMaskingKey();

        spyWebSocketHandler.wrapBuffer(srcBuffer, dstBuffer);
        dstBuffer.flip();

        byte[] actual = dstBuffer.array();

        assertEquals("invalid content length", srcBuffer.limit() + expectedHeaderSize, dstBuffer.limit());

        assertEquals("first byte mismatch", expected[0], actual[0]);
        assertEquals("second byte mismatch", expected[1], actual[1]);

        assertEquals("payload length byte mismatch 1", expected[2], actual[2]);
        assertEquals("payload length byte mismatch 2", expected[3], actual[3]);

        assertEquals("masking key mismatch 1", maskingKey[0], actual[4]);
        assertEquals("masking key mismatch 2", maskingKey[1], actual[5]);
        assertEquals("masking key mismatch 3", maskingKey[2], actual[6]);
        assertEquals("masking key mismatch 4", maskingKey[3], actual[7]);

        assertTrue(Arrays.equals(expected, actual));
    }

    @Test
    public void testWrapBuffer_medium_payload_max()
    {
        WebSocketHandlerImpl webSocketHandler = new WebSocketHandlerImpl();
        WebSocketHandlerImpl spyWebSocketHandler = spy(webSocketHandler);

        int messageLength = 65535;

        byte[] maskingKey = new byte[] { 0x01, 0x02, 0x03, 0x04};

        byte[] data = new byte[messageLength];
        Random random = new SecureRandom();
        random.nextBytes(data);

        ByteBuffer srcBuffer = ByteBuffer.allocate(messageLength + WebSocketHeader.MED_HEADER_LENGTH);
        ByteBuffer dstBuffer = ByteBuffer.allocate(messageLength + WebSocketHeader.MED_HEADER_LENGTH);
        srcBuffer.put(data);
        srcBuffer.flip();

        int expectedHeaderSize = WebSocketHeader.MED_HEADER_LENGTH;
        byte[] expected = new byte[dstBuffer.capacity()];
        expected[0] = (byte) (WebSocketHeader.FINBIT_MASK | WebSocketHeader.OPCODE_BINARY);
        expected[1] = (byte) (WebSocketHeader.MASKBIT_MASK | 126);

        expected[2] = (byte) (messageLength >>> 8);
        expected[3] = (byte) (messageLength);

        expected[4] = maskingKey[0];
        expected[5] = maskingKey[1];
        expected[6] = maskingKey[2];
        expected[7] = maskingKey[3];

        for (int i = 0; i < srcBuffer.limit(); i++)
        {
            byte nextByte = srcBuffer.get();
            nextByte ^= maskingKey[i % 4];
            expected[i + 8] = nextByte;
        }
        srcBuffer.flip();

        doReturn(maskingKey).when(spyWebSocketHandler).createRandomMaskingKey();

        spyWebSocketHandler.wrapBuffer(srcBuffer, dstBuffer);
        dstBuffer.flip();

        byte[] actual = dstBuffer.array();

        assertEquals("invalid content length", srcBuffer.limit() + expectedHeaderSize, dstBuffer.limit());

        assertEquals("first byte mismatch", expected[0], actual[0]);
        assertEquals("second byte mismatch", expected[1], actual[1]);

        assertEquals("payload length byte mismatch 1", expected[2], actual[2]);
        assertEquals("payload length byte mismatch 2", expected[3], actual[3]);

        assertEquals("masking key mismatch 1", maskingKey[0], actual[4]);
        assertEquals("masking key mismatch 2", maskingKey[1], actual[5]);
        assertEquals("masking key mismatch 3", maskingKey[2], actual[6]);
        assertEquals("masking key mismatch 4", maskingKey[3], actual[7]);

        assertTrue(Arrays.equals(expected, actual));
    }

    @Test
    public void testWrapBuffer_long_payload()
    {
        WebSocketHandlerImpl webSocketHandler = new WebSocketHandlerImpl();
        WebSocketHandlerImpl spyWebSocketHandler = spy(webSocketHandler);

        int messageLength = 65536;

        byte[] maskingKey = new byte[] { 0x01, 0x02, 0x03, 0x04};

        byte[] data = new byte[messageLength];
        Random random = new SecureRandom();
        random.nextBytes(data);

        ByteBuffer srcBuffer = ByteBuffer.allocate(messageLength + WebSocketHeader.MAX_HEADER_LENGTH);
        ByteBuffer dstBuffer = ByteBuffer.allocate(messageLength + WebSocketHeader.MAX_HEADER_LENGTH);
        srcBuffer.put(data);
        srcBuffer.flip();

        int expectedHeaderSize = WebSocketHeader.MAX_HEADER_LENGTH;
        byte[] expected = new byte[dstBuffer.capacity()];
        expected[0] = (byte) (WebSocketHeader.FINBIT_MASK | WebSocketHeader.OPCODE_BINARY);
        expected[1] = (byte) (WebSocketHeader.MASKBIT_MASK | 127);

        expected[2] = (byte) (messageLength >>> 56);
        expected[3] = (byte) (messageLength >>> 48);
        expected[4] = (byte) (messageLength >>> 40);
        expected[5] = (byte) (messageLength >>> 32);
        expected[6] = (byte) (messageLength >>> 24);
        expected[7] = (byte) (messageLength >>> 16);
        expected[8] = (byte) (messageLength >>> 8);
        expected[9] = (byte) (messageLength);

        expected[10] = maskingKey[0];
        expected[11] = maskingKey[1];
        expected[12] = maskingKey[2];
        expected[13] = maskingKey[3];

        for (int i = 0; i < srcBuffer.limit(); i++)
        {
            byte nextByte = srcBuffer.get();
            nextByte ^= maskingKey[i % 4];
            expected[i + 14] = nextByte;
        }
        srcBuffer.flip();

        doReturn(maskingKey).when(spyWebSocketHandler).createRandomMaskingKey();

        spyWebSocketHandler.wrapBuffer(srcBuffer, dstBuffer);
        dstBuffer.flip();

        byte[] actual = dstBuffer.array();

        assertEquals("invalid content length", srcBuffer.limit() + expectedHeaderSize, dstBuffer.limit());

        assertEquals("first byte mismatch", expected[0], actual[0]);
        assertEquals("second byte mismatch", expected[1], actual[1]);

        assertEquals("payload length byte mismatch 1", expected[2], actual[2]);
        assertEquals("payload length byte mismatch 2", expected[3], actual[3]);
        assertEquals("payload length byte mismatch 3", expected[4], actual[4]);
        assertEquals("payload length byte mismatch 4", expected[5], actual[5]);
        assertEquals("payload length byte mismatch 5", expected[6], actual[6]);
        assertEquals("payload length byte mismatch 6", expected[7], actual[7]);
        assertEquals("payload length byte mismatch 7", expected[8], actual[8]);
        assertEquals("payload length byte mismatch 8", expected[9], actual[9]);

        assertEquals("masking key mismatch 1", maskingKey[0], actual[10]);
        assertEquals("masking key mismatch 2", maskingKey[1], actual[11]);
        assertEquals("masking key mismatch 3", maskingKey[2], actual[12]);
        assertEquals("masking key mismatch 4", maskingKey[3], actual[13]);

        assertTrue(Arrays.equals(expected, actual));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWrapBuffer_src_buffer_null()
    {
        WebSocketHandlerImpl webSocketHandler = new WebSocketHandlerImpl();
        WebSocketHandlerImpl spyWebSocketHandler = spy(webSocketHandler);

        int messageLength = 10;
        ByteBuffer srcBuffer = null;
        ByteBuffer dstBuffer = ByteBuffer.allocate(messageLength + WebSocketHeader.MAX_HEADER_LENGTH);

        spyWebSocketHandler.wrapBuffer(srcBuffer, dstBuffer);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWrapBuffer_dst_buffer_null()
    {
        WebSocketHandlerImpl webSocketHandler = new WebSocketHandlerImpl();
        WebSocketHandlerImpl spyWebSocketHandler = spy(webSocketHandler);

        int messageLength = 10;
        ByteBuffer srcBuffer = ByteBuffer.allocate(messageLength + WebSocketHeader.MAX_HEADER_LENGTH);
        ByteBuffer dstBuffer = null;

        spyWebSocketHandler.wrapBuffer(srcBuffer, dstBuffer);
    }

    @Test (expected = OutOfMemoryError.class)
    public void testWrapBuffer_dst_buffer_small()
    {
        WebSocketHandlerImpl webSocketHandler = new WebSocketHandlerImpl();
        WebSocketHandlerImpl spyWebSocketHandler = spy(webSocketHandler);

        int messageLength = 10;
        ByteBuffer srcBuffer = ByteBuffer.allocate(messageLength);
        ByteBuffer dstBuffer = ByteBuffer.allocate(messageLength);;

        spyWebSocketHandler.wrapBuffer(srcBuffer, dstBuffer);
    }

    @Test
    public void testUnwrapBuffer_short_message()
    {
        WebSocketHandlerImpl webSocketHandler = new WebSocketHandlerImpl();
        WebSocketHandlerImpl spyWebSocketHandler = spy(webSocketHandler);

        int payloadLength = 10;
        int messageLength = payloadLength + WebSocketHeader.MIN_HEADER_LENGTH;

        byte[] data = new byte[messageLength];
        Random random = new SecureRandom();
        random.nextBytes(data);

        data[0] = (byte) (WebSocketHeader.FINBIT_MASK | WebSocketHeader.OPCODE_BINARY);
        data[1] = (byte) (messageLength);

        ByteBuffer srcBuffer = ByteBuffer.allocate(messageLength);
        srcBuffer.put(data);
        srcBuffer.flip();

        assertEquals(spyWebSocketHandler.unwrapBuffer(srcBuffer), WebSocketHandler.WebSocketMessageType.WEB_SOCKET_MESSAGE_TYPE_AMQP);

        byte[] expected = Arrays.copyOfRange(data, 2, 12);
        byte[] actual = new byte[srcBuffer.limit()];
        srcBuffer.get(actual);
        assertTrue(Arrays.equals(expected, actual));
    }

    @Test
    public void testUnwrapBuffer_invalid_opcode()
    {
        WebSocketHandlerImpl webSocketHandler = new WebSocketHandlerImpl();
        WebSocketHandlerImpl spyWebSocketHandler = spy(webSocketHandler);

        int messageLength = 10;
        ByteBuffer srcBuffer = ByteBuffer.allocate(messageLength);

        assertEquals(spyWebSocketHandler.unwrapBuffer(srcBuffer), WebSocketHandler.WebSocketMessageType.WEB_SOCKET_MESSAGE_TYPE_INVALID);
    }

    @Test
    public void testUnwrapBuffer_src_buffer_empty()
    {
        WebSocketHandlerImpl webSocketHandler = new WebSocketHandlerImpl();
        WebSocketHandlerImpl spyWebSocketHandler = spy(webSocketHandler);

        int messageLength = 10;
        ByteBuffer srcBuffer = ByteBuffer.allocate(messageLength);
        srcBuffer.flip();

        assertEquals(spyWebSocketHandler.unwrapBuffer(srcBuffer), WebSocketHandler.WebSocketMessageType.WEB_SOCKET_MESSAGE_TYPE_EMPTY);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testUnwrapBuffer_src_buffer_null()
    {
        WebSocketHandlerImpl webSocketHandler = new WebSocketHandlerImpl();
        WebSocketHandlerImpl spyWebSocketHandler = spy(webSocketHandler);

        int messageLength = 10;
        ByteBuffer srcBuffer = null;

        spyWebSocketHandler.unwrapBuffer(srcBuffer);
    }
}
