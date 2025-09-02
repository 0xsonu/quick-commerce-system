package com.ecommerce.gateway.grpc;

import io.grpc.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * gRPC client interceptor for collecting metrics
 */
@Component
public class GrpcMetricsInterceptor implements ClientInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(GrpcMetricsInterceptor.class);

    private final MeterRegistry meterRegistry;
    private final Timer.Builder timerBuilder;
    private final Counter.Builder errorCounterBuilder;

    public GrpcMetricsInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.timerBuilder = Timer.builder("grpc.client.duration")
            .description("Duration of gRPC client calls");
        this.errorCounterBuilder = Counter.builder("grpc.client.errors")
            .description("Number of gRPC client errors");
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {

        String serviceName = method.getServiceName();
        String methodName = method.getBareMethodName();

        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                next.newCall(method, callOptions)) {

            private Timer.Sample sample;

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                sample = Timer.start(meterRegistry);
                logger.debug("Starting gRPC call: {}/{}", serviceName, methodName);

                super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(
                        responseListener) {

                    @Override
                    public void onClose(Status status, Metadata trailers) {
                        // Record timing metrics
                        sample.stop(timerBuilder
                            .tag("service", serviceName)
                            .tag("method", methodName)
                            .tag("status", status.getCode().name())
                            .register(meterRegistry));

                        // Record error metrics
                        if (!status.isOk()) {
                            errorCounterBuilder
                                .tag("service", serviceName)
                                .tag("method", methodName)
                                .tag("error", status.getCode().name())
                                .register(meterRegistry)
                                .increment();
                            
                            logger.warn("gRPC call failed: {}/{} - Status: {}, Description: {}", 
                                serviceName, methodName, status.getCode(), status.getDescription());
                        } else {
                            logger.debug("gRPC call completed successfully: {}/{}", serviceName, methodName);
                        }

                        super.onClose(status, trailers);
                    }
                }, headers);
            }
        };
    }
}