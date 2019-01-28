package org.ws4d.coap.client;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.server.ServerCloneException;
import java.util.Random;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.ws4d.coap.Constants;
import org.ws4d.coap.connection.BasicCoapChannelManager;
import org.ws4d.coap.connection.BasicCoapClientChannel;
import org.ws4d.coap.interfaces.CoapChannelManager;
import org.ws4d.coap.interfaces.CoapClient;
import org.ws4d.coap.interfaces.CoapClientChannel;
import org.ws4d.coap.interfaces.CoapRequest;
import org.ws4d.coap.interfaces.CoapResponse;
import org.ws4d.coap.messages.CoapBlockOption;
import org.ws4d.coap.messages.CoapBlockOption.CoapBlockSize;
import org.ws4d.coap.messages.CoapMediaType;
import org.ws4d.coap.messages.CoapRequestCode;

/**
 * @author	Christian Lerche <christian.lerche@uni-rostock.de>
 * 			Bjoern Konieczek <bjoern.konieczek@uni-rostock.de>
 */
public class BasicCoapClient extends JFrame implements CoapClient, ActionListener
{
	private String SERVER_ADDRESS;
	private int PORT; 

	static int counter = 0;
	private CoapChannelManager channelManager = null;
	private BasicCoapClientChannel clientChannel = null;
	private Random tokenGen = null;


	//UI
	private JTextField uriField;
	private JTextField payloadField;
	private JButton postBtn, getBtn;
	private JTextArea area;
	
	
	public BasicCoapClient(String server_addr, int port ){
		super();
		this.SERVER_ADDRESS = server_addr;
		this.PORT = port;
		this.channelManager = BasicCoapChannelManager.getInstance();
		this.tokenGen = new Random();
	}
	public boolean connect(){
		try {
			clientChannel = (BasicCoapClientChannel) channelManager.connect(this, InetAddress.getByName(SERVER_ADDRESS), PORT);
		} catch( UnknownHostException e ){
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public boolean connect( String server_addr, int port ){
		this.SERVER_ADDRESS = server_addr;
		this.PORT = port;
		return this.connect();
	}

	public CoapRequest createRequest( boolean reliable, CoapRequestCode reqCode ) {
		return clientChannel.createRequest( reliable, reqCode );
	}

	public byte[] generateRequestToken(int tokenLength ){
		byte[] token = new byte[tokenLength];
		tokenGen.nextBytes(token);
		return token;
	}

	@Override
	public void onConnectionFailed(CoapClientChannel channel, boolean notReachable, boolean resetByServer) {
		System.out.println("Connection Failed");
	}
	
	@Override
	public void onResponse(CoapClientChannel channel, CoapResponse response) {
		System.out.println("Received response");
		if(response.getPayload() != null)
		{
			String responseData = new String(response.getPayload()); 
			System.out.println(responseData);
			area.append("Response:\n");
			if(responseData.matches(".*well-known/core.*"))
			{
				String[] tempData = responseData.split(",");
				for(String data : tempData)
				{
					if(!data.equals("</.well-known/core>"))
					{
						area.append(data+"\n");
					}
				}
			}else
			{
				area.append(responseData+"\n");
			}
			area.append("--------------------------------\n");

		}else
		{
			System.out.println("response payload null");
			area.append("Response:\n");
			area.append("response payload null\n");
			area.append("--------------------------------\n");
		}
	}
		
	@Override
	public void onMCResponse(CoapClientChannel channel, CoapResponse response, InetAddress srcAddress, int srcPort) {
		System.out.println("MCReceived response");
	}

	
	public void observeExample()
	{
		try {
			clientChannel = (BasicCoapClientChannel) channelManager.connect(this, InetAddress.getByName(SERVER_ADDRESS), PORT);
			CoapRequest coapRequest = clientChannel.createRequest(true, CoapRequestCode.GET);
			byte [] token = generateRequestToken(3);
			coapRequest.setUriPath("/test/temp");
			coapRequest.setToken(token);
			coapRequest.setObserveOption(1);
			clientChannel.sendMessage(coapRequest);
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
	}

	//client Ui
	public void clientUi()
	{
		setTitle("CoAP Client");
		setSize(800,380);
		setLocation(400,300);
		setLayout(null);
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		postBtn = new JButton("라면 시작");
		postBtn.setBounds(100, 100, 100, 100);
		postBtn.setFont(new Font("Serif", Font.PLAIN, 15));
		postBtn.addActionListener(this);
		add(postBtn);
		
		area = new JTextArea();
		area.setFont(new Font("Serif", Font.PLAIN, 15));
		JScrollPane sc = new JScrollPane(area);
		sc.setBounds(270, 20, 500, 300);
		add(sc);
		

		setVisible(true);
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
	
		if(e.getSource() == postBtn)
		{
			String uriPath= "/test/temp";
			String payload = "start";
			
			area.append("Request:\n");
			area.append("Method: POST\n");
			area.append("Uri path: "+uriPath+"\n");
			area.append("Payload: "+payload+"\n");
			area.append("--------------------------------\n");
			try {
				clientChannel = (BasicCoapClientChannel) channelManager.connect(this, InetAddress.getByName(SERVER_ADDRESS), PORT);
				CoapRequest coapRequest = clientChannel.createRequest(true, CoapRequestCode.POST);
				byte [] token = generateRequestToken(3);
				coapRequest.setUriPath(uriPath);
				coapRequest.setPayload(payload.getBytes());
				coapRequest.setToken(token);
				clientChannel.sendMessage(coapRequest);

			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

	}

	public static void main(String[] args)
	{
		System.out.println("Start CoAP Client");
		String serverIp = "raspberrypi.mshome.net";
		BasicCoapClient client = new BasicCoapClient(serverIp, Constants.COAP_DEFAULT_PORT);
		client.channelManager = BasicCoapChannelManager.getInstance();
		
		//UI
		client.clientUi();
		client.observeExample();
	}



}
