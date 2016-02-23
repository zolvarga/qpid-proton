package org.apache.qpid.proton.engine.impl;

import java.security.InvalidParameterException;

public class WebSocketUpgradeRequest
{
    private final String _httpMethod = "GET / ";
    private final String _httpVersion = "HTTP/1.1";
    private final String _connectionHeader = "Connection: Upgrade";
    private final String _upgradeHeader = "Upgrade: websocket";
    private final String _versionHeader = "Sec-WebSocket-Version: 13";


    private String _host = "";
    private String _path = "";
    private String _key = "";
    private String _protocol = "AMQPWSB10";

    public void setHost(String host)
    {
        this._host = host;
    }

    public void setPath(String path)
    {
        this._path = path;
    }

    public void setProtocol(String protocol)
    {
        this._protocol = protocol;
    }

//    /**
//     * Sets the header field to the given value.
//     *
//     * @param field The header field name.
//     * @param value The header field value.
//     *
//     * @return The object itself, for fluent setting.
//     */
//    public HttpRequest setHeaderField(String field, String value)
//    {
//        // Codes_SRS_SERVICE_SDK_JAVA_HTTPREQUEST_12_009: [The function shall set the header field with the given name to the given value.]
//        this.connection.setRequestHeader(field, value);
//        return this;
//    }

    @Override
    public String toString()
    {
        _host = "iot-sdks-test.azure-devices.net";
        if (_host.isEmpty())
            throw new InvalidParameterException("host string is empty");

        // GENERATE KEY!!!
        _key = "mQzPElOHKd+RwPyWnWOJiQ==";

        _path = "/$iothub/websocket";

        String endOfLine = "\r\n";
        StringBuilder stringBuilder = new StringBuilder()
                .append(_httpMethod).append(_path).append(_httpVersion).append(endOfLine)
                .append("Connection: Upgrade").append(endOfLine)
                .append("Upgrade: websocket").append(endOfLine)
                .append("Sec-WebSocket-Key: ").append(_key).append(endOfLine)
                .append("Sec-WebSocket-Version: 13").append(endOfLine)
                .append("Sec-WebSocket-Protocol:").append(_protocol).append(endOfLine)
                .append("Host: ").append(_host)
                .append(endOfLine)
                .append(endOfLine)
                ;

        String upgradeRequest = stringBuilder.toString();
        return upgradeRequest;
    }
}
