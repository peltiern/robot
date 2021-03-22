# This is a _very simple_ example of a web service that recognizes faces in uploaded images.
# Upload an image file and it will check if the image contains a picture of Barack Obama.
# The result is returned as json. For example:
#
# $ curl -XPOST -F "file=@obama2.jpg" http://127.0.0.1:5001
#
# Returns:
#
# {
#  "face_found_in_image": true,
#  "is_picture_of_obama": true
# }
#
# This example is based on the Flask file upload example: http://flask.pocoo.org/docs/0.12/patterns/fileuploads/

# NOTE: This example requires flask to be installed! You can install it with pip:
# $ pip3 install flask

from flask import Flask, jsonify, request, redirect
import time
import argparse
import logging

# You can change this to any folder on your system
from face.face_recognizer import FaceRecognizer
from object.yolov3_object_detector import YoloV3ObjectDetector
from utils.utils import allowed_file

app = Flask(__name__)

logging.basicConfig(filename='test_log.log',level=logging.DEBUG,\
      format='%(asctime)s -- %(name)s -- %(levelname)s -- %(message)s')

# Récupération des arguments en entrée
parser = argparse.ArgumentParser()
parser.add_argument("known_faces_dir", help="The directory of the known faces")
args = parser.parse_args()
known_faces_dir = args.known_faces_dir
if known_faces_dir == '':
    raise ValueError('Le répertoire des visages n''est pas renseigné')

# Création de l'objet de reconnaissance faciale
face_recognizer = FaceRecognizer(known_faces_dir)
# Création de l'objet de détection d'objets
object_detector = YoloV3ObjectDetector()


@app.route('/face-recognition', methods=['GET', 'POST'])
def recognize_faces():
    return process_detection_or_recognition('FR')


@app.route('/face-detection', methods=['GET', 'POST'])
def detect_faces():
    return process_detection_or_recognition('FD')


@app.route('/object-detection', methods=['GET', 'POST'])
def detect_objects():
    return process_detection_or_recognition('OD')


def process_detection_or_recognition(process_type):
    # Check if a valid image file was uploaded
    if request.method == 'POST':
        if 'file' not in request.files:
            return redirect(request.url)

        file = request.files['file']

        if file.filename == '':
            return redirect(request.url)

        if file and allowed_file(file.filename):
            if (process_type == 'OD'):
                return detect_objects_in_image(file)
            else:
                return detect_faces_in_image(file, process_type == 'FR')

    # If no valid image file was uploaded, show the file upload form:
    return '''
    <!doctype html>
    <title>Is this a picture of Obama?</title>
    <h1>Upload a picture and see if it's a picture of Obama!</h1>
    <form method="POST" enctype="multipart/form-data">
      <input type="file" name="file">
      <input type="submit" value="Upload">
    </form>
    '''


def detect_faces_in_image(image_file, recognition):
    before = int(round(time.time() * 1000))
    if recognition:
        # Recognition
        face_locations, face_names, face_landmarks_list = face_recognizer.recognize_faces(image_file)
    else:
        # Detection
        face_locations, face_names, face_landmarks_list = face_recognizer.detect_faces(image_file)

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

    json = str(results)

    after = int(round(time.time() * 1000))
    logging.debug('(' + str(after - before) + " ms) : " + json)
    return jsonify(results)


def detect_objects_in_image(image_file):
    before = int(round(time.time() * 1000))

    # Detection
    object_locations, object_names, object_scores = object_detector.detect_objects(image_file)

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

    json = str(results)

    after = int(round(time.time() * 1000))
    logging.debug('(' + str(after - before) + " ms) : " + json)
    return jsonify(results)


if __name__ == "__main__":
    app.run(host='localhost', port=5001, debug=True)
