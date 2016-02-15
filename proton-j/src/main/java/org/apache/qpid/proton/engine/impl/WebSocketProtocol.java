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

public class WebSocketProtocol
{
    public static String createUpgradeRequest()
    {
        String host = "zolvargahub.azure-devices.net"; // 168.61.54.255
        String path = "/$iothub/websocket";

        String key = "mQzPElOHKd+RwPyWnWOJiQ==";
        String endOfLine = "\r\n";
        StringBuilder stringBuilder = new StringBuilder()
                .append("GET /").append(path).append(" HTTP/1.1").append(endOfLine)
                .append("Host: ").append(host).append(endOfLine)
                .append("Connection: Upgrade").append(endOfLine)
                .append("Upgrade: websocket").append(endOfLine)
                .append("Sec-WebSocket-Version: 13").append(endOfLine)
                .append("Sec-WebSocket-Key: ").append(key).append(endOfLine)
                .append("Sec-WebSocket-Protocol: AMQPWSB10").append(endOfLine)
                .append("Sec-WebSocket-Extensions: permessage-deflate").append(endOfLine).append(endOfLine);

        String upgradeRequest = stringBuilder.toString();
        return upgradeRequest;
    }

    public static String createUpgradeRequestEcho()
    {
        String host = "echo.websocket.org"; // 168.61.54.255
        String path = "";

        String key = "xuQy3IC/xr6VBhMS6QWOeQ=a";
        String endOfLine = "\r\n";
        StringBuilder stringBuilder = new StringBuilder()
                .append("GET /").append(path).append(" HTTP/1.1").append(endOfLine)
                .append("Connection: Upgrade").append(endOfLine)
                .append("Host: ").append(host + ":80").append(endOfLine)
                .append("Sec-WebSocket-Key: ").append(key).append(endOfLine)
                .append("Sec-WebSocket-Version: 13").append(endOfLine)
                .append("Upgrade: websocket").append(endOfLine)
                .append(endOfLine);

        String upgradeRequest = stringBuilder.toString();
        return upgradeRequest;
    }

}
