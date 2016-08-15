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

package org.apache.qpid.proton.engine;

import java.nio.ByteBuffer;
import java.util.Map;

public interface WebSocketHandler
{
    public enum WebSocketMessageType
    {
        WEB_SOCKET_MESSAGE_TYPE_UNKNOWN,
        WEB_SOCKET_MESSAGE_TYPE_CHUNK,
        WEB_SOCKET_MESSAGE_TYPE_HEADER_CHUNK,
        WEB_SOCKET_MESSAGE_TYPE_AMQP,
        WEB_SOCKET_MESSAGE_TYPE_PING,
        WEB_SOCKET_MESSAGE_TYPE_CLOSE,
    }

    String createUpgradeRequest(String hostName, String webSocketPath, int webSocketPort, String webSocketProtocol, Map<String, String> additionalHeaders);

    Boolean validateUpgradeReply(ByteBuffer buffer);

    void wrapBuffer(ByteBuffer srcBuffer, ByteBuffer dstBuffer);

    WebsocketTuple unwrapBuffer(ByteBuffer srcBuffer);

    void createPong(ByteBuffer srcBuffer, ByteBuffer dstBuffer);

    int calculateHeaderSize(int payloadSize);

    public class WebsocketTuple{

        private long length;
        private WebSocketMessageType type;

        public WebsocketTuple(long length, WebSocketMessageType type){
            this.length = length;
            this.type = type;
        }

        public void setLength(long length){
            this.length = length;
        }

        public void setType(WebSocketMessageType type){
            this.type = type;
        }

        public long getLength(){
            return this.length;
        }
        public WebSocketMessageType getType(){
            return this.type;
        }
    }
}
