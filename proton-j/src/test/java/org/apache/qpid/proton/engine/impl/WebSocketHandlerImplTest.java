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

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;

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
}
