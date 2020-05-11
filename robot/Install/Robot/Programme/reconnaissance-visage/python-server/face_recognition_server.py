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

import face_recognition
from flask import Flask, jsonify, request, redirect
import numpy as np
import cv2
import time
import argparse
import logging
import os
import re
import pandas as pd

# You can change this to any folder on your system
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}

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


def scan_known_people(known_people_folder):
    known_names = []
    known_encodings = []
    known_faces_file = known_people_folder + '/known-faces.csv'

    if os.path.exists(known_faces_file) and os.path.isfile(known_faces_file):
        df_known_names_encodings = pd.read_csv(known_faces_file, header=None)
        known_names = df_known_names_encodings[0].tolist()
        for i in range((df_known_names_encodings.shape[0])):
            known_encodings.append(list(df_known_names_encodings.iloc[i, 1:]))
    else:
        # Le fichier n'existe pas : récupération à partir des images + sauvegarde
        for file in image_files_in_folder(known_people_folder):
            basename = os.path.splitext(os.path.basename(file))[0]
            img = face_recognition.load_image_file(file)
            encodings = face_recognition.face_encodings(img)

            if len(encodings) > 1:
                logging.warning("More than one face found in {}. Only considering the first face.".format(file))

            if len(encodings) == 0:
                logging.warning("WARNING: No faces found in {}. Ignoring file.".format(file))
            else:
                known_names.append(basename)
                known_encodings.append(encodings[0])

        df_known_names = pd.DataFrame(known_names)
        df_known_encodings = pd.DataFrame(known_encodings)

        # Sauvegarde dans un fichier
        df_known_names_encodings = pd.concat([df_known_names, df_known_encodings], axis=1, ignore_index=True)
        df_known_names_encodings.to_csv(known_people_folder + '/known_faces.csv', index=False, header=False)

    return known_names, known_encodings


def image_files_in_folder(folder):
    return [os.path.join(folder, f) for f in os.listdir(folder) if re.match(r'.*\.(jpg|jpeg|png)', f, flags=re.I)]


def allowed_file(filename):
    return '.' in filename and \
           filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS


# Charge l'encodage et le nom des visages connus
known_face_names, known_face_encodings = scan_known_people(known_faces_dir)


@app.route('/face-recognition', methods=['GET', 'POST'])
def recognize_faces():
    return upload_image(True)


@app.route('/face-detection', methods=['GET', 'POST'])
def detect_faces():
    return upload_image(False)


def upload_image(recognition):
    # Check if a valid image file was uploaded
    if request.method == 'POST':
        if 'file' not in request.files:
            return redirect(request.url)

        file = request.files['file']

        if file.filename == '':
            return redirect(request.url)

        if file and allowed_file(file.filename):
            # The image file seems valid! Detect faces and return the result.
            return detect_faces_in_image(file, recognition)

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


def detect_faces_in_image(file_stream, recognition):
    avant = int(round(time.time() * 1000))
    # Initialize some variables
    face_names = []

    # Load the uploaded image file
    img = face_recognition.load_image_file(file_stream)
    small_img = cv2.resize(img, (0, 0), fx=0.5, fy=0.5)

    # Find all the faces and face encodings in the current frame of video
    face_locations = face_recognition.face_locations(small_img)
    face_landmarks_list = face_recognition.face_landmarks(small_img, face_locations)

    face_found = False

    if recognition:
        face_encodings = face_recognition.face_encodings(small_img, face_locations)

        if len(face_encodings) > 0:
            face_found = True

        for face_encoding in face_encodings:
            # See if the face is a match for the known face(s)
            matches = face_recognition.compare_faces(known_face_encodings, face_encoding)
            name = ""

            # # If a match was found in known_face_encodings, just use the first one.
            # if True in matches:
            #     first_match_index = matches.index(True)
            #     name = known_face_names[first_match_index]

            # Or instead, use the known face with the smallest distance to the new face
            face_distances = face_recognition.face_distance(known_face_encodings, face_encoding)
            best_match_index = np.argmin(face_distances)

            if matches[best_match_index]:
                name = known_face_names[best_match_index]

            face_names.append(name)

    else:
        # Juste la détection
        if len(face_locations) > 0:
            face_found = True
        for face_location in face_locations:
            face_names.append("")

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
        "faceFound": face_found,
        "faces": faces

    }

    json = str(results)

    apres = int(round(time.time() * 1000))
    logging.debug('(' + str(apres - avant) + " ms) : " + json)
    return jsonify(results)


if __name__ == "__main__":
    app.run(host='localhost', port=5001, debug=True)
