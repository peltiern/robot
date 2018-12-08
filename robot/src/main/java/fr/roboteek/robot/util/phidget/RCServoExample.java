package fr.roboteek.robot.util.phidget;

import com.phidget22.*;
import java.util.Scanner;

public class RCServoExample {
	
	static Scanner s = new Scanner(System.in);
	
    public static void main(String[] args) throws Exception {
		
		/***
        * Allocate a new Phidget Channel object
        ***/
        RCServo ch = new RCServo();

		/**
		* Displays info about the attached Phidget channel.
		* Fired when a Phidget channel with onAttachHandler registered attaches
		*/
        ch.addAttachListener(new AttachListener() {
			public void onAttach(AttachEvent ae) {
			
				try {
					//If you are unsure how to use more than one Phidget channel with this event, we recommend going to
					//www.phidgets.com/docs/Using_Multiple_Phidgets for information

					System.out.print("\nAttach Event:");
					
					RCServo ph = (RCServo) ae.getSource();
					
					/**
					* Get device information and display it.
					**/
					int serialNumber = ph.getDeviceSerialNumber();
					String channelClass = ph.getChannelClassName();
					int channel = ph.getChannel();
					
					DeviceClass deviceClass = ph.getDeviceClass();
					if (deviceClass != DeviceClass.VINT) {
						System.out.print("\n\t-> Channel Class: " + channelClass + "\n\t-> Serial Number: " + serialNumber +
							  "\n\t-> Channel:  " + channel + "\n");
					} 
					else {            
						int hubPort = ph.getHubPort();
						System.out.print("\n\t-> Channel Class: " + channelClass + "\n\t-> Serial Number: " + serialNumber +
							  "\n\t-> Hub Port: " + hubPort + "\n\t-> Channel:  " + channel + "\n");
					}
					
					/*
					* Set a TargetPosition inside of the attach handler to initialize the servo's starting position.
					* TargetPosition defines position the RC Servo will move to.
					* TargetPosition can be set to any value from MinPosition to MaxPosition.
					*/
					ph.setTargetPosition(90);
					
					/*
					* Engage the RCServo inside of the attach handler to allow the servo to move to its target position
					* The servo will only track a target position if it is engaged.
					* Engaged can be set to TRUE to enable the servo, or FALSE to disable it.
					*/
					ph.setEngaged(true);
				} 
				catch (PhidgetException e) {
					e.printStackTrace();
				}
			}	
        });

		
		/**
		* Displays info about the detached Phidget channel.
		* Fired when a Phidget channel with onDetachHandler registered detaches
		*/
        ch.addDetachListener(new DetachListener() {
			public void onDetach(DetachEvent de) {
				try {
					//If you are unsure how to use more than one Phidget channel with this event, we recommend going to
					//www.phidgets.com/docs/Using_Multiple_Phidgets for information

					System.out.print("\nAttach Event:");
					
					Phidget ph = de.getSource();
					
					/**
					* Get device information and display it.
					**/
					int serialNumber = ph.getDeviceSerialNumber();
					String channelClass = ph.getChannelClassName();
					int channel = ph.getChannel();
					
					DeviceClass deviceClass = ph.getDeviceClass();
					if (deviceClass != DeviceClass.VINT) {
						System.out.print("\n\t-> Channel Class: " + channelClass + "\n\t-> Serial Number: " + serialNumber +
							  "\n\t-> Channel:  " + channel + "\n");
					} 
					else {            
						int hubPort = ph.getHubPort();
						System.out.print("\n\t-> Channel Class: " + channelClass + "\n\t-> Serial Number: " + serialNumber +
							  "\n\t-> Hub Port: " + hubPort + "\n\t-> Channel:  " + channel + "\n");
					}
				} 
				catch (PhidgetException e) {
					e.printStackTrace();
				}
			}
        });

		/**
		* Writes Phidget error info to stderr.
		* Fired when a Phidget channel with onErrorHandler registered encounters an error in the library
		*/
        ch.addErrorListener(new ErrorListener() {
			public void onError(ErrorEvent ee) {
				System.out.println("Error: " + ee.getDescription());
			}
		});
		
		/**
		* Outputs the RCServo's most recently reported position change.
		* Fired when a RCServo channel with onTargetPositionReachedHandler registered reports a position change
		*/
		ch.addTargetPositionReachedListener(new RCServoTargetPositionReachedListener() {
			public void onTargetPositionReached(RCServoTargetPositionReachedEvent e) {
				//If you are unsure how to use more than one Phidget channel with this event, we recommend going to
				//www.phidgets.com/docs/Using_Multiple_Phidgets for information

				System.out.println("[Position Event] -> Position: " + e.getPosition());
			}
		});
		
        try {
			
			/***
			* Set matching parameters to specify which channel to open
			***/
			ch.setChannel(7);

			
			
			//This call may be harmlessly removed
			PrintEventDescriptions();
			
			/***
			* Open the channel with a timeout
			***/
			System.out.println("\nOpening and Waiting for Attachment...");
			
			try {
				ch.open(5000);
			}
			catch (PhidgetException e) {
				e.printStackTrace();
				throw new Exception ("Program Terminated: Open Failed", e);
			}
			
			/***
			* To find additional functionality not included in this example,
			* be sure to check the API for your device.
			***/
			
			System.out.print("--------------------\n" +
				"\n  | Motor position can be controlled by setting its Target Position.\n" +
				"  | The target position can be a number between MinPosition and MaxPosition.\n" +
				"  | For this example, acceleration and velocity limit are left as their default values, but can be changed in custom code.\n" +

				"\nInput a desired position between -1.0 and 1.0 and press ENTER\n" +
				"Input Q and press ENTER to quit\n");

			/*
			* To find additional functionality not included in this example,
			* be sure to check the API for your device.
			*/
			
			boolean end = false;
			while (!end) {

				//Get user input
				String buf = s.nextLine();

				if (buf.length() == 0)
					continue;
				
				//Process user input
				if (buf.charAt(0) == 'Q' || buf.charAt(0) == 'q') {
					end = true;
					continue;
				}

				double targetPosition;
				try {
					targetPosition = Double.parseDouble(buf);
				} catch (NumberFormatException e) {
					System.out.format("TargetPosition must be between %.2f and %.2f\n", ch.getMinPosition(), ch.getMaxPosition());
					continue;
				}
				
				if (targetPosition > ch.getMaxPosition() || targetPosition < ch.getMinPosition()) {
					System.out.format("TargetPosition must be between %.2f and %.2f\n", ch.getMinPosition(), ch.getMaxPosition());
					continue;
				}

				//Send the value to the device
				ch.setTargetPosition(targetPosition);
			}

			/***
			* Perform clean up and exit
			***/			
			System.out.println("\nDone Sampling...");

			System.out.println("Cleaning up...");
			ch.close();
			System.out.println("\nExiting...");
			return;

			
        } catch (PhidgetException ex) {
            System.out.println(ex.getDescription());
        }
    }
	
	/***
	* Prints descriptions of how events related to this class work
	***/
	public static void PrintEventDescriptions()	{
		System.out.print("\n--------------------\n" +
				"\n  | Position change events will call their associated function every time new position data is received from the device.\n" +
				"  | The rate of these events can be set by adjusting the DataInterval for the channel.\n" +
				"  | Press ENTER once you have read this message.\n");
		s.nextLine();
		
		System.out.println("\n--------------------");
	}
}
