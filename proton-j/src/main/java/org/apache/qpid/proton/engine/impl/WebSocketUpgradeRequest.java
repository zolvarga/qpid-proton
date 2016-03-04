package org.apache.qpid.proton.engine.impl;

import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class WebSocketUpgradeRequest
{
    private final byte _colon = ':';
    private final byte _slash = '/';

    private String _host = "";
    private String _path = "";
    private String _port = "";
    private String _protocol = "AMQPWSB10";
    private String _webSocketKey = "";

    private Map<String, String> _additionalHeaders = null;

    public WebSocketUpgradeRequest(
            String hostName,
            String webSocketPath,
            int webSocketPort,
            String webSocketProtocol,
            Map<String, String> additionalHeaders)
    {
        setHost(hostName);
        setPath(webSocketPath);
        setPort(webSocketPort);
        setProtocol(webSocketProtocol);
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
        _port = "";
        if (port != 0)
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
            key[i] = (byte) (int) (Math.random() * 256);
        }
        return Base64.getEncoder().encodeToString(key).trim();
    }

    public String createUpgradeRequest()
    {
        if (_host.isEmpty())
            throw new InvalidParameterException("host header has no value");

        if (_protocol.isEmpty())
            throw new InvalidParameterException("protocol header has no value");

        _webSocketKey = createWebSocketKey();

        String _endOfLine = "\r\n";
        StringBuilder stringBuilder = new StringBuilder()
                .append("GET ").append(_path).append(" HTTP/1.1").append(_endOfLine)
                .append("Connection: Upgrade").append(_endOfLine)
                .append("Upgrade: websocket").append(_endOfLine)
                .append("Sec-WebSocket-Version: 13").append(_endOfLine)
                .append("Sec-WebSocket-Key: ").append(_webSocketKey).append(_endOfLine)
                .append("Sec-WebSocket-Protocol: ").append(_protocol).append(_endOfLine);

        stringBuilder.append("Host: ").append(_host + _port).append(_endOfLine);

        if (_additionalHeaders != null)
        {
            for (Map.Entry<String, String> entry : _additionalHeaders.entrySet())
            {
                stringBuilder.append(entry.getKey() + ": " + entry.getValue()).append(_endOfLine);
            }
        }

        stringBuilder.append(_endOfLine);

        return stringBuilder.toString();
    }

    public Boolean validateUpgradeReply(byte[] responseBytes)
    {
        String httpString = new String(responseBytes, StandardCharsets.UTF_8);

        List<String> httpLines = new ArrayList<String>();
        Scanner scanner = new Scanner(httpString);
        while (scanner.hasNextLine())
        {
            httpLines.add(scanner.nextLine());
        }
        scanner.close();

        Boolean isStatusLineOk = false;
        Boolean isUpgradeHeaderOk = false;
        Boolean isConnectionHeaderOk = false;
        Boolean isProtocolHeaderOk = false;
        Boolean isAcceptHeaderOk = false;

        for (Iterator<String> iterator = httpLines.iterator(); iterator.hasNext(); )
        {
            String line = iterator.next();

            if ((line.toLowerCase().contains("http/1.1")) && (line.contains("101")) && (line.toLowerCase().contains("switching protocols")))
            {
                isStatusLineOk = true;
                continue;
            }
            if ((line.toLowerCase().contains("upgrade")) && (line.toLowerCase().contains("websocket")))
            {
                isUpgradeHeaderOk = true;
                continue;
            }
            if ((line.toLowerCase().contains("connection")) && (line.toLowerCase().contains("upgrade")))
            {
                isConnectionHeaderOk = true;
                continue;
            }
            if (line.toLowerCase().contains("sec-websocket-protocol") && (line.toLowerCase().contains("amqpwsb10")))
            {
                isProtocolHeaderOk = true;
                continue;
            }
            if (line.toLowerCase().contains("sec-websocket-accept"))
            {
                MessageDigest messageDigest = null;
                try
                {
                    messageDigest = MessageDigest.getInstance("SHA-1");
                } catch (NoSuchAlgorithmException e)
                {
                    e.printStackTrace();
                }

                String expectedKey = Base64.getEncoder().encodeToString(messageDigest.digest((_webSocketKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes()));

                if (line.contains(expectedKey))
                {
                    isAcceptHeaderOk = true;
                }
                continue;
            }
        }

        if ((isStatusLineOk) && (isUpgradeHeaderOk) && (isConnectionHeaderOk) && (isProtocolHeaderOk) && (isAcceptHeaderOk))
        {
            return true;
        }
        else
        {
            return false;
        }
    }
}
