import logging
import os

import cv2
import face_recognition
import numpy as np
import pandas as pd
import csv

import utils.utils as utils


class FaceRecognizer:

    def __init__(self, known_faces_dir):
        self.known_faces_dir = known_faces_dir
        self.known_faces_file = known_faces_dir + '/known-faces.csv'
        # Chargement des visages connus
        self.__load_known_faces()

    def __load_known_faces(self):
        # Charge, l'ID, le nom des visages connus et leur encodage
        self.known_faces_ids = []
        self.known_faces_names = []
        self.known_faces_encodings = []

        # Recherche du fichier des encodages des visages connus
        if os.path.exists(self.known_faces_file) and os.path.isfile(self.known_faces_file):
            df_known_names_encodings = pd.read_csv(self.known_faces_file, header=0)
            if not df_known_names_encodings.empty:
                self.known_faces_ids = df_known_names_encodings['id'].tolist()
                self.known_faces_names = df_known_names_encodings['name'].tolist()
                for i in range((df_known_names_encodings.shape[0])):
                    self.known_faces_encodings.append(list(df_known_names_encodings.iloc[i, 2:]))
        else:
            self.createKnownFacesFile()
            # Le fichier n'existe pas : récupération à partir des images + sauvegarde
            for file in utils.image_files_in_folder(self.known_faces_dir):
                self.addFaceFromFile(file)

    def createKnownFacesFile(self):
        header = ['id', 'name']
        for i in range(128):
            header.append(f"e{i}")

        # Création du fichier CSV vide avec l'en-tête
        with open(self.known_faces_file, 'w', newline='') as file:
            writer = csv.writer(file)
            # Écriture de l'en-tête
            writer.writerow(header)

    def addFaceFromFile(self, file):
        basename = os.path.splitext(os.path.basename(file))[0]
        id_name = basename.split('_')
        img = face_recognition.load_image_file(file)
        encodings = face_recognition.face_encodings(img)
        if len(encodings) > 1:
            logging.warning("More than one face found in {}. Only considering the first face.".format(file))
        if len(encodings) == 0:
            logging.warning("WARNING: No faces found in {}. Ignoring file.".format(file))
        else:
            # Mise à jour des tableaux en mémoire
            self.known_faces_ids.append(id_name[0])
            self.known_faces_names.append(id_name[1])
            self.known_faces_encodings.append(encodings[0])

            # Mise à jour du fichier CSV
            with open(self.known_faces_file, mode='a', newline='') as file:
                writer = csv.writer(file)
                # Créer une ligne de données
                data_row = [id_name[0], id_name[1]] + list(encodings[0])
                writer.writerow(data_row)


    def addFaceFromByteAndName(self, fileContent, faceName):
        # Recherche du plus grand ID dans le fichier
        next_id = self.findNextId()

        faceFileName = next_id + '_' + faceName + '.jpg'
        # Chemin complet du fichier
        faceFilePath = os.path.join(self.known_faces_dir, faceFileName)

        # Créer le fichier et y écrire le contenu
        with open(faceFilePath, 'wb') as faceFile:
            faceFile.write(fileContent.getbuffer())

        # Ajout du fichier dans les visages connus
        self.addFaceFromFile(faceFilePath)

        return next_id, faceName


    def findNextId(self):
        # Lire le fichier CSV (remplacez 'fichier.csv' par le nom de votre fichier)
        df = pd.read_csv(self.known_faces_file)
        # Vérifier si le DataFrame est vide
        if df.empty:
            return "0001"
        else:
            # Convertir la colonne "id" en nombres entiers
            df['id'] = df['id'].astype(int)
            # Trouver l'ID maximum
            max_id = df['id'].max()
            # Calculer l'ID suivant
            next_id = max_id + 1
            # Formater l'ID suivant pour qu'il ait 4 caractères avec des zéros
            return str(next_id).zfill(4)

    def __detect_faces(self, image_file, recognition=False):
        # Initialize some variables
        face_names = []

        # Load the uploaded image file
        img = face_recognition.load_image_file(image_file)
        small_img = cv2.resize(img, (0, 0), fx=0.25, fy=0.25)

        # Find all the faces and face encodings in the current frame of video
        face_boxes = face_recognition.face_locations(small_img, model="cnn")
        face_landmarks_list = face_recognition.face_landmarks(small_img, face_boxes)

        if recognition:
            face_encodings = face_recognition.face_encodings(small_img, face_boxes)

            for face_encoding in face_encodings:

                name = ""

                if len(self.known_faces_encodings) > 0:
                    # Or instead, use the known face with the smallest distance to the new face
                    face_distances = face_recognition.face_distance(self.known_faces_encodings, face_encoding)
                    best_match_index = np.argmin(face_distances)

                    if face_distances[best_match_index] < 0.6:
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
