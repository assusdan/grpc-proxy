package com.codahale.grpcproxy.dc;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpTransportInterceptor implements io.grpc.ServerInterceptor {
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpTransportInterceptor.class);

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> serverCall,
      Metadata metadata,
      ServerCallHandler<ReqT, RespT> serverCallHandler) {
    LOGGER.info(metadata.toString());
    return serverCallHandler.startCall(serverCall, metadata);
  }
}
