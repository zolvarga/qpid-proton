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

import org.apache.qpid.proton.engine.WebSocketHeader;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class WebSocketHandlerImplTest
{
//    private WebSocketHandlerImpl webSocketHandler;
//
//    @Before
//    public void setUp()
//    {
//        webSocketHandler = new WebSocketHandlerImpl();
//    }

//    @InjectMocks
//    private WebSocketHandlerImpl webSocketHandler;
//
//    @InjectMocks
//    private WebSocketUpgrade webSocketUpgrade;

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
        WebSocketHandlerImpl webSocketHandlerSpy = spy(webSocketHandler);

//        WebSocketUpgrade mockWebSocketUpgrade = mock(WebSocketUpgrade.class);
//        doReturn(mockWebSocketUpgrade).when(webSocketHandlerSpy).createWebSocketUpgrade(
//                hostName,
//                webSocketPath,
//                webSocketPort,
//                webSocketProtocol,
//                additionalHeaders
//        );
        webSocketHandlerSpy.createUpgradeRequest(
                hostName,
                webSocketPath,
                webSocketPort,
                webSocketProtocol,
                additionalHeaders
        );
        verify(webSocketHandlerSpy, times(1)).createWebSocketUpgrade(
                hostName,
                webSocketPath,
                webSocketPort,
                webSocketProtocol,
                additionalHeaders
        );
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
}
