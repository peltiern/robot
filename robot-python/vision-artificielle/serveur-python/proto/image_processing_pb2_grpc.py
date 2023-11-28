# Generated by the gRPC Python protocol compiler plugin. DO NOT EDIT!
"""Client and server classes corresponding to protobuf-defined services."""
import grpc

from proto import image_processing_pb2 as proto_dot_image__processing__pb2


class ImageProcessingServiceStub(object):
    """Missing associated documentation comment in .proto file."""

    def __init__(self, channel):
        """Constructor.

        Args:
            channel: A grpc.Channel.
        """
        self.processImage = channel.unary_unary(
                '/org.example.dto.image.ImageProcessingService/processImage',
                request_serializer=proto_dot_image__processing__pb2.ImageProcessingRequest.SerializeToString,
                response_deserializer=proto_dot_image__processing__pb2.ImageProcessingResponse.FromString,
                )


class ImageProcessingServiceServicer(object):
    """Missing associated documentation comment in .proto file."""

    def processImage(self, request, context):
        """Missing associated documentation comment in .proto file."""
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)
        context.set_details('Method not implemented!')
        raise NotImplementedError('Method not implemented!')


def add_ImageProcessingServiceServicer_to_server(servicer, server):
    rpc_method_handlers = {
            'processImage': grpc.unary_unary_rpc_method_handler(
                    servicer.processImage,
                    request_deserializer=proto_dot_image__processing__pb2.ImageProcessingRequest.FromString,
                    response_serializer=proto_dot_image__processing__pb2.ImageProcessingResponse.SerializeToString,
            ),
    }
    generic_handler = grpc.method_handlers_generic_handler(
            'org.example.dto.image.ImageProcessingService', rpc_method_handlers)
    server.add_generic_rpc_handlers((generic_handler,))


 # This class is part of an EXPERIMENTAL API.
class ImageProcessingService(object):
    """Missing associated documentation comment in .proto file."""

    @staticmethod
    def processImage(request,
            target,
            options=(),
            channel_credentials=None,
            call_credentials=None,
            insecure=False,
            compression=None,
            wait_for_ready=None,
            timeout=None,
            metadata=None):
        return grpc.experimental.unary_unary(request, target, '/org.example.dto.image.ImageProcessingService/processImage',
            proto_dot_image__processing__pb2.ImageProcessingRequest.SerializeToString,
            proto_dot_image__processing__pb2.ImageProcessingResponse.FromString,
            options, channel_credentials,
            insecure, call_credentials, compression, wait_for_ready, timeout, metadata)
