package com.example.btsms;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.widget.TextView;
/**
 * Интерфейс btThreadDeleteSMS позволяющий 
 * удалять смс записи с памяти телефона и
 * базы данных.
 * 
 * @version 	1.0 17 октября 2014
 * @author 	Карпенко Александр karpenkoAV@ukr.net
 * Лицензия Apache License 2
 */
public class btThreadDeleteSMS implements Runnable, btInterface {

	private MainActivity context;
	private BluetoothDevice device;
	private int port;
	private Handler btHandler;
	private TextView textView_bank_memory;
	private String[] checked;
	private String[] remote_device_sms_id;
	private BtSMS btSMS;

	public btThreadDeleteSMS(MainActivity mainActivity,
			String[] checked) {
		// TODO Auto-generated constructor stub
		this.context = mainActivity;
		this.device = mainActivity.device;
		this.port = mainActivity.RemoteDeviceRfcommPort;
		this.btHandler = mainActivity.btHandler;
		this.textView_bank_memory = mainActivity.textView_bank_memory;	
		this.checked = checked;
		this.remote_device_sms_id = mainActivity.remote_device_sms_id;
		this.btSMS = new BtSMS(this.btHandler);		
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		if(btSMS.btSocketConnect(this.device, this.port)){
			if(this.textView_bank_memory.getText().equals(this.context.getResources().getString(R.string.textView_bank_memory_text_sim)))
				btSMS.deleteSMS("SM", checked, remote_device_sms_id);
			else
				btSMS.deleteSMS("ME", checked, remote_device_sms_id);
		}
		btSMS.btSocketClose();
		//отправим сообщение об окончании удаления смс с выбранного банка памяти
		btHandler.sendEmptyMessage(STATUS_END_THREAD);			
	}

}
