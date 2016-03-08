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
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WebSocketImplTest
{
    @Test
    public void testConstructor()
    {
        WebSocketImpl webSocketImpl = new WebSocketImpl(WebSocketHeader.PAYLOAD_MEDIUM_MAX);

        ByteBuffer inputBuffer = webSocketImpl.getInputBuffer();
        ByteBuffer outputBuffer = webSocketImpl.getOutputBuffer();
        ByteBuffer pingBuffer = webSocketImpl.getPingBuffer();

        assertNotNull(inputBuffer);
        assertNotNull(outputBuffer);
        assertNotNull(pingBuffer);

        assertEquals(inputBuffer.capacity(), WebSocketHeader.PAYLOAD_MEDIUM_MAX);
        assertEquals(outputBuffer.capacity(), WebSocketHeader.PAYLOAD_MEDIUM_MAX);
        assertEquals(pingBuffer.capacity(), WebSocketHeader.PAYLOAD_MEDIUM_MAX);

        assertFalse(webSocketImpl.getEnabled());
    }

    @Test
    public void testConfigure_handler_null()
    {
        String hostName = "host_XXX";
        String webSocketPath = "path1/path2";
        int webSocketPort = 1234567890;
        String webSocketProtocol = "subprotocol_name";
        Map<String, String> additionalHeaders = new HashMap<String, String>();
        additionalHeaders.put("header1", "content1");
        additionalHeaders.put("header2", "content2");
        additionalHeaders.put("header3", "content3");

        WebSocketImpl webSocketImpl = new WebSocketImpl(WebSocketHeader.PAYLOAD_MEDIUM_MAX);

        webSocketImpl.configure(
                hostName,
                webSocketPath,
                webSocketPort,
                webSocketProtocol,
                additionalHeaders,
                null
        );

        assertNotNull(webSocketImpl.getWebSocketHandler());
        assertTrue(webSocketImpl.getEnabled());

    }

    @Test
    public void testConfigure_handler_not_null()
    {
        String hostName = "host_XXX";
        String webSocketPath = "path1/path2";
        int webSocketPort = 1234567890;
        String webSocketProtocol = "subprotocol_name";
        Map<String, String> additionalHeaders = new HashMap<String, String>();
        additionalHeaders.put("header1", "content1");
        additionalHeaders.put("header2", "content2");
        additionalHeaders.put("header3", "content3");

        WebSocketImpl webSocketImpl = new WebSocketImpl(WebSocketHeader.PAYLOAD_MEDIUM_MAX);
        WebSocketHandler webSocketHandler = new WebSocketHandlerImpl();

        webSocketImpl.configure(
                hostName,
                webSocketPath,
                webSocketPort,
                webSocketProtocol,
                additionalHeaders,
                webSocketHandler
        );

        assertEquals(webSocketHandler, webSocketImpl.getWebSocketHandler());
        assertTrue(webSocketImpl.getEnabled());

    }

    @Test
    public void testWriteUpgradeRequest()
    {
        String hostName = "host_XXX";
        String webSocketPath = "path1/path2";
        int webSocketPort = 1234567890;
        String webSocketProtocol = "subprotocol_name";
        Map<String, String> additionalHeaders = new HashMap<String, String>();
        additionalHeaders.put("header1", "content1");
        additionalHeaders.put("header2", "content2");
        additionalHeaders.put("header3", "content3");

        WebSocketImpl webSocketImpl = new WebSocketImpl(WebSocketHeader.PAYLOAD_MEDIUM_MAX);
        WebSocketHandlerImpl webSocketHandler = new WebSocketHandlerImpl();
        WebSocketHandlerImpl spyWebSocketHandler = spy(webSocketHandler);

//        webSocketImpl = new WebSocketImpl(WebSocketHeader.PAYLOAD_MEDIUM_MAX);
//        TransportInput transportInput = new TransportIn

        webSocketImpl.configure(
                hostName,
                webSocketPath,
                webSocketPort,
                webSocketProtocol,
                additionalHeaders,
                webSocketHandler
        );



        verify(spyWebSocketHandler, times(1)).createWebSocketUpgrade(
                hostName,
                webSocketPath,
                webSocketPort,
                webSocketProtocol,
                additionalHeaders
        );


    }
}
