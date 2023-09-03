import socket
import json
from PIL import Image
import io
import struct
import argparse
import time

from face.face_recognizer import FaceRecognizer

# Récupération des arguments en entrée
parser = argparse.ArgumentParser()
parser.add_argument("known_faces_dir", help="The directory of the known faces")
args = parser.parse_args()
known_faces_dir = args.known_faces_dir
if known_faces_dir == '':
    raise ValueError('Le répertoire des visages n''est pas renseigné')

# Création de l'objet de reconnaissance faciale
face_recognizer = FaceRecognizer(known_faces_dir)


def start_server():
    # Configuration du serveur
    host = 'localhost'
    port = 8502
    backlog = 5
    buffer_size = 4096

    # Création du socket du serveur
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.bind((host, port))
    server_socket.listen(backlog)
    print('Serveur à l\'écoute sur {}:{}'.format(host, port))

    while True:
        # Attente d'une connexion avec un client
        client_socket, client_address = server_socket.accept()
        print('Connexion acceptée depuis', client_address)

        try:
            # Lire la taille de l'image
            data_size = recvall(client_socket, 4)
            if not data_size:
                break
            data_size = struct.unpack('!I', data_size)[0]

            # Lire les données de l'image
            data = recvall(client_socket, int(data_size))

            # Vérification de l'image
            image = Image.open(io.BytesIO(data))
            if image.format not in ['JPEG', 'PNG']:
                raise ValueError('Format d\'image non supporté')

            # Traitement de l'image
            resultats = detect_faces_in_image(io.BytesIO(data), True)

            # Envoi de la réponse au client
            client_socket.sendall(resultats)
        except Exception as e:
            # Envoi de l'erreur au client
            resultats = {'error': str(e)}
            client_socket.sendall(json.dumps(resultats).encode('utf-8'))
            print('Erreur lors du traitement de l\'image:', e)

        # Fermeture de la connexion avec le client
        client_socket.close()

    # Fermeture du socket du serveur
    server_socket.close()


def recvall(sock, count):
    buf = b''
    while count:
        newbuf = sock.recv(count)
        if not newbuf: return None
        buf += newbuf
        count -= len(newbuf)
    return buf


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

    # Convertir l'objet Python en une chaîne de caractères JSON
    json_string = json.dumps(results)

    # Encoder la chaîne de caractères en UTF-8
    utf8_string = json_string.encode('utf-8')

    after = int(round(time.time() * 1000))
    print('(' + str(after - before) + " ms) : " + json_string)
    return utf8_string


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

    # Convertir l'objet Python en une chaîne de caractères JSON
    json_string = json.dumps(results)

    # Encoder la chaîne de caractères en UTF-8
    utf8_string = json_string.encode('utf-8')

    after = int(round(time.time() * 1000))
    print('(' + str(after - before) + " ms) : " + json_string)
    return utf8_string


if __name__ == '__main__':
    start_server()
