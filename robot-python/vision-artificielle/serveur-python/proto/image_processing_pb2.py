# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: proto/image-processing.proto
"""Generated protocol buffer code."""
from google.protobuf import descriptor as _descriptor
from google.protobuf import descriptor_pool as _descriptor_pool
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()




DESCRIPTOR = _descriptor_pool.Default().AddSerializedFile(b'\n\x1cproto/image-processing.proto\x12\x15org.example.dto.image\"?\n\x16ImageProcessingRequest\x12\r\n\x05image\x18\x01 \x01(\x0c\x12\x16\n\x0eprocessingType\x18\x02 \x01(\t\"G\n\x17ImageProcessingResponse\x12\x14\n\x0cjsonResponse\x18\x01 \x01(\t\x12\x16\n\x0eprocessingType\x18\x02 \x01(\t2\x87\x01\n\x16ImageProcessingService\x12m\n\x0cprocessImage\x12-.org.example.dto.image.ImageProcessingRequest\x1a..org.example.dto.image.ImageProcessingResponseB\x02P\x01\x62\x06proto3')



_IMAGEPROCESSINGREQUEST = DESCRIPTOR.message_types_by_name['ImageProcessingRequest']
_IMAGEPROCESSINGRESPONSE = DESCRIPTOR.message_types_by_name['ImageProcessingResponse']
ImageProcessingRequest = _reflection.GeneratedProtocolMessageType('ImageProcessingRequest', (_message.Message,), {
  'DESCRIPTOR' : _IMAGEPROCESSINGREQUEST,
  '__module__' : 'proto.image_processing_pb2'
  # @@protoc_insertion_point(class_scope:org.example.dto.image.ImageProcessingRequest)
  })
_sym_db.RegisterMessage(ImageProcessingRequest)

ImageProcessingResponse = _reflection.GeneratedProtocolMessageType('ImageProcessingResponse', (_message.Message,), {
  'DESCRIPTOR' : _IMAGEPROCESSINGRESPONSE,
  '__module__' : 'proto.image_processing_pb2'
  # @@protoc_insertion_point(class_scope:org.example.dto.image.ImageProcessingResponse)
  })
_sym_db.RegisterMessage(ImageProcessingResponse)

_IMAGEPROCESSINGSERVICE = DESCRIPTOR.services_by_name['ImageProcessingService']
if _descriptor._USE_C_DESCRIPTORS == False:

  DESCRIPTOR._options = None
  DESCRIPTOR._serialized_options = b'P\001'
  _IMAGEPROCESSINGREQUEST._serialized_start=55
  _IMAGEPROCESSINGREQUEST._serialized_end=118
  _IMAGEPROCESSINGRESPONSE._serialized_start=120
  _IMAGEPROCESSINGRESPONSE._serialized_end=191
  _IMAGEPROCESSINGSERVICE._serialized_start=194
  _IMAGEPROCESSINGSERVICE._serialized_end=329
# @@protoc_insertion_point(module_scope)
