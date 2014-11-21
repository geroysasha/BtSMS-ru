package com.example.btsms;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

/**
 * Класс BtSMS определяет взаимодействие с 
 * телефоном по bluetooth.    
 * 
 * @version 	1.0 17 октября 2014
 * @author 	Карпенко Александр karpenkoAV@ukr.net
 * Лицензия Apache License 2
 */

public class BtSMS implements btInterface{
	
	private BluetoothSocket RfommSocket;
	private InputStream inRfcomm;
	private OutputStream outRfcomm;
	private int byteSize = 1024*4;
	private byte[] inByte = new byte[byteSize];
	private Handler btHandler;
	
	public BtSMS(Handler btHandler) {
		// TODO Auto-generated constructor stub
		this.btHandler = btHandler;
		//	Log.e(this.toString(), String str);
	}
	
	//создание Rfcomm сокета и подключение
	public boolean btSocketConnect(BluetoothDevice device, int port){
		try {
			Method m = null;
			try {
				m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				btHandler.sendMessage(btHandler.obtainMessage(STATUS_INTERNAL_ERROR
										, 0
										, 0
										, e.toString()));
			}
			try {
				RfommSocket = (BluetoothSocket) m.invoke(device, port);
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				btHandler.sendMessage(btHandler.obtainMessage(STATUS_INTERNAL_ERROR
										, 0
										, 0
										, e.toString()));
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				btHandler.sendMessage(btHandler.obtainMessage(STATUS_INTERNAL_ERROR
										, 0
										, 0
										, e.toString()));
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				btHandler.sendMessage(btHandler.obtainMessage(STATUS_INTERNAL_ERROR
										, 0
										, 0
										, e.toString()));
			}   
	        
			inRfcomm = RfommSocket.getInputStream();
			outRfcomm = RfommSocket.getOutputStream(); 	
			//подключим сокет
			RfommSocket.connect();
			return true;			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			btSocketClose();
			btHandler.sendMessage(btHandler.obtainMessage(STATUS_INTERNAL_ERROR,  0, 0, e.toString()));			
			return false;
		}
	}

	//Закрывает Rfcomm сокет
	public boolean btSocketClose(){
		try {
			RfommSocket.close();
			return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			btHandler.sendMessage(btHandler.obtainMessage(STATUS_INTERNAL_ERROR
									, 0
									, 0
									, e.toString()));			
			return false;
		}
	}
	
	//отправка АТ комманды
	private String sendAtcommands(String commands) throws IOException, InterruptedException{
		
		byte[] tmpByte = new byte[0]; // хранение промежуточного состояния messByte
		byte[] messByte = new byte[0];	//содержит полный ответ с сокета
		int bytesRead = 0;
		int bytesReadAdd = 0;
		String mess = "";
		//отправим команду в телефон
		outRfcomm.write(commands.getBytes());
		//читаем ответ
		if(!commands.contains("AT+CMGS") && !commands.contains("AT+CMGD")){
			while(true){
						
					bytesRead = inRfcomm.read(inByte);
					
					// увеличим размер массива на величину равную bytesRead
					messByte = new byte[tmpByte.length + bytesRead];
					
					//копирование считанных байт в конец messByte
					System.arraycopy(inByte, 0, messByte, bytesReadAdd, bytesRead);
					
					//если tmpByte уже содержит байты (не первая итерация)
					if(tmpByte.length !=0)
						System.arraycopy(tmpByte, 0, messByte, 0, tmpByte.length);
					tmpByte = new byte[messByte.length];
					
					//скопируем в начало messByte массив tmpByte
					System.arraycopy(messByte, 0, tmpByte, 0, messByte.length);
					
					bytesReadAdd += bytesRead;
	
					mess =  new String(messByte, 0, bytesReadAdd);
					if(mess.contains("OK"))
						break;	
					if(mess.contains("ERROR"))
						break;					
			}
		}else{
			Thread.sleep(3000);
			bytesRead = inRfcomm.read(inByte);
			mess =  new String(inByte, 0, bytesRead);
		}
		return  mess;
	}
	
	//проверка поддержки телефоном чтения смс по bluetooth
	public boolean SMSsupport(){

		String message;
		try {
			message = sendAtcommands("AT+CMGR=?;\r");
			if(message.contains("ERROR"))
				return false;
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			btHandler.sendMessage(btHandler.obtainMessage(STATUS_INTERNAL_ERROR
									, 0
									, 0
									, e.toString()));
		}	
		
		return true;
	}
	
	//проверяет доступность памяти телефона 'ME'
	public boolean memoryEeprom(){
		String message;
			try {
				message = sendAtcommands("AT+CPMS=?;\r");
				if(message.contains("ME"))
					return true;
			} catch (IOException | InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				btHandler.sendMessage(btHandler.obtainMessage(STATUS_INTERNAL_ERROR
										, 0
										, 0
										, e.toString()));				
				return false;
			}
			return false;		
	}

	//чтение списка контактов
	public boolean contacts(){
		String message = null;
		String[] arrTmp = {"1","2"};
		try {
			sendAtcommands("AT+CMGF=1;\r");
			sendAtcommands("AT+CPBS=\"ME\";\r");
			arrTmp = sendAtcommands("AT+CPBS?;\r").split(",");
			arrTmp[arrTmp.length - 1] = arrTmp[arrTmp.length - 1].replace("\r\nOK\r\n", "");
			message = sendAtcommands("AT+CPBR=1," + arrTmp[arrTmp.length - 1] + ";\r\n");
			message = message.replace("AT+CPBR=1," + arrTmp[arrTmp.length - 1] + ";\r\r", "");		
			sendAtcommands("AT+CPBS=\"SM\";\r");
			arrTmp = sendAtcommands("AT+CPBS?;\r\n").split(",");
			arrTmp[arrTmp.length - 1] = arrTmp[arrTmp.length - 1].replace("\r\nOK\r\n", "");
			message += sendAtcommands("AT+CPBR=1," + arrTmp[arrTmp.length - 1] + ";\r\n");
			message = message.replace("AT+CPBR=1," + arrTmp[arrTmp.length - 1] + ";\r\r", "");	
			message = message.replace("+CPBR:", "");
			message = message.replace("OK\r", "");
			message = message.replace(",129,", ",");
			message = message.replace(",145,", ",");
			message = message.replace(",0", "");
			message = message.replace("\"", "");
			arrTmp = message.split("\r");
			btHandler.sendMessage(btHandler.obtainMessage(STATUS_END_READ_CONTACT,  0, 0, arrTmp));	
			return true;
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			btHandler.sendMessage(btHandler.obtainMessage(STATUS_INTERNAL_ERROR,  0, 0, e.toString()));			
		}
		return false;		
	}		
	
	//отправляет смс
	public boolean sendSMS(String number_telephone, String textMessage){
		String message;
		try {
			message = sendAtcommands("AT+CMGF=1;\r");
			message = sendAtcommands("AT+CMGS=\"" + number_telephone + "\";\r\n");	
			message += sendAtcommands(textMessage + "\032;\r");

			if(message.contains("OK"))
				return true;
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			btHandler.sendMessage(btHandler.obtainMessage(STATUS_INTERNAL_ERROR
									, 0
									, 0
									, e.toString()));			
			return false;
		}
		return false;
	}	
	
	//читает смс
	public void readSMS(String bankMemory){
        
		String message;
		String sizeMessage = "";
		try {
			message = sendAtcommands("AT+CMGF=1;\r");
			message = sendAtcommands("AT+CPMS=\"" + bankMemory + "\";\r");
			String[] splitMessage = message.split(",");
			if(message.contains(bankMemory))
				sizeMessage = splitMessage[2];
			else
				sizeMessage = splitMessage[1];
			
			//отправим сообщение о максимальном количестве смс
			btHandler.sendMessage(btHandler.obtainMessage(STATUS_MAX_SMS
									, Integer.valueOf(sizeMessage)
									, 0));		
			
			for(int i = 0; i <   Integer.valueOf(sizeMessage); i++){
				
				message = sendAtcommands("AT+CMGR="+String.valueOf(i)+";\r");
				
				if(!message.contains("ERROR"))
					btHandler.sendMessage(btHandler.obtainMessage(STATUS_SET_PROGRESS_SMS
											, i
											, 0
											, messageToDB(message
													, bankMemory
													, "AT+CMGR="+String.valueOf(i)+";\r"
													, i)));
				else
					btHandler.sendMessage(btHandler.obtainMessage(STATUS_SET_PROGRESS_SMS
											, i
											, 0
											, null));
			}
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			
			e.printStackTrace();
			
			btHandler.sendMessage(btHandler.obtainMessage(STATUS_INTERNAL_ERROR
									, 0
									, 0
									, e.toString()));			
		}
		
	}
	
	//удаляет смс с памяти телефона
	public void deleteSMS(String bankMemory, String[] checked, String[] remote_device_sms_id){

		try {
			sendAtcommands("AT+CMGF=0;\r");
			sendAtcommands("AT+CPMS=\"" + bankMemory + "\";\r");
			
			//отправим сообщение о максимальном количестве смс
			btHandler.sendMessage(btHandler.obtainMessage(STATUS_MAX_SMS
									, checked.length
									, 0));	
			
			for(int i = 0; i <  checked.length; i++){
				sendAtcommands("AT+CMGD=" + remote_device_sms_id[i] + ";\r");
				btHandler.sendMessage(btHandler.obtainMessage(STATUS_SET_PROGRESS_SMS
										, i
										, 1
										, checked[i]));	
			}
			
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			btHandler.sendMessage(btHandler.obtainMessage(STATUS_INTERNAL_ERROR
									, 0
									, 0
									, e.toString()));			
		}
		
	}
	
	//формирует значение полей для записи в базу данных
    public String[] messageToDB(String str, String bankMemory, String atCommand, int i){
	    String[] strReturn = new String[6];
	    strReturn[0] = bankMemory;
		if(str.contains("REC UNREAD")){
			strReturn[1] = "REC UNREAD";
			str = str.replace("REC UNREAD", "");
		}
		if(str.contains("REC READ")){
			strReturn[1] = "REC READ";
			str = str.replace("REC READ", "");
		}
		if(str.contains("STO UNSENT")){
			strReturn[1] = "STO UNSENT";	
			strReturn[2] = "";
			str = str.replace("STO UNSENT", "");
		}
		if(str.contains("STO SENT")){
			strReturn[1] = "STO SENT";
			str = str.replace("STO SENT", "");
		}
		//очищаем полученную информацию с телефона
		str = str.replace("\r\n\r\n", " ");
		str = str.replace(atCommand, "");
		str = str.replace("+CMGR:\"\",", "");	
		str = str.replace("OK", "");
		str = str.replace(", \"0\"", "");
		str = str.replace("\"", "");
		str = str.replace("\n", "");
		strReturn[3] = "";	
		strReturn[4] = str;
		strReturn[5] = String.valueOf(i);
		return strReturn;
    }	
}
