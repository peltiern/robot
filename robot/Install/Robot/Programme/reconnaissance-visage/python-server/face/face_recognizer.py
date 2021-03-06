import face_recognition
import numpy as np
import cv2
import time
import logging
import os
import pandas as pd
import utils.utils as utils


class FaceRecognizer:

    def __init__(self, known_faces_dir):
        self.known_faces_dir = known_faces_dir
        # Chargement des visages connus
        self.__load_known_faces()

    def __load_known_faces(self):
        # Charge l'encodage et le nom des visages connus
        self.known_faces_names = []
        self.known_faces_encodings = []

        # Recherche du fichier des encodages des visages connus
        known_faces_file_name = '/known-faces.csv'
        known_faces_file = self.known_faces_dir + known_faces_file_name
        if os.path.exists(known_faces_file) and os.path.isfile(known_faces_file):
            df_known_names_encodings = pd.read_csv(known_faces_file, header=None)
            self.known_faces_names = df_known_names_encodings[0].tolist()
            for i in range((df_known_names_encodings.shape[0])):
                self.known_faces_encodings.append(list(df_known_names_encodings.iloc[i, 1:]))
        else:
            # Le fichier n'existe pas : récupération à partir des images + sauvegarde
            for file in utils.image_files_in_folder(self.known_faces_dir):
                basename = os.path.splitext(os.path.basename(file))[0]
                img = face_recognition.load_image_file(file)
                encodings = face_recognition.face_encodings(img)

                if len(encodings) > 1:
                    logging.warning("More than one face found in {}. Only considering the first face.".format(file))

                if len(encodings) == 0:
                    logging.warning("WARNING: No faces found in {}. Ignoring file.".format(file))
                else:
                    self.known_faces_names.append(basename)
                    self.known_faces_encodings.append(encodings[0])

            df_known_names = pd.DataFrame(self.known_faces_names)
            df_known_encodings = pd.DataFrame(self.known_faces_encodings)

            # Sauvegarde dans un fichier
            df_known_names_encodings = pd.concat([df_known_names, df_known_encodings], axis=1, ignore_index=True)
            df_known_names_encodings.to_csv(self.known_faces_dir + known_faces_file_name, index=False, header=False)

    def __detect_faces(self, image_file, recognition=False):
        # Initialize some variables
        face_names = []

        # Load the uploaded image file
        img = face_recognition.load_image_file(image_file)
        small_img = cv2.resize(img, (0, 0), fx=0.5, fy=0.5)

        # Find all the faces and face encodings in the current frame of video
        face_boxes = face_recognition.face_locations(small_img)
        face_landmarks_list = face_recognition.face_landmarks(small_img, face_boxes)

        face_found = False

        if recognition:
            face_encodings = face_recognition.face_encodings(small_img, face_boxes)

            for face_encoding in face_encodings:
                # See if the face is a match for the known face(s)
                matches = face_recognition.compare_faces(self.known_faces_encodings, face_encoding)
                name = ""

                # # If a match was found in known_face_encodings, just use the first one.
                # if True in matches:
                #     first_match_index = matches.index(True)
                #     name = known_face_names[first_match_index]

                # Or instead, use the known face with the smallest distance to the new face
                face_distances = face_recognition.face_distance(self.known_faces_encodings, face_encoding)
                best_match_index = np.argmin(face_distances)

                if matches[best_match_index]:
                    name = self.known_faces_names[best_match_index]

                face_names.append(name)

        else:
            # Juste la détection
            for face_location in face_boxes:
                face_names.append("")

        return face_boxes, face_names, face_landmarks_list

    def detect_faces(self, image_file):
        return self.__detect_faces(image_file, False)

    def recognize_faces(self, image_file):
        return self.__detect_faces(image_file, True)
