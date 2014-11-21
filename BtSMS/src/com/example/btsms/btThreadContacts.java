package com.example.btsms;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
/**
 * Интерфейс btThreadContacts позволяющий 
 * считывать записи телефонной книги.
 * 
 * @version 	1.0 17 октября 2014
 * @author 	Карпенко Александр karpenkoAV@ukr.net
 * Лицензия Apache License 2
 */
public class btThreadContacts implements Runnable, btInterface {
	
	private MainActivity context;
	private BtSMS btSMS;
	private BluetoothDevice device;
	private int port;
	private Handler btHandler;

	public btThreadContacts(MainActivity mainActivity) {
		// TODO Auto-generated constructor stub
		this.context = mainActivity;
		this.device = mainActivity.device;
		this.port = mainActivity.RemoteDeviceRfcommPort;
		this.btHandler = mainActivity.btHandler;		
		this.btSMS = new BtSMS(this.btHandler);
	}
	

	@Override
	public void run() {
		// TODO Auto-generated method stub
		if(btSMS.btSocketConnect(this.device, this.port)){
			//Если чтение телефонной книги прошло с ошибкой
			if(!btSMS.contacts())
				btHandler.sendMessage(btHandler.obtainMessage(STATUS_INTERNAL_ERROR,  
															  0, 
															  0, 
															  this.context.getResources().getString(R.string.ErrorBtReadPhoneBooks)));
				btSMS.btSocketClose();
		}
	}
}
