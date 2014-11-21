package com.example.btsms;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.widget.TextView;
/**
 * Интерфейс btThreadReadSMS позволяющий 
 * считывать смс записи с памяти телефона или
 * памяти сим карты.
 * 
 * @version 	1.0 17 октября 2014
 * @author 	Карпенко Александр karpenkoAV@ukr.net
 * Лицензия Apache License 2
 */
public class btThreadReadSMS implements Runnable, btInterface {

	private MainActivity context;
	private BluetoothDevice device;
	private int port;
	private Handler btHandler;
	private BtSMS btSMS;
	private TextView textView_bank_memory;

	public btThreadReadSMS(MainActivity mainActivity) {
		// TODO Auto-generated constructor stub
		this.context =  mainActivity;
		this.device = mainActivity.device;
		this.port = mainActivity.RemoteDeviceRfcommPort;
		this.btHandler = mainActivity.btHandler;
		this.btSMS = new BtSMS(this.btHandler);
		this.textView_bank_memory = mainActivity.textView_bank_memory;		
	}

	@Override
	public void run() {
		
		// TODO Auto-generated method stub
		try {
			if(btSMS.btSocketConnect(this.device, this.port)){
				//работаем с телефоном, выясняем поодерживает ли он чтение смс через bluetooth
				if(!btSMS.SMSsupport()){
					this.btHandler.sendEmptyMessage(1); // сообщим основному потоку что телефон не поддерживает чтение смс по bluetooth
				}else{
					if( btSMS.memoryEeprom()){
						// сообщим основному потоку что нужно остановиться и выбрать банк памяти для чтения смс
						this.btHandler.sendEmptyMessage(2);   
						//приостановим поток до выбора пользователем банка памяти
						while(this.textView_bank_memory.getText().equals(context.getResources().getString(R.string.textView_bank_memory_text)))
								Thread.sleep(1000);
						//продолжаем выполнение потока с выбранным банком памяти
						if(this.textView_bank_memory.getText().equals(context.getResources().getString(R.string.textView_bank_memory_text_sim)))
							btSMS.readSMS("SM");
						else
							btSMS.readSMS("ME");
					}
				}
				btSMS.btSocketClose();
				//отправим сообщение об окончании чтения смс с выбранного банка памяти
				btHandler.sendEmptyMessage(STATUS_END_THREAD);					
			}

		} catch (IllegalArgumentException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			btSMS.btSocketClose();
			//отправим сообщение об внутренней ошибке
			btHandler.sendMessage(btHandler.obtainMessage(STATUS_INTERNAL_ERROR,  0, 0, e.toString()));			
		}
	
	}

}
