package org.ws4d.coap.server;

import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.ws4d.coap.Constants;
import org.ws4d.coap.client.BasicCoapClient;
import org.ws4d.coap.connection.BasicCoapChannelManager;
import org.ws4d.coap.connection.BasicCoapClientChannel;
import org.ws4d.coap.connection.BasicCoapServerChannel;
import org.ws4d.coap.connection.BasicCoapSocketHandler;
import org.ws4d.coap.interfaces.CoapChannel;
import org.ws4d.coap.interfaces.CoapChannelManager;
import org.ws4d.coap.interfaces.CoapClient;
import org.ws4d.coap.interfaces.CoapClientChannel;
import org.ws4d.coap.interfaces.CoapRequest;
import org.ws4d.coap.interfaces.CoapResponse;
import org.ws4d.coap.interfaces.CoapServer;
import org.ws4d.coap.interfaces.CoapServerChannel;
import org.ws4d.coap.messages.BasicCoapResponse;
import org.ws4d.coap.messages.CoapMediaType;
import org.ws4d.coap.messages.CoapRequestCode;
import org.ws4d.coap.messages.CoapResponseCode;
import org.ws4d.coap.rest.BasicCoapResource;
import org.ws4d.coap.rest.CoapResourceServer;
import org.ws4d.coap.rest.ResourceHandler;

import com.pi4j.io.gpio.*;

import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Random;

import javax.swing.JFrame;

import com.pi4j.io.spi.SpiChannel;
import com.pi4j.io.spi.SpiDevice;
import com.pi4j.io.spi.SpiFactory;
import com.pi4j.wiringpi.*;
import java.util.*;
/**
 * @author Christian Lerche <christian.lerche@uni-rostock.de>
 * 
 */

public class CoapSampleResourceServer
{
	
	private static CoapSampleResourceServer sampleServer;
	private static CoapResourceServer resourceServer;

	private static Logger logger = Logger
			.getLogger(CoapSampleResourceServer.class.getName());
	
	public static SpiDevice spi = null;
	private int tempval = 0;
	private int inttemp = 0;
	private PinState waterval;
	

	// ADC channel count
	private static short ADC_CHANNEL_COUNT = 4;  // MCP3204 = 4
	
	//Steinhart Parameters
	private static String a0 = "1.131786e-003";
	private static String a1 = "2.336422e-004";
	private static String a3 = "8.985024e-008";
	private static String T0 = "273.15";
	

	private static int read() throws IOException, InterruptedException {
		for(short channel = 0; channel < ADC_CHANNEL_COUNT; channel++){
			if(channel == 0){
				int conversion_value = getConversionValue(channel);
				//System.out.println("adc value: "+conversion_value);
				Thread.sleep(250);
				return conversion_value;
			}
		}
		Thread.sleep(250);
		return 0;
	}

	private static int getConversionValue(short channel) throws IOException {

		// create a data buffer and initialize a conversion request payload
		byte data[] = new byte[] {
				(byte) 0b00000001,                              
				// first byte, start bit
				(byte)(0b10000000 |( ((channel & 7) << 4))),    
				// second byte transmitted -> (SGL/DIF = 1, D2=D1=D0=0)
				(byte) 0b00000000                               
				// third byte transmitted....don't care
		};

		// send conversion request to ADC chip via SPI channel
		byte[] result = spi.write(data);

		// calculate and return conversion value from result bytes
		int value = (result[1]<< 8) & 0b1100000000; //merge data[1] & data[2] to get 10-bit result
		value |=  (result[2] & 0xff);
		return value;
	}
	
	private void getTemp() throws InterruptedException, IOException {
		// create SPI object instance for SPI for communication
		spi = SpiFactory.getInstance(SpiChannel.CS0,
				SpiDevice.DEFAULT_SPI_SPEED, // default spi speed 1 MHz
				SpiDevice.DEFAULT_SPI_MODE); // default spi mode 0
					
			double u = read()/1023.0;
			double R = (1/u-1)*10000;
			double InR = Math.log(R);
			double temp = Double.parseDouble(a0)+Double.parseDouble(a1)*InR
					+Double.parseDouble(a3)*Math.pow(InR, 3);
			double inv = 1/temp;
			double temper = inv - Double.parseDouble(T0);
			inttemp = (int)temper;
			
		
	}
	
	
	private void getWaterLevel() throws InterruptedException, IOException {
		waterval=waterPin.getState();
	}
		
	//GPIO
	
	private static boolean gpiostate = false;
	
	private static GpioController gpio ;
	private static GpioPinDigitalOutput RedLedControlPin;
	private static GpioPinDigitalOutput GreenLedControlPin;
	private static GpioPinDigitalOutput BlueLedControlPin;
	
	//================
	private static GpioPinDigitalInput waterPin;
	private static Timer m_timer;
	private int count=0;
	private int ServerCount=0;
	TimerTask m_task;
	//================
	
	public static void gpioinit() throws InterruptedException{
		
		gpio = GpioFactory.getInstance();
		
		Gpio.wiringPiSetup();
		SoftPwm.softPwmCreate(15, 0, 100);
		SoftPwm.softPwmWrite(15, 15);
		
		waterPin = gpio.provisionDigitalInputPin(RaspiBcmPin.GPIO_25);
		RedLedControlPin = gpio.provisionDigitalOutputPin(RaspiBcmPin.GPIO_27, PinState.LOW);
		GreenLedControlPin = gpio.provisionDigitalOutputPin(RaspiBcmPin.GPIO_28, PinState.LOW);
		BlueLedControlPin = gpio.provisionDigitalOutputPin(RaspiBcmPin.GPIO_29, PinState.LOW);
		
		RedLedControlPin.setShutdownOptions(true,PinState.LOW);
		GreenLedControlPin.setShutdownOptions(true,PinState.LOW);
		BlueLedControlPin.setShutdownOptions(true,PinState.LOW);
		RedLedControlPin.low();
		GreenLedControlPin.low();
		BlueLedControlPin.low();
		
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
				
		gpiostate = true;
		logger.addAppender(new ConsoleAppender(new SimpleLayout()));
		logger.setLevel(Level.INFO);
		logger.info("Start Sample Resource Server");			

		sampleServer = new CoapSampleResourceServer();	
		
		if(gpiostate == true) {
			try {
				gpioinit();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			gpiostate = false;
		}
		sampleServer.run();
		
	}
	
	private void run() throws IOException 
	{
		
		if (resourceServer != null)
			resourceServer.stop();
		resourceServer = new CoapResourceServer();
		
		/* Show detailed logging of Resource Server*/
		Logger resourceLogger = Logger.getLogger(CoapResourceServer.class.getName());
		resourceLogger.setLevel(Level.ALL);

		/* add resources */	
		final BasicCoapResource temp = new BasicCoapResource("/test/temp","Temp".getBytes(),
				CoapMediaType.text_plain);
		
		
		temp.registerResourceHandler(new ResourceHandler() {
			@Override
			public void onPost(byte[] data) {
				System.out.println("라면을 시작합니다.");	
				String inputData = new String (data);
				if(inputData.equals("start")) {			
					//=================
					m_timer=new Timer();
					TimerTask m_task = new TimerTask() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							System.out.println("라면이 완성 되었습니다.");
							BlueLedControlPin.high();
							SoftPwm.softPwmWrite(15, 15);
						}
					};
					
			
					int counter = 0;
					SoftPwm.softPwmWrite(15, 24);
					
					while(true){
						try {
							Thread.sleep(1000);
							ServerCount++;
						}catch (@SuppressWarnings("unused") InterruptedException e) {
							// do nothing
						}
						try {
							try {
								getTemp();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							try {
								getWaterLevel();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						if(tempval != inttemp){
							counter++;
							System.out.println("온도 chenged to : "+ inttemp);
							tempval = inttemp;	
							System.out.println("수위 :"+waterval);
							if(waterPin.isLow()) {
								RedLedControlPin.high();
							}
							
							if(inttemp>=30) {
								count++;
								if(count==1) {
									m_timer.schedule(m_task, 5000);
								}
								
								System.out.println("물이 끓습니다.");
								//RedLedControlPin.high();
								
								GreenLedControlPin.high();
								
								
							}
							temp.setValue(("Message #" + counter +"    Temp:  "+ inttemp).getBytes());
							temp.changed();		
						}
						
						if(ServerCount==0) {
							temp.setObservable(false);
							resourceServer.createResource(temp);
						try {
							resourceServer.start();
						} catch (Exception e) {
							e.printStackTrace();
						}
						}
						//====================
						
						
					}//while-end
					//=================
					
				}
				
			}			
		});		
		
		temp.setObservable(false);
		resourceServer.createResource(temp);
		
		try {
			resourceServer.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
}
