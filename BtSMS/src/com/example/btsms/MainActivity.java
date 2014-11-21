package com.example.btsms;


import java.util.ArrayList;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Класс MainActivity определяющий содержимое Activity,
 * задает обработчики элементов управления,регистрирует 
 * широковещательные приемники системных сообщений от 
 * локального bluetooth адаптера, определяет обработку
 * сообщений от потоков обмена данными.   
 * 
 * @version 	1.0 17 октября 2014
 * @author 	Карпенко Александр karpenkoAV@ukr.net
 * Лицензия Apache License 2
 */
public class MainActivity extends Activity implements OnClickListener, btInterface{
	
	//класс для работы с локальным адаптером
	BluetoothAdapter LocalAdapter;
	//класс для работы с удаленным устройством
	BluetoothDevice device;
	//порт удаленного устройства предоставляющего сервис 0x1101
	int RemoteDeviceRfcommPort;
	TabHost  tabHost;
	//выводит имя и МАС адпес удаленного устройства
	TextView deviceField; 
	//выводит банк памяти с которым работает пользователь
	TextView textView_bank_memory;
	//для создания и подключения базы данных SQLite
	BtDbHelper btDbHelper;
	//для работы с подключенной базой данных
	SQLiteDatabase db;
	//для добавления записей в базу данных
	ContentValues btContentValues;
	//для работы с диалоговыми окнами
	BtMessage btMessage;
	//для приема сообщений с потоков
	Handler btHandler;
	//содержит достоупные для подключения телефоны
	ArrayAdapter<String> adapter;
	//хранит смс телефона который подключен к планшету
	ListView ListView_inbox;
	//для ввода номера телефона получателя смс
	EditText number_phone;
	//для ввода текста смс
	EditText text_message;
	// хранит массив ID смс в базу данных
	String[] ListView_inbox_sms_id;
	// хранит массив ID смс в телефоне
	String[] remote_device_sms_id;
	//пртиемники широковещательных сообщений от локального bluetooth адаптера
	BroadcastReceiver StatusAdapter;
	BroadcastReceiver DiscoveryResult;
	//поле для сообщения что телефон не поддерживает чтение смс
	TextView phoneIsNotSupported;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        //создаем базу данных хранения смс
        btDbHelper = new BtDbHelper(this);
        btContentValues = new ContentValues();
        //диалоговые окна
    	btMessage = new BtMessage();
    	//зададим контекст для диалоговых окошек 
    	btMessage.setBtMessageContext(this);
        
        LocalAdapter = BluetoothAdapter.getDefaultAdapter();

        //найдем TabHost
        tabHost = (TabHost) this.findViewById(android.R.id.tabhost);
        tabHost.setup();
        
        TabHost.TabSpec tabSpec;
        
        tabSpec = tabHost.newTabSpec("tag1");
        tabSpec.setIndicator("Create SMS");
        tabSpec.setContent(tabContentFactory);
        tabHost.addTab(tabSpec);
        
        tabSpec = tabHost.newTabSpec("tag2");
        tabSpec.setIndicator("InBox", getResources().getDrawable(R.drawable.ic_contacts));
        tabSpec.setContent(tabContentFactory);        
        tabHost.addTab(tabSpec);
        
        tabHost.setCurrentTabByTag("tag2");
        
        //Ищем TextView - ы
        phoneIsNotSupported = (TextView) tabHost.findViewById(R.id.phoneIsNotSupported);
        textView_bank_memory = (TextView) tabHost.findViewById(R.id.textView_bank_memory);
        final TextView textView_number_characters = (TextView) tabHost.findViewById(R.id.textView_number_characters);

        
        //Ищем EditText - ы
        number_phone = (EditText) tabHost.findViewById(R.id.editText_number_phone);
        text_message = (EditText) tabHost.findViewById(R.id.editText_message);
        
        //отслеживаем изменения в поле редактора смс text_message
        text_message.addTextChangedListener(new TextWatcher(){

			@Override
			public void afterTextChanged(Editable arg0) {
				// TODO Auto-generated method stub		
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {
				// TODO Auto-generated method stub					
			}

			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) {
				// TODO Auto-generated method stub
				String text = text_message.getText().toString();
				int len = text.length();
				textView_number_characters.setText(String.valueOf(152 - len));				
			}});        
        
        //найдем ListView
        ListView_inbox = (ListView) tabHost.findViewById(R.id.listView_inbox);
        ListView_inbox.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE); 
	
        //найдем Spinner
        final Spinner spinner = (Spinner) tabHost.findViewById(R.id.spinner_set_mode);
        spinner.setSelection(0);
        //обработчик изменений в Spinner
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,int position, long id) {
				Toast.makeText(getApplicationContext(), "Position = " + position, Toast.LENGTH_SHORT).show();
	    		//подключаем базу данных
	    		db = btDbHelper.getWritableDatabase();
	    		//содержит статус смс которые выбираются из БД и отображаются 
	    		String status = "";
				switch(position){
            	case 0:
            		//принятые не прочитанные
            		status = "REC UNREAD";
            		break;
            	case 1:
            		//принятые прочитанные
            		status = "REC READ";
            		break;
            	case 2:
            		//не отправленные
            		status = "STO UNSENT";
            		break;
            	case 3:
            		//отправленные
            		status = "STO SENT";
            		break; 					
				}
				//делаем выборку из БД согласно статусу смс
				readDB(status,ListView_inbox);
				db.close();
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				
			}
		});   
        
        //найдем button

        ImageButton ButtonNewConnection = (ImageButton) findViewById(R.id.ButtonNewConnection);
        ImageButton ButtonContacts = (ImageButton) tabHost.findViewById(R.id.ButtonContacts);
        Button ButtonSend = (Button) tabHost.findViewById(R.id.buttonSend);
        ImageButton ButtonReload = (ImageButton) tabHost.findViewById(R.id.ButtonReload);
        Button ButtonReply = (Button) tabHost.findViewById(R.id.buttonReply);
        Button buttonDelete = (Button) tabHost.findViewById(R.id.buttonDelete);
        
        //об'явим обработчики button
        ButtonNewConnection.setOnClickListener(this);
        ButtonContacts.setOnClickListener(this);
        ButtonSend.setOnClickListener(this);
        ButtonReload.setOnClickListener(this);
        ButtonReply.setOnClickListener(this);
        buttonDelete.setOnClickListener(this);
        
        //зарегистрируем приемнки сообщений от  bluetooth
        btCreateRegisterReceiver();
        
        //обработчик сообщений от потоков обмена данными
        btHandler = new Handler(){      	

			public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                //сигнал максимального кол-ва смс в памяти телефона
                case STATUS_MAX_SMS: 
                	btMessage.ProgressDialogClose();
                	btMessage.ProgressDialogReadMessage(msg.arg1);
                	break;
                //сигнал того что телефон не поддерживает чтение по bluetooth
                case STATUS_PHONE_IS_NOT_SUPPORTED:
                	btMessage.ProgressDialogClose();
                	ListView_inbox.setVisibility(0);
                	phoneIsNotSupported.setVisibility(1);
                	break;
                //сигнал выбора банка памяти
                case STATUS_SELECT_BAMK_MEMORY:
                	btMessage.AlertDialogSelectBankMemory();
                	break;
                //сигнал изменения горизонтального прогресса
                case STATUS_SET_PROGRESS_SMS:
                	//если сообщение от потока читающего смс 
                	if(msg.arg2 == 0){
	                	String[] messages = new String[5];
						if(MainActivity.this.textView_bank_memory.getText().equals(MainActivity.this.getResources().getString(R.string.textView_bank_memory_text_sim)))
							messages = (String[]) msg.obj;
	                	else
	                		messages = (String[]) msg.obj;
						if (messages != null){
		            		btContentValues.put("bank", messages[0]);
		            		btContentValues.put("status", messages[1]);
		            		btContentValues.put("date", messages[2]); 
		            		btContentValues.put("number", messages[3]);
		            		btContentValues.put("content", messages[4]);
		            		btContentValues.put("num", messages[5]);
		            	    // вставляем запись и получаем ее ID
		            	    db.insert("btTable", null, btContentValues);  
	                	}
					//если сообщение от потока удаляющего смс 	
                	} else{
                		//удалим из базы смс
                		//Log.e(this.toString(), "id = " + ListView_inbox_sms_id[Integer.valueOf((String) msg.obj)] );
                		db.delete("btTable", 
                				  "id = " + ListView_inbox_sms_id[Integer.valueOf((String) msg.obj)],
                				  null);
                	}
                	//изменяем прогресс +1
                	btMessage.setProgressDialogReadMessage();
                	break;
                //сигнал завершения потока
                case STATUS_END_THREAD:
                	String status = "";
                	switch(spinner.getSelectedItemPosition()){
                	case 0:
                		status = "REC UNREAD";
                		break;
                	case 1:
                		status = "REC READ";
                		break;
                	case 2:
                		status = "STO UNSENT";
                		break;
                	case 3:
                		status = "STO SENT";
                		break;                		
                	}
                	//чтение из БД
                	readDB(status,ListView_inbox);
                	db.close();
                	break;
                //сигнал удаления выбранного в списке смс
                case STATUS_LIST_VIEW_ITEM_DELETE:
                	//обрабатываем сообщение от btMessage.AlertDialogDelete
                	///////////////////////////////////////////
    	    		//подключаем базу данных
    	    		db = btDbHelper.getWritableDatabase();
    	    		
    	    		//показываем progressDialog
                	btMessage.ProgressDialogWait();
    	    		
    		    	String[] macremoteDeviceInfo = ((String) deviceField.getText()).split("    ");  
    		    	device = LocalAdapter.getRemoteDevice(macremoteDeviceInfo[1]);	
    	    		Runnable btRunnable = new btThreadDeleteSMS(MainActivity.this, ListView_inbox_checked());
    				//запускаем поток обмена данными bleutooth
    	    		Thread btTh = new Thread(btRunnable);	
    				btTh.start();   
                	break;
                //сигнал завершения стения списка контактов
                case STATUS_END_READ_CONTACT:
                	btMessage.ProgressDialogClose();
                	btMessage.AlertDialogSelectContacts((String[]) msg.obj, number_phone);
                	break;
                //сигнал завершения потока отправки смс
                case STATUS_END_THREAD_SEND_SMS:
                	btMessage.ProgressDialogClose();
                	if(msg.arg1 == 1)
                		btMessage.AlertDialogError(MainActivity.this.getResources().getString(R.string.alertDialogStatusSendSmsTitle)
                								    , MainActivity.this.getResources().getString(R.string.alertDialogStatusSendSmsOk));
                	else
                		btMessage.AlertDialogError(MainActivity.this.getResources().getString(R.string.alertDialogStatusSendSmsTitle)
								   					, MainActivity.this.getResources().getString(R.string.alertDialogStatusSendSmsErr));
                	break;
                //сигнали внутренних ошибок
                case STATUS_INTERNAL_ERROR:
                	btMessage.ProgressDialogClose();
                	btMessage.AlertDialogError(MainActivity.this.getResources().getString(R.string.alertDialogBtError)
                							   , (String) msg.obj);
                	break;
                }     		
        	};
        };
        
    	//создадим диалоговые окна
    	btMessage.createDialog(btHandler, textView_bank_memory);
    }
    
    //чтение из БД и вывод информации в  ListView_inbox
	void readDB(String status,ListView ListView_inbox){
		
		//содержит выбранные из БД смс
    	ArrayList<String> arrayMessage = new ArrayList<String>();
    	
    	Cursor c;
		if(textView_bank_memory.getText().equals(MainActivity.this.getResources().getString(R.string.textView_bank_memory_text_sim)))
            c = db.query("btTable"
            				, null 
            				, "bank = \"SM\" AND status = \""+ status +"\"" 
            				, null 
            				, null
            				, null 
            				, null);
		else
            c = db.query("btTable" 
            				, null
            				, "bank = \"ME\" AND status = \"" + status + "\""
            				, null 
            				, null
            				, null
            				, null);

		ListView_inbox_sms_id = new String[c.getCount()];
		remote_device_sms_id = new String[c.getCount()];
        if (c.moveToFirst()) {

          int idColIndex = c.getColumnIndex("id");
          //int bankColIndex = c.getColumnIndex("bank");
          //int statusColIndex = c.getColumnIndex("status");
          //int dateColIndex = c.getColumnIndex("date");
          //int numberColIndex = c.getColumnIndex("number");
          int contentColIndex = c.getColumnIndex("content");
          int numColIndex = c.getColumnIndex("num");
          int i = 0;
          do {
        	  ListView_inbox_sms_id[i] = c.getString(idColIndex);
        	  remote_device_sms_id[i] = c.getString(numColIndex);
        	  arrayMessage.add(c.getString(contentColIndex)); 
        	  i++;
          } while (c.moveToNext());  
          
  		adapter = new ArrayAdapter<String>(MainActivity.this 
  											, android.R.layout.simple_list_item_single_choice 
  											, arrayMessage.toArray(new String[0]));
  		ListView_inbox.setAdapter(adapter);  
        }else{
        	ListView_inbox.setAdapter(null);; 
        	Log.e(this.toString(), "0 rows");
        }
        c.close();		
	}
    
	//обработчик ListView_inbox, выполняющий поис выбранных элементов списка
	String[] ListView_inbox_checked()
	{
		ArrayList<String> listViewChecked = new ArrayList<String>();
	    SparseBooleanArray sbArray = ListView_inbox.getCheckedItemPositions();
	    //String[] arr;
	    for (int i = 0; i < sbArray.size(); i++) {
	      int key = sbArray.keyAt(i);
	      if (sbArray.get(key))
	    	  listViewChecked.add(String.valueOf(key));
	    }	
	    return listViewChecked.toArray(new String[0]);
	}
	
    //определяем содержимое TabHost от выбранной вкладки
    TabHost.TabContentFactory tabContentFactory = new TabHost.TabContentFactory() {
		
		@Override
		public View createTabContent(String tag) {
			switch(tag){
			case "tag1":
				return getLayoutInflater().inflate(R.layout.create_sms, null);
			case "tag2":
				return getLayoutInflater().inflate(R.layout.inbox, null);
			};		
			// TODO Auto-generated method stub
			return null;
		}
	};

	void btCreateRegisterReceiver(){
		
		// отслеживание конца сканирования
		StatusAdapter = null;
		//отслеживаем найденные удаленные устройства
		DiscoveryResult = null;
		
		deviceField = (TextView) findViewById(R.id.textView_device);
		final ArrayList<String> remoteDeviceArray = new ArrayList<String>();
		//срабатывает по окончанию сканирования BLUETOOTH
		StatusAdapter = new BroadcastReceiver(){

			@Override
			public void onReceive(Context context, Intent intent) {
				// TODO Auto-generated method stub
				//отключим приемники сообщений от локального Bluetooth адаптера
				btUnRegisterReceiver();
				//преобразуем в массив строк
				String[] StringRemoteDevice = remoteDeviceArray.toArray(new String[0]);
				remoteDeviceArray.clear();
				btMessage.ProgressDialogClose();
				btMessage.AlertDialogConnectionDevice(StringRemoteDevice, deviceField);
			}};
		//срабатывает с каждым найденным bluetooth устройством
		DiscoveryResult = new BroadcastReceiver(){

			@Override
			public void onReceive(Context context, Intent intent) {
				// TODO Auto-generated method stub
				device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				Toast.makeText(context, device.getName() + " " + device.getAddress(), Toast.LENGTH_SHORT).show();
				remoteDeviceArray.add(device.getName() + "    " + device.getAddress());			
			}};		
	};
	
	void btRegisterReceiver(){
		// регистрируем приемники
		registerReceiver(StatusAdapter, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
		registerReceiver(DiscoveryResult, new IntentFilter(BluetoothDevice.ACTION_FOUND));	
	}
	
	void btUnRegisterReceiver(){
		// отключим приемники
		unregisterReceiver(StatusAdapter);
		unregisterReceiver(DiscoveryResult);
	}
	
	@Override
	public void onClick(View v) {

	
		// TODO Auto-generated method stub
		String[] checked;
		Runnable btRunnable;
		Thread btTh;		
		
    	RemoteDeviceRfcommPort = btMessage.getRemoteDeviceRfcommPort();
		switch(v.getId()){
		//новое подключение
		case R.id.ButtonNewConnection:
			 //если уже был подключен телефон не поддерживающий чтение смс
			if(phoneIsNotSupported.getVisibility() == View.VISIBLE)
				phoneIsNotSupported.setVisibility(View.INVISIBLE);
			if(LocalAdapter==null){
				btMessage.AlertDialogError(this.getResources().getString(R.string.alertDialogBtError)
										   , this.getResources().getString(R.string.BtErrorAdapterNotSupport) );
			}else{
				//зарегистрируем приемники сообщений от локального Bluetooth адаптера
				btRegisterReceiver();
				if(LocalAdapter.isEnabled()){
					if(!LocalAdapter.isDiscovering()){
						btMessage.ProgressDialogDiscoveryDevice(LocalAdapter);
					}
				}else{
					Intent enableBtIntent = new Intent(LocalAdapter.ACTION_REQUEST_ENABLE);
					startActivityForResult(enableBtIntent, 1);
				}
			}	
			break;
		//получить спиок контактов
		case R.id.ButtonContacts:
			if(!((String) deviceField.getText()).equals("No device")){
				btMessage.ProgressDialogWait();			
	    		btRunnable = new btThreadContacts(MainActivity.this);
				//запускаем поток обмена данными bleutooth
	    		btTh = new Thread(btRunnable);	//поток обменна данными по bluetooth
				btTh.start();			
			}else
				btMessage.AlertDialogError(this.getResources().getString(R.string.alertDialogBtError)
										  , this.getResources().getString(R.string.alertDialogBtnContactsErr));
			break;	
		//отправка смс
		case R.id.buttonSend:
			if(!number_phone.getText().toString().equals("")  && !((String) deviceField.getText()).equals("")){
	    		//показываем progressDialog
	    		btMessage.ProgressDialogWait();
	    		
		    	String[] macRemoteDeviceInfo = ((String) deviceField.getText()).split("    ");  
		    	device = LocalAdapter.getRemoteDevice(macRemoteDeviceInfo[1]);	
		    	String[] number_phone_split = number_phone.getText().toString().split(" ");
	    		btRunnable = new btThreadSendSMS(MainActivity.this
							    				, number_phone_split[0]
							    				, (String) text_message.getText().toString());
				//запускаем поток обмена данными bleutooth
	    		btTh = new Thread(btRunnable);	
				btTh.start(); 	
			}else
				btMessage.AlertDialogError(this.getResources().getString(R.string.alertDialogBtError)
										   , this.getResources().getString(R.string.alertDialogBtnSendErr));
			break;	
		//обновить смс сообщения из банка памяти телефона
		case R.id.ButtonReload:
	    	if (((String) deviceField.getText()).equals("No device"))
	    		btMessage.AlertDialogError(this.getResources().getString(R.string.alertDialogBtError)
	    									, this.getResources().getString( R.string.ErrorBtRemoteNoSelected));
	    	else{
				
	    		//подключаем базу данных
	    		db = btDbHelper.getWritableDatabase();
	    		
	    		//если программа запущена НЕ впервые
	    		if(!this.textView_bank_memory.getText().equals(this.getResources().getString(R.string.textView_bank_memory_text))){
		    		//очистим старые записи в базе по условию
					if(this.textView_bank_memory.getText().equals(this.getResources().getString(R.string.textView_bank_memory_text_sim)))
			    		//проверим есть ли записи
			    		if (db.query("btTable"
			    				, null, "bank = \"SM\""
			    				, null
			    				, null
			    				, null
			    				, null).getCount() != 0) {
			    			db.delete("btTable", "bank = \"SM\"", null);
			    		}else
			    		//проверим есть ли записи
			    		if (db.query("btTable"
			    				, null
			    				, "bank = \"ME\""
			    				, null
			    				, null
			    				, null
			    				, null).getCount() != 0) {
			    			db.delete("btTable", "bank = \"ME\"", null);
			    		}	    			
	    		}else{
	    			Cursor c = db.query("btTable"
	    								, null
	    								, null
	    								, null
	    								, null
	    								, null
	    								, null);
	    			if(c.getColumnCount() != 0)
	    				db.delete("btTable", null, null);
	    		}

				//установим значение по умолчанию
				textView_bank_memory.setText(this.getResources().getString(R.string.textView_bank_memory_text));	    		
	    		//показываем progressDialog
	    		btMessage.ProgressDialogWait();
	    		 
	    		String[] macRemoteDeviceInfo = ((String) deviceField.getText()).split("    ");  
		    	device = LocalAdapter.getRemoteDevice(macRemoteDeviceInfo[1]);	
	    		btRunnable = new btThreadReadSMS(this);
				//запускаем поток обмена данными bleutooth
	    		btTh = new Thread(btRunnable);	//поток обменна данными по bluetooth
				btTh.start();
	    	}
			break;
		//ответить на выбранное из списка смс
		case R.id.buttonReply:
			checked = ListView_inbox_checked();
			if(checked.length > 1 || checked.length == 0)
				btMessage.AlertDialogError(this.getResources().getString(R.string.alertDialogBtError)
										   , this.getResources().getString(R.string.alertDialogBtnReplyMessage));
			else if (checked.length == 1){
				//сдесь метод ListView_inbox_checked() вернет только один выбранный элемент
				String[] item = ListView_inbox_checked();
				String telephone = "";
				tabHost.setCurrentTabByTag("tag1");
				//проверим что сообщение имеет номер отправителя
				if(adapter.getItem(Integer.parseInt(item[0])).charAt(1) == '+'){
					for(int i = 1; i < 14; i++){
						telephone += adapter.getItem(Integer.parseInt(item[0])).charAt(i);
					}
					number_phone.setText(telephone);
   				
	        		Log.e(this.toString(), telephone);
				}else{
					btMessage.AlertDialogError(this.getResources().getString(R.string.alertDialogBtError)
											   , this.getResources().getString(R.string.alertDialogBtnReplyErr));
					number_phone.setText("");
				}					
			}
		
			break;
		//удалить выбранную(ые) смс из списка
		case R.id.buttonDelete:
			checked = ListView_inbox_checked();
			if(checked.length == 0)
				btMessage.AlertDialogError(this.getResources().getString(R.string.alertDialogBtError)
											, this.getResources().getString(R.string.alertDialogBtnDelete));	
			else
				btMessage.AlertDialogDelete(this.getResources().getString(R.string.alertDialogBtnDeleteTitle)
											, this.getResources().getString(R.string.alertDialogBtnDeleteMessage));
			break;		
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if (resultCode == RESULT_OK) {
	        switch (requestCode) {
	        case 1:
	        	btMessage.ProgressDialogDiscoveryDevice(LocalAdapter);
	          break;
	        }
	    }else {
	        btMessage.AlertDialogError(this.getResources().getString(R.string.alertDialogBtError)
	        							, this.getResources().getString( R.string.ErrorBtAdapterEnableRequest));
	    }
	}
	

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
     
}
