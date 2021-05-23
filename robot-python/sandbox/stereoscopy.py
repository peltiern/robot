import cv2

imgL = cv2.imread('left.png',0)
imgR = cv2.imread('right.png',0)

cv2.namedWindow("Image")
cv2.moveWindow("Image", 50, 100)
cv2.namedWindow("left")
cv2.moveWindow("left", 450, 100)
cv2.namedWindow("right")
cv2.moveWindow("right", 850, 100)

cv2.imshow("left", imgL)
cv2.imshow("right", imgR)

stereo = cv2.StereoBM_create(numDisparities=16, blockSize=15)
disparity = stereo.compute(imgL,imgR)
local_max = disparity.max()
local_min = disparity.min()
disparity_grayscale = (disparity-local_min)*(65535.0/(local_max-local_min))
disparity_fixtype = cv2.convertScaleAbs(disparity_grayscale, alpha=(255.0/65535.0))
disparity_color = cv2.applyColorMap(disparity_fixtype, cv2.COLORMAP_JET)
cv2.imshow("Image", disparity_color)

while(True) :
    key = cv2.waitKey(1) & 0xFF
    if key == ord("q"):
        quit()

