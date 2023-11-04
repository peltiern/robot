from pathlib import Path
from jetson_inference import detectNet
from jetson_utils import loadImage
import tempfile



class JetsonObjectDetector:

    def __init__(self):
        self.__load_detector()

    def detect_objects(self, image_data):
        # Enregistrez le tableau de bytes dans un fichier temporaire
        # Specify a different directory for the temporary file
        temp_file_path = "/jetson-inference/temp_image.jpg"

        # Save the bytes to the temporary file
        with open(temp_file_path, "wb") as temp_file:
            temp_file.write(image_data.read())


        # Chargez l'image à partir du fichier temporaire
        img_cuda = loadImage(temp_file_path)

        return self.net.Detect(img_cuda, overlay="box,labels")

    def getClassName(self, idxClass):
        return self.net.GetClassLabel(idxClass) 
         

    # Load detector
    def __load_detector(self):
        # load the object detection network
        self.net = detectNet("ssd-mobilenet-v2", 0.5)

        #base_path = Path(__file__).parent
        #name_path = base_path / "cfg/coco_fr.names"
        #self.classes = []
        #with open(name_path) as f:
        #    self.classes = [line.strip() for line in f.readlines()]
