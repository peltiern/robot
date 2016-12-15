package fr.roboteek.robot.test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;

import com.yoctopuce.YoctoAPI.YAPI;
import com.yoctopuce.YoctoAPI.YAPI_Exception;
import com.yoctopuce.YoctoAPI.YDisplay;
import com.yoctopuce.YoctoAPI.YDisplayLayer;

public class YoctopuceMaxiDisplayTest {

	 public static void main(String[] args)
	    {
	        try {
	            // setup the API to use local VirtualHub
	            YAPI.RegisterHub("127.0.0.1");
	        } catch (YAPI_Exception ex) {
	            System.out.println("Cannot contact VirtualHub on 127.0.0.1 (" + ex.getLocalizedMessage() + ")");
	            System.out.println("Ensure that the VirtualHub application is running");
	            System.exit(1);
	        }
	        
	        try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	        YDisplay disp;
	        YDisplayLayer l0;
			disp = YDisplay.FirstDisplay();
	        /*if (args.length == 0) {
	            disp = YDisplay.FirstDisplay();
	            if (disp == null) {
	                System.out.println("No module connected (check USB cable)");
	                System.exit(1);
	            }
	        } else {
	            disp = YDisplay.FindDisplay(args[0] + ".display");
	        }*/
	        try {
	            disp.resetAll();
	            // retreive the display size
	            int w = disp.get_displayWidth();
	            int h = disp.get_displayHeight();

	            // reteive the first layer
	            l0 = disp.get_displayLayer(0);
	            
	            final String image00 = "yeux_00.gif";
	            final String image01 = "yeux_01.gif";
	            final String image02 = "yeux_02.gif";
	            final String image03 = "yeux_03.gif";
	            final String image04 = "yeux_04.gif";
	            final String image05 = "yeux_05.gif";
	            final String image06 = "yeux_06.gif";
	            final String image07 = "yeux_07.gif";
	            final String image08 = "yeux_08.gif";
	            final String image09 = "yeux_09.gif";
	            final String image10 = "yeux_10.gif";
	            final String image11 = "yeux_11.gif";
	            final String image12 = "yeux_12.gif";
	            
	            final String image20 = "yeux_20.gif";
	            final String image21 = "yeux_21.gif";
	            final String image22 = "yeux_22.gif";
	            final String image23 = "yeux_23.gif";
	            final String image24 = "yeux_24.gif";
	            
	            final String image30 = "yeux_30.gif";
	            final String image31 = "yeux_31.gif";
	            
	            final String image40 = "yeux_40.gif";
	            final String image41 = "yeux_41.gif";
	            
	            final int[] listeRandom =  {150,50,100,200,150,500,100,500,300,200};
	            
//	            disp.newSequence();
	            for (int i = 0; i < 10; i++) {
	            	l0.drawImage(0, 0, image01);
	            	l0.drawImage(0, 0, image03);
	            	l0.drawImage(0, 0, image05);
	            	l0.drawImage(0, 0, image00);
	            	l0.drawImage(0, 0, image05);
	            	l0.drawImage(0, 0, image03);
	            	l0.drawImage(0, 0, image01);
//	            	disp.pauseSequence(listeRandom[i]); 
	            	try {
						Thread.sleep(listeRandom[i]);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	            }
//	            disp.saveSequence("test.seq");
//	            disp.playSequence("test.seq");
	            
	            final byte[] tabImage00 = getDataFromImage("C:/Users/Nicolas/Documents/Robot/images/yeux/SVG/yeux_00.bmp");
	            final byte[] tabImage01 = getDataFromImage("C:/Users/Nicolas/Documents/Robot/images/yeux/SVG/yeux_01.bmp");
//	            final byte[] image02 = getDataFromImage("C:/Users/Nicolas/Documents/Robot/images/yeux/SVG/yeux_02.bmp");
	            final byte[] tabImage03 = getDataFromImage("C:/Users/Nicolas/Documents/Robot/images/yeux/SVG/yeux_03.bmp");
//	            final byte[] image04 = getDataFromImage("C:/Users/Nicolas/Documents/Robot/images/yeux/SVG/yeux_04.bmp");
	            final byte[] tabImage05 = getDataFromImage("C:/Users/Nicolas/Documents/Robot/images/yeux/SVG/yeux_05.bmp");
	            final byte[] tabImage06 = getDataFromImage("C:/Users/Nicolas/Documents/Robot/images/yeux/SVG/yeux_06.bmp");
	            final byte[] tabImage07 = getDataFromImage("C:/Users/Nicolas/Documents/Robot/images/yeux/SVG/yeux_07.bmp");
	            final byte[] tabImage08 = getDataFromImage("C:/Users/Nicolas/Documents/Robot/images/yeux/SVG/yeux_08.bmp");
	            final byte[] tabImage09 = getDataFromImage("C:/Users/Nicolas/Documents/Robot/images/yeux/SVG/yeux_09.bmp");
	            final byte[] tabImage10 = getDataFromImage("C:/Users/Nicolas/Documents/Robot/images/yeux/SVG/yeux_10.bmp");
	            final byte[] tabImage11 = getDataFromImage("C:/Users/Nicolas/Documents/Robot/images/yeux/SVG/yeux_11.bmp");
	            final byte[] tabImage12 = getDataFromImage("C:/Users/Nicolas/Documents/Robot/images/yeux/SVG/yeux_12.bmp");
	            
//	            final byte[] image20 = getDataFromImage("C:/Users/Nicolas/Documents/Robot/images/yeux/SVG/yeux_20.bmp");
//	            final byte[] image21 = getDataFromImage("C:/Users/Nicolas/Documents/Robot/images/yeux/SVG/yeux_21.bmp");
//	            final byte[] image22 = getDataFromImage("C:/Users/Nicolas/Documents/Robot/images/yeux/SVG/yeux_22.bmp");
//	            final byte[] image23 = getDataFromImage("C:/Users/Nicolas/Documents/Robot/images/yeux/SVG/yeux_23.bmp");
//	            final byte[] image24 = getDataFromImage("C:/Users/Nicolas/Documents/Robot/images/yeux/SVG/yeux_24.bmp");
	            
	            final byte[] tabImage30 = getDataFromImage("C:/Users/Nicolas/Documents/Robot/images/yeux/SVG/yeux_30.bmp");
	            final byte[] tabImage31 = getDataFromImage("C:/Users/Nicolas/Documents/Robot/images/yeux/SVG/yeux_31.bmp");
	            
	            final byte[] tabImage40 = getDataFromImage("C:/Users/Nicolas/Documents/Robot/images/yeux/SVG/yeux_40.bmp");
	            final byte[] tabImage41 = getDataFromImage("C:/Users/Nicolas/Documents/Robot/images/yeux/SVG/yeux_41.bmp");
	            
//	            // display a text in the middle of the screen
//	            l0.setLayerPosition(0, h, 0);
//	            l0.selectFont("Medium.yfm");
//	            l0.drawText(w / 2, h / 2, YDisplayLayer.ALIGN.BOTTOM_CENTER, "COUCOU");
//	            l0.drawText(w / 2, h / 2 + 25, YDisplayLayer.ALIGN.BOTTOM_CENTER, "MISS G. !!!");
//	            l0.setLayerPosition(0, -h - 25, 3000);
//	           
//	            try {
//					Thread.sleep(3000);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//	            
//	            l0.clear();
//	            l0.setLayerPosition(0, 0, 0);
	            
	            
	            disp.resetAll();
//	            disp.newSequence();
	            for (int i = 0; i < 10; i++) {
//	            	System.out.println(System.currentTimeMillis());
	            	l0.drawBitmap(0, 0, w, tabImage01, 0);
//	            	System.out.println(System.currentTimeMillis());
//	            	l0.drawBitmap(0, 0, w, image02, 0);
	            	l0.drawBitmap(0, 0, w, tabImage03, 0);
	            	//l0.drawBitmap(0, 0, w, image04, 0);
	            	l0.drawBitmap(0, 0, w, tabImage05, 0);
	            	//l0.drawBitmap(0, 0, w, image06, 0);
	            	l0.drawBitmap(0, 0, w, tabImage00, 0);
	            	//l0.drawBitmap(0, 0, w, image00, 0);
	            	l0.drawBitmap(0, 0, w, tabImage05, 0);
	            	//l0.drawBitmap(0, 0, w, image04, 0);
	            	l0.drawBitmap(0, 0, w, tabImage03, 0);
	            	//l0.drawBitmap(0, 0, w, image02, 0);
	            	l0.drawBitmap(0, 0, w, tabImage01, 0);
//	            	disp.pauseSequence(listeRandom[i]); 
	            	try {
						Thread.sleep(listeRandom[i]);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	            }
//	            System.out.println("Coucou");
//	            disp.saveSequence("test.seq");
//	            disp.playSequence("test.seq");
//	            
	            try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	           
	            for (int i = 0; i < 5; i++) {
	            	l0.drawBitmap(0, 0, w, tabImage07, 0);
	            	l0.drawBitmap(0, 0, w, tabImage08, 0);
	            	l0.drawBitmap(0, 0, w, tabImage09, 0);
	            	l0.drawBitmap(0, 0, w, tabImage08, 0);
	            	l0.drawBitmap(0, 0, w, tabImage07, 0);
	            	l0.drawBitmap(0, 0, w, tabImage01, 0);
	            	l0.drawBitmap(0, 0, w, tabImage10, 0);
	            	l0.drawBitmap(0, 0, w, tabImage11, 0);
	            	l0.drawBitmap(0, 0, w, tabImage12, 0);
	            	l0.drawBitmap(0, 0, w, tabImage11, 0);
	            	l0.drawBitmap(0, 0, w, tabImage10, 0);
	            	l0.drawBitmap(0, 0, w, tabImage01, 0);
	            }
//	            
//	            
//	            final int[] listeRandom2 = {200,100,300,300,200,300,600,1000,100,200};
//	            for (int i = 0; i < 5; i++) {
//	            	l0.drawBitmap(0, 0, w, image20, 0);
//	            	l0.drawBitmap(0, 0, w, image22, 0);
//	            	try {
//						Thread.sleep(listeRandom2[2 * i]);
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//	            	l0.drawBitmap(0, 0, w, image20, 0);
//	            	l0.drawBitmap(0, 0, w, image24, 0);
//	            	try {
//						Thread.sleep(listeRandom2[2 * i + 1]);
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//	            }
//	            
//	            l0.drawBitmap(0, 0, w, image01, 0);
//	            
//	            try {
//					Thread.sleep(500);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//	            
	            l0.drawBitmap(0, 0, w, tabImage30, 0);
	            l0.drawBitmap(0, 0, w, tabImage31, 0);
//	            
	            try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
//	            
	            l0.drawBitmap(0, 0, w, tabImage30, 0);
	            l0.drawBitmap(0, 0, w, tabImage01, 0);
//	            
	            try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	            
	            l0.drawBitmap(0, 0, w, tabImage40, 0);
	            l0.drawBitmap(0, 0, w, tabImage41, 0);
	            l0.drawBitmap(0, 0, w, tabImage40, 0);
	            l0.drawBitmap(0, 0, w, tabImage01, 0);
//	            
	            try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
////	            
////	            for (int i = 0; i < 5; i++) {
////	            	l0.drawBitmap(0, 0, w, image20, 0);
////	            	l0.drawBitmap(0, 0, w, image21, 0);
////	            	l0.drawBitmap(0, 0, w, image22, 0);
////	            	l0.drawBitmap(0, 0, w, image21, 0);
////	            	l0.drawBitmap(0, 0, w, image20, 0);
////	            	l0.drawBitmap(0, 0, w, image23, 0);
////	            	l0.drawBitmap(0, 0, w, image24, 0);
////	            	l0.drawBitmap(0, 0, w, image23, 0);
////	            }
	            
	            disp.resetAll();
	           
	        } catch (YAPI_Exception ex) {
	            System.out.println("Exception:" + ex.getLocalizedMessage() + "\n" + ex.getStackTraceToString());
	            System.out.println("Module not connected (check identification and USB cable)");
	        }

	        
	        YAPI.FreeAPI();
	    }
	 
	 
	 public static byte[] getDataFromImage(String cheminImage) {
		 try {
			 final int largeur = 128;
			 final int hauteur = 64;
			 final int bytesPerLines = largeur / 8;
			 
			final FImage image = ImageUtilities.readF(new File(cheminImage));
			byte[] data = new byte[hauteur * bytesPerLines];
			String valByte = "";
			if (image != null && image.getWidth() <= largeur && image.getHeight() <= hauteur) {
				for (int j = 0; j < hauteur; j++) {
					for (int i = 0; i < largeur; i++) {
						// Si pixel noir ==> 0
						if (image.getPixelNative(i, j) == 0) {
							valByte += "1";
						} else {
							valByte += "0";
						}
						
						// On ajoute l'octet dans le tableau pour le dernier bit
						if (i % 8 == 7) {
							data[j * bytesPerLines + ((i + 1) - 8) / 8] = (byte)Integer.parseInt(valByte, 2);
							valByte = "";
						}
					}
				}
			}
			return data;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	 }
}
