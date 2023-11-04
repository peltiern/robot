from pathlib import Path
from jetson_inference import detectNet


class JetsonObjectDetector:

    def __init__(self):
        self.__load_detector()

    def detect_objects(self, image_file):
        return self.net.Detect(image_file, overlay="box,labels")

    # Load detector
    def __load_detector(self):
        # load the object detection network
        self.net = detectNet("ssd-mobilenet-v2", 0.5)

        base_path = Path(__file__).parent
        name_path = base_path / "cfg/coco_fr.names"
        self.classes = []
        with open(name_path) as f:
            self.classes = [line.strip() for line in f.readlines()]
