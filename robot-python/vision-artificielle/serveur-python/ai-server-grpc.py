import argparse
import io
import json
import logging
import time
from concurrent import futures

import grpc

from face.face_recognizer import FaceRecognizer
from proto import image_processing_pb2
from proto import image_processing_pb2_grpc

logging.basicConfig(filename='test_log.log',level=logging.ERROR, \
                    format='%(asctime)s -- %(name)s -- %(levelname)s -- %(message)s')

# Récupération des arguments en entrée
parser = argparse.ArgumentParser()
parser.add_argument("known_faces_dir", help="The directory of the known faces")
args = parser.parse_args()
known_faces_dir = args.known_faces_dir
if known_faces_dir == '':
    raise ValueError('Le répertoire des visages n''est pas renseigné')


class ImageProcessingService(image_processing_pb2_grpc.ImageProcessingServiceServicer):

    def __init__(self):
        # Création de l'objet de reconnaissance faciale
        self.face_recognizer = FaceRecognizer(known_faces_dir)
        # Création de l'objet de détection d'objets
        # self.object_detector = YoloV3ObjectDetector()

    def processImage(self, request, context):
        resultat = self.detect_faces_in_image(io.BytesIO(request.image), True)
        return image_processing_pb2.ImageProcessingResponse(jsonResponse=resultat, processingType='face-recognition')

    def detect_faces_in_image(self, image_file, recognition):
        before = int(round(time.time() * 1000))
        if recognition:
            # Recognition
            face_locations, face_names, face_landmarks_list = self.face_recognizer.recognize_faces(image_file)
        else:
            # Detection
            face_locations, face_names, face_landmarks_list = self.face_recognizer.detect_faces(image_file)

        faces = []
        for (top, right, bottom, left), name, landmarks in zip(face_locations, face_names, face_landmarks_list):
            # Bounds
            top *= 2
            right *= 2
            bottom *= 2
            left *= 2

            # Landmark
            for k, v in landmarks.items():
                landmarks[k] = [(x * 2, y * 2) for x, y in landmarks[k]]
            chin = [{"x": x, "y": y} for x, y in landmarks["chin"]]
            left_eyebrow = [{"x": x, "y": y} for x, y in landmarks["left_eyebrow"]]
            right_eyebrow = [{"x": x, "y": y} for x, y in landmarks["right_eyebrow"]]
            nose_bridge = [{"x": x, "y": y} for x, y in landmarks["nose_bridge"]]
            nose_tip = [{"x": x, "y": y} for x, y in landmarks["nose_tip"]]
            left_eye = [{"x": x, "y": y} for x, y in landmarks["left_eye"]]
            right_eye = [{"x": x, "y": y} for x, y in landmarks["right_eye"]]
            top_lip = [{"x": x, "y": y} for x, y in landmarks["top_lip"]]
            bottom_lip = [{"x": x, "y": y} for x, y in landmarks["bottom_lip"]]
            faces.append({
                "x": left,
                "y": top,
                "width": right - left,
                "height": bottom - top,
                "name": name,
                "landmarks": {
                    "chin": chin,
                    "left_eyebrow": left_eyebrow,
                    "right_eyebrow": right_eyebrow,
                    "nose_bridge": nose_bridge,
                    "nose_tip": nose_tip,
                    "left_eye": left_eye,
                    "right_eye": right_eye,
                    "top_lip": top_lip,
                    "bottom_lip": bottom_lip
                }
            })

        # Return the result as json
        results = {
            "faceFound": len(faces) > 0,
            "faces": faces

        }

        # Convertir l'objet Python en une chaîne de caractères JSON
        json_string = json.dumps(results)

        # Encoder la chaîne de caractères en UTF-8
        utf8_string = json_string.encode('utf-8')

        after = int(round(time.time() * 1000))
        print('(' + str(after - before) + " ms) : " + json_string)
        return utf8_string


    def detect_objects_in_image(self, image_file):
        before = int(round(time.time() * 1000))

        # Detection
        object_locations, object_names, object_scores = self.object_detector.detect_objects(image_file)

        objects = []
        for (x, y, width, height), name, score in zip(object_locations, object_names, object_scores):
            objects.append({
                "x": x,
                "y": y,
                "width": width,
                "height": height,
                "name": name,
                "score": score
            })

        # Return the result as json
        results = {
            "objectFound": len(objects) > 0,
            "objects": objects

        }

        # Convertir l'objet Python en une chaîne de caractères JSON
        json_string = json.dumps(results)

        # Encoder la chaîne de caractères en UTF-8
        utf8_string = json_string.encode('utf-8')

        after = int(round(time.time() * 1000))
        print('(' + str(after - before) + " ms) : " + json_string)
        return utf8_string


def serve():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    image_processing_pb2_grpc.add_ImageProcessingServiceServicer_to_server(ImageProcessingService(), server)
    server.add_insecure_port('[::]:50051')
    server.start()
    print('Serveur démarré')
    server.wait_for_termination()

if __name__ == '__main__':
    serve()
