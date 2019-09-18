# grpc-context-debug

### Steps to reproduce 


This will always succeed
```bash
grpcurl -max-time 10 -plaintext -d '{"run_async": true}' 127.0.0.1:50051 grpc.example.ContextExample/RunTest
```

The `fireAndForget` RPC will fail on the second invocation with an `io.grpc.StatusRuntimeException: CANCELLED: io.grpc.Context was cancelled without error`
 
```bash
grpcurl -max-time 10 -plaintext -d '{"run_async": false}' 127.0.0.1:50051 grpc.example.ContextExample/RunTest
grpcurl -max-time 10 -plaintext -d '{"run_async": false}' 127.0.0.1:50051 grpc.example.ContextExample/RunTest
```