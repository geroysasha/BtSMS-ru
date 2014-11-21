package com.example.btsms;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
/**
 * Интерфейс btThreadSendSMS позволяющий 
 * отправлять смс по bluetooth через GSM
 * телефон.
 * 
 * @version 	1.0 17 октября 2014
 * @author 	Карпенко Александр karpenkoAV@ukr.net
 * Лицензия Apache License 2
 */
public class btThreadSendSMS implements Runnable, btInterface {

	private BtSMS btSMS;
	private BluetoothDevice device;
	private Handler btHandler;
	private int port;
	private String number_telephone;
	private String textMessage;

	public btThreadSendSMS(MainActivity mainActivity, 
			String number_telephone, 
			String textMessage) {
		// TODO Auto-generated constructor stub
		this.device = mainActivity.device;
		this.port = mainActivity.RemoteDeviceRfcommPort;
		this.btHandler = mainActivity.btHandler;
		this.btSMS = new BtSMS(this.btHandler);
		this.number_telephone = number_telephone;
		this.textMessage = textMessage;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		if(btSMS.btSocketConnect(this.device, this.port)){
			if(btSMS.sendSMS(this.number_telephone, this.textMessage))
				btHandler.sendMessage(btHandler.obtainMessage(STATUS_END_THREAD_SEND_SMS
																, 1
																, 0
																, null));
			else
				btHandler.sendMessage(btHandler.obtainMessage(STATUS_END_THREAD_SEND_SMS
																, 0
																, 0
																, null));
		}		
		btSMS.btSocketClose();	
	}
}
