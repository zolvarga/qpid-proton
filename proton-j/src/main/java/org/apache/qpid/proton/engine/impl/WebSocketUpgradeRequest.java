package org.apache.qpid.proton.engine.impl;

import java.net.URI;
import java.security.InvalidParameterException;
import java.util.Base64;
import java.util.Map;

public class WebSocketUpgradeRequest
{
    private final byte _colon = ':';
    private final byte _slash = '/';

    private String _host = "";
    private String _path = "";
    private String _port = "";
    private String _protocol = "AMQPWSB10";

    private Map<String, String> _additionalHeaders = null;

    public WebSocketUpgradeRequest(String hostName, String path, int port, String protocol, Map<String, String> additionalHeaders)
    {
        setHost(hostName);
        setPath(path);
        setPort(port);
        setProtocol(protocol);
        setAdditionalHeaders(additionalHeaders);
    }

    /**
     * Set host value in host header
     *
     * @param host The host header field value.
     */
    public void setHost(String host)
    {
        this._host = host;
    }

    /**
     * Set port value in host header
     *
     * @param port The port header field value.
     */
    public void setPort(int port)
    {
        _port = String.valueOf(port);
        if (!_port.isEmpty())
        {
            if (_port.charAt(0) != ':')
            {
                _port = _colon + _port;
            }
        }
    }

    /**
     * Set path value in handshake
     *
     * @param path The path field value.
     */
    public void setPath(String path)
    {
        _path = path;
        if (!_path.isEmpty())
        {
            if (_path.charAt(0) != _slash)
            {
                _path = _slash + _path;
            }
        }
    }

    /**
     * Set protocol value in protocol header
     *
     * @param protocol The protocol header field value.
     */
    public void setProtocol(String protocol)
    {
        _protocol = protocol;
    }

    /**
     * Add field-value pairs to HTTP header
     *
     * @param additionalHeaders  The Map containing the additional headers.
     */
    public void setAdditionalHeaders(Map<String, String> additionalHeaders)
    {
        _additionalHeaders = additionalHeaders;
    }

    /**
     * Utility function to clear all additional headers
     */
    public void clearAdditionalHeaders()
    {
        _additionalHeaders.clear();
    }

    /**
     * Utility function to create random, Base64 encoded key
     */
    private String createWebSocketKey()
    {
        byte[] key = new byte[16];
        for (int i = 0; i < 16; i++)
        {
            key[i] = (byte) (int) (Math.random() * 255);
        }
        return String.valueOf(Base64.getEncoder().encode(key));
    }

    public String createUpgradeRequest()
    {
        if (_host.isEmpty())
            throw new InvalidParameterException("host header has no value");

        if (_protocol.isEmpty())
            throw new InvalidParameterException("protocol header has no value");

        String webSocketKey = createWebSocketKey();

        String _endOfLine = "\r\n";
        StringBuilder stringBuilder = new StringBuilder()
                .append("GET ").append(_path).append(" HTTP/1.1").append(_endOfLine)
                .append("Connection: Upgrade").append(_endOfLine)
                .append("Upgrade: websocket").append(_endOfLine)
                .append("Sec-WebSocket-Version: 13").append(_endOfLine)
                .append("Sec-WebSocket-Key: ").append(webSocketKey).append(_endOfLine)
                .append("Sec-WebSocket-Protocol: ").append(_protocol).append(_endOfLine);

        if (!_host.isEmpty())
            stringBuilder.append("Host: ").append(_host + _port);

        if (_additionalHeaders != null) {
            for (Map.Entry<String, String> entry : _additionalHeaders.entrySet()) {
                stringBuilder.append(entry.getKey() + ": " + entry.getValue());
            }
        }

        stringBuilder.append(_endOfLine).append(_endOfLine);

        return stringBuilder.toString();
    }
}
