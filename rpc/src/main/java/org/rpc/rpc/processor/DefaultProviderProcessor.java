package org.rpc.rpc.processor;

import com.esotericsoftware.reflectasm.MethodAccess;
import io.netty.channel.ChannelHandlerContext;
import org.rpc.remoting.api.RequestProcessor;
import org.rpc.remoting.api.payload.RequestBytes;
import org.rpc.remoting.api.payload.ResponseBytes;
import org.rpc.remoting.api.procotol.ProtocolHead;
import org.rpc.rpc.model.RequestWrapper;
import org.rpc.rpc.model.ServiceWrapper;
import org.rpc.rpc.provider.Provider;
import org.rpc.serializer.Serializer;
import org.rpc.serializer.SerializerFactory;
import org.rpc.serializer.SerializerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;

public class DefaultProviderProcessor implements RequestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DefaultProviderProcessor.class);

    private ConcurrentHashMap<Class<?>, MethodAccess> methodAccessCache = new ConcurrentHashMap<>();

    private Provider provider;

    public DefaultProviderProcessor(Provider provider) {
        this.provider = provider;
    }

    @Override
    public ResponseBytes process(ChannelHandlerContext context, RequestBytes request) {

        SerializerType serializerType = SerializerType.parse(request.getSerializerCode());
        Serializer serializer = SerializerFactory.serializer(serializerType);

        switch (request.getMessageCode()) {
            case ProtocolHead.REQUEST : {
                RequestWrapper requestWrapper = serializer.deserialize(request.getBody(), RequestWrapper.class);
                ServiceWrapper serviceWrapper = provider.lookupService(requestWrapper.getServiceMeta());

                String errorMessage = String.format("%s not register local serviceContainer", requestWrapper.getServiceMeta().directory());
                checkNotNull(serviceWrapper, errorMessage);

                MethodAccess methodAccess = methodAccessCache.get(serviceWrapper.getServiceProvider().getClass());
                if (methodAccess == null) {
                    MethodAccess newMethodAccess = MethodAccess.get(serviceWrapper.getServiceProvider().getClass());
                    methodAccess = methodAccessCache.putIfAbsent(serviceWrapper.getServiceProvider().getClass(), newMethodAccess);
                    if (methodAccess == null) {
                        methodAccess = newMethodAccess;
                    }
                }
                Object result = methodAccess.invoke(serviceWrapper.getServiceProvider(), requestWrapper.getMethodName(), requestWrapper.getArgs());

                ResponseBytes responseBytes = new ResponseBytes(request.getSerializerCode(), serializer.serialize(result));
                responseBytes.setStatus(ProtocolHead.STATUS_SUCCESS);
                responseBytes.setInvokeId(request.getInvokeId());

                return responseBytes;
            }
            default:
                throw new UnsupportedOperationException("DefaultProviderProcessor Unsupported MessageCode: " + request.getMessageCode());
        }
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }
}