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
import org.apache.qpid.proton.engine.Transport;
import org.apache.qpid.proton.engine.TransportException;
import org.apache.qpid.proton.engine.WebSocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.qpid.proton.engine.impl.ByteBufferUtils.*;

public class WebSocketImpl implements WebSocket
{
    private static final Logger _logger = Logger.getLogger(WebSocketImpl.class.getName());

    private WebSocketHandler _webSocketHandler;

    private final TransportImpl _transport;

    private boolean _tail_closed = false;
    private final ByteBuffer _inputBuffer;
    private boolean _head_closed = false;
    private final ByteBuffer _outputBuffer;

    private Boolean _webSocketEnabled = false;
    private WebSocketState _state = WebSocketState.PN_WS_NOT_STARTED;

    /**
     * @param maxFrameSize the size of the input and output buffers
     *                     returned by {@link WebSocketTransportWrapper#getInputBuffer()} and
     *                     {@link WebSocketTransportWrapper#getOutputBuffer()}.
     */
    WebSocketImpl(TransportImpl transport, int maxFrameSize, WebSocketHandler externalWebSocketHandler, Boolean isEnabled) throws IOException
    {
        _transport = transport;
        _inputBuffer = newWriteableBuffer(maxFrameSize);
        _outputBuffer = newWriteableBuffer(maxFrameSize);
        if (externalWebSocketHandler != null) {
            _webSocketHandler = externalWebSocketHandler;
        }
        else
        {
            _webSocketHandler = new WebSocketHandlerImpl();
        }
        _webSocketEnabled = isEnabled;

        WebSocketHandlerImpl.clearLogFile();
    }

    public void setEnabled(Boolean isEnabled)
    {
        _webSocketEnabled = isEnabled;
    }

    private void writeUpgradeRequest()
    {
        _outputBuffer.clear();
        String request = _webSocketHandler.createUpgradeRequest();
        System.out.println("WEBSOCKETIMPL is sending: ");
        System.out.println(request);
        System.out.println("***************************************************");
        _outputBuffer.put(request.getBytes());

        if (_logger.isLoggable(Level.FINER))
        {
            _logger.log(Level.FINER, "Finished writing WebSocket UpgradeRequest. Output Buffer : " + _outputBuffer);
        }
    }

    public TransportWrapper wrap(final TransportInput input, final TransportOutput output)
    {
        return new WebSocketSniffer(new WebSocketTransportWrapper(input, output), new PlainTransportWrapper(output, input))
        {
            protected boolean isDeterminationMade()
            {
                _selectedTransportWrapper = _wrapper1;
                return true;
            }
        };
    }

    @Override
    public void wrapBuffer(ByteBuffer srcBuffer, ByteBuffer dstBuffer) {
        _webSocketHandler.wrapBuffer(srcBuffer, dstBuffer);
    }

    @Override
    public void unwrapBuffer(ByteBuffer buffer) {
        _webSocketHandler.unwrapBuffer(buffer);
    }

    @Override
    public WebSocketState getState()
    {
        // TODO: Implement function
        return _state;
    }

    @Override
    public int pending()
    {
        // TODO: Implement function
        return 0;
    }

    @Override
    final public int recv(byte[] bytes, int offset, int size)
    {
        // TODO: Implement function
        return size;
    }

    @Override
    final public int send(byte[] bytes, int offset, int size)
    {
        // TODO: Implement function
        return size;
    }

    @Override
    public String toString()
    {
        // TODO: Implement function
        return "";
    }

    private class WebSocketTransportWrapper implements TransportWrapper
    {
        private final TransportInput _underlyingInput;
        private final TransportOutput _underlyingOutput;
        private final ByteBuffer _head;

        private WebSocketTransportWrapper(TransportInput input, TransportOutput output)
        {
            _underlyingInput = input;
            _underlyingOutput = output;
            _head = _outputBuffer.asReadOnlyBuffer();
            _head.limit(0);
        }

        private void processInput() throws TransportException
        {
            switch (_state) {
                case PN_WS_NOT_STARTED:
                    break;
                case PN_WS_CONNECTING:
                    if (_webSocketHandler.validateUpgradeReply(_inputBuffer))
                    {
                        _state = WebSocketState.PN_WS_CONNECTED;
                    }

                    if (_logger.isLoggable(Level.FINER))
                    {
                        _logger.log(Level.FINER, WebSocketImpl.this + " about to call plain input");
                    }
                    break;
                case PN_WS_CONNECTED:
                    Boolean isRepeatedUpgradeAccept = false;
                    int size = _inputBuffer.remaining();
                    if (size > 0)
                    {
                        byte[] data = new byte[_inputBuffer.remaining()];
                        _inputBuffer.get(data);
                        if ((data[0] == 72) && (data[1] == 84))
                        {
                            _inputBuffer.compact();
                            isRepeatedUpgradeAccept = true;
                        }
                    }

                    if (!isRepeatedUpgradeAccept)
                    {
                        _inputBuffer.flip();

                        WebSocketHandlerImpl.unwrapB(_inputBuffer);

                        int bytes = pourAll(_inputBuffer, _underlyingInput);
                        if (bytes == Transport.END_OF_STREAM) {
                            _tail_closed = true;
                        }
                        _inputBuffer.compact();
                        
                        _underlyingInput.process();
                    }
                    break;
                case PN_WS_CLOSED:
                    break;
                case PN_WS_FAILED:
                    break;
                default:
                    break;
            }
        }

        @Override
        public int capacity()
        {
            if (_webSocketEnabled)
            {
                if (_tail_closed)
                {
                    return Transport.END_OF_STREAM;
                }
                else
                {
                    return _inputBuffer.remaining();
                }
            }
            else
            {
                return _underlyingInput.capacity();
            }
        }

        @Override
        public int position()
        {
            if (_webSocketEnabled)
            {
                if (_tail_closed)
                {
                    return Transport.END_OF_STREAM;
                }
                else
                {
                    return _inputBuffer.position();
                }
            }
            else
            {
                return _underlyingInput.position();
            }
        }

        @Override
        public ByteBuffer tail()
        {
            if (_webSocketEnabled)
            {
                return _inputBuffer;
            }
            else
            {
                return _underlyingInput.tail();
            }
        }

        @Override
        public void process() throws TransportException
        {
            if (_webSocketEnabled)
            {
                _inputBuffer.flip();

                switch (_state)
                {
                    case PN_WS_NOT_STARTED:
                        _underlyingInput.process();
                        break;
                    case PN_WS_CONNECTING:
                        try
                        {
                            processInput();
                        } finally
                        {
                            _inputBuffer.compact();
                        }
                        break;
                    case PN_WS_CONNECTED:
                        processInput();
                        break;
                    case PN_WS_FAILED:
                        _underlyingInput.process();
                        break;
                    default:
                        _underlyingInput.process();
                }
            }
            else
            {
                _underlyingInput.process();
            }
        }

        @Override
        public void close_tail()
        {
            _tail_closed = true;
            if (_webSocketEnabled)
            {
                _head_closed = true;
                _underlyingInput.close_tail();
            }
            else
            {
                _underlyingInput.close_tail();
            }
        }

        @Override
        public int pending()
        {
            if (_webSocketEnabled)
            {
                switch (_state)
                {
                    case PN_WS_NOT_STARTED:
                        if (_outputBuffer.position() == 0)
                        {
                            _state = WebSocketState.PN_WS_CONNECTING;

                            writeUpgradeRequest();

                            _head.limit(_outputBuffer.position());

                            if (_head_closed)
                            {
                                _state = WebSocketState.PN_WS_FAILED;
                                return Transport.END_OF_STREAM;
                            }
                            else
                            {
                                return _outputBuffer.position();
                            }
                        }
                        else
                        {
                            return _outputBuffer.position();
                        }
                    case PN_WS_CONNECTING:
                        if (_head_closed && _outputBuffer.position() == 0)
                        {
                            _state = WebSocketState.PN_WS_FAILED;
                            return Transport.END_OF_STREAM;
                        }
                        else
                        {
                            return _outputBuffer.position();
                        }
                    case PN_WS_CONNECTED:
                        return _underlyingOutput.pending();
                    case PN_WS_FAILED:
                        return Transport.END_OF_STREAM;
                    default:
                        return 0;
                }
            }
            else
            {
                return _underlyingOutput.pending();
            }
        }

        @Override
        public ByteBuffer head()
        {
            if (_webSocketEnabled)
            {
                switch (_state) {
                    case PN_WS_CONNECTING:
                        pending();
                        return _head;
                    case PN_WS_CONNECTED:
                    case PN_WS_NOT_STARTED:
                    case PN_WS_CLOSED:
                    case PN_WS_FAILED:
                    default:
                        return _underlyingOutput.head();
                }
            }
            else
            {
                return _underlyingOutput.head();
            }
        }

        @Override
        public void pop(int bytes)
        {
            if (_webSocketEnabled)
            {
                if (_outputBuffer.position() != 0)
                {
                    _outputBuffer.flip();
                    _outputBuffer.position(bytes);
                    _outputBuffer.compact();
                    _head.position(0);
                    _head.limit(_outputBuffer.position());
                }
                else
                {
                    _underlyingOutput.pop(bytes);
                }
            }
            else
            {
                _underlyingOutput.pop(bytes);
            }
        }

        @Override
        public void close_head()
        {
            _underlyingOutput.close_head();
        }
    }
}
