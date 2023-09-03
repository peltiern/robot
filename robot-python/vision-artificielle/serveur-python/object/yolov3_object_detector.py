from pathlib import Path

import cv2
import numpy as np
from utils.utils import load_image_file


class YoloV3ObjectDetector:

    def __init__(self):
        self.__load_yolo()

    def detect_objects(self, image_file):
        image, height, width, channels = self.__load_image(image_file)
        blob, outputs = self.__detect_objects(image)
        object_locations, object_names, object_scores = self.__get_bounding_boxes(outputs, height, width)
        return object_locations, object_names, object_scores

    # Load yolo
    def __load_yolo(self):
        base_path = Path(__file__).parent
        weights_path = base_path / "cfg/yolov3-tiny.weights"
        config_path = base_path / "cfg/yolov3-tiny.cfg"
        name_path = base_path / "cfg/coco_fr.names"
        self.net = cv2.dnn.readNet(str(weights_path), str(config_path))
        self.classes = []
        with open(name_path) as f:
            self.classes = [line.strip() for line in f.readlines()]

        self.layers_names = self.net.getLayerNames()
        self.output_layers = [self.layers_names[i[0] - 1] for i in self.net.getUnconnectedOutLayers()]

    def __load_image(self, image_file):
        # image loading
        img = load_image_file(image_file)
        img = cv2.resize(img, None, fx=0.5, fy=0.5)
        height, width, channels = img.shape
        return img, height, width, channels

    def __detect_objects(self, img):
        blob = cv2.dnn.blobFromImage(img, scalefactor=0.00392, size=(320, 320), mean=(0, 0, 0), swapRB=True, crop=False)
        self.net.setInput(blob)
        outputs = self.net.forward(self.output_layers)
        return blob, outputs

    def __get_bounding_boxes(self, outputs, height, width):
        object_boxes = []
        object_scores = []
        object_names = []
        for output in outputs:
            for detect in output:
                scores = detect[5:]
                class_id = np.argmax(scores)
                conf = scores[class_id]
                if conf > 0.3:
                    center_x = int(detect[0] * width)
                    center_y = int(detect[1] * height)
                    w = int(detect[2] * width)
                    h = int(detect[3] * height)
                    x = int(center_x - w / 2)
                    y = int(center_y - h / 2)
                    object_boxes.append([x * 2, y * 2, w * 2, h * 2])
                    object_scores.append(float(conf))
                    object_names.append(self.classes[int(class_id)])

        # Suppression des box superposées
        indexes = cv2.dnn.NMSBoxes(object_boxes, object_scores, 0.5, 0.4)
        object_boxes = [object_boxes[i] for i, e in enumerate(object_boxes) if i not in indexes]
        object_scores = [object_scores[i] for i, e in enumerate(object_scores) if i not in indexes]
        object_names = [object_names[i] for i, e in enumerate(object_names) if i not in indexes]

        return object_boxes, object_names, object_scores
