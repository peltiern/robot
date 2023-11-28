from pathlib import Path
from jetson_inference import detectNet
import jetson_utils
import numpy as np
import io
from PIL import Image


class JetsonObjectDetector:

    def __init__(self):
        self.__load_detector()

    def detect_objects(self, image_data):
        
        # Convert databytes to PIL image
        image = Image.open(io.BytesIO(image_data))
        # Convert PIL image to NumPy array
        array = np.array(image)
        # Convert Numpy array to CUDA image
        img_cuda = jetson_utils.cudaFromNumpy(array, isBGR=False)

        # Detection
        return self.net.Detect(img_cuda, width=640, height=480, overlay="box,labels")


    def getClassName(self, idxClass):
        return self.classesNames[idxClass] 
         

    # Load detector
    def __load_detector(self):
        # load the object detection network
        self.net = detectNet("ssd-mobilenet-v2", 0.5)

        base_path = Path(__file__).parent
        name_path = base_path / "cfg/ssd_coco_labels_fr.txt"
        self.classesNames = []
        with open(name_path, 'r', encoding='utf-8') as f:
            self.classesNames = [line.strip() for line in f.readlines()]
