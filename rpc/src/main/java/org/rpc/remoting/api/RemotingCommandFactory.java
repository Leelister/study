package org.rpc.remoting.api;

import org.rpc.remoting.api.payload.RequestCommand;
import org.rpc.remoting.api.payload.ResponseCommand;

public class RemotingCommandFactory {

    public static RequestCommand createRequestCommand(byte serializerCode, byte[] body) {
        RequestCommand requestCommand = new RequestCommand(
                serializerCode,
                body
        );
        return requestCommand;
    }

    public static RequestCommand createRequestCommand(byte messageCode, byte serializerCode, byte[] body) {
        RequestCommand requestCommand = new RequestCommand(
                messageCode,
                serializerCode,
                body
        );
        return requestCommand;
    }

    public static RequestCommand createRequestCommand(byte messageCode, byte serializerCode,
                                                      byte[] body, long invokeId) {
        RequestCommand requestCommand = new RequestCommand(
                messageCode,
                serializerCode,
                body,
                invokeId
        );
        return requestCommand;
    }

    public static ResponseCommand createResponseCommand(byte serializerCode, byte[] body, long invokeId) {
        ResponseCommand responseCommand = new ResponseCommand(
                serializerCode,
                body,
                invokeId
        );
        return responseCommand;
    }

    public static ResponseCommand createResponseCommand(byte messageCode, byte serializerCode,
                                                        byte[] body, long invokeId) {
        ResponseCommand responseCommand = new ResponseCommand(
                serializerCode,
                body,
                invokeId
        );
        return responseCommand;
    }
}
