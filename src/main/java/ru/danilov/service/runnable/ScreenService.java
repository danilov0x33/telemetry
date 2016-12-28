package ru.danilov.service.runnable;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Отправка скриншотов на Restful сервер.
 * Created by Danilov on 01.05.2016.
 */
public class ScreenService implements Runnable {
    private final Logger LOG = LoggerFactory.getLogger(ScreenService.class);
    private final String username = System.getProperty("user.name");
    private final String pathTemp = "C:\\Temp\\KolaerService";
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H_m_s");
	
    private final String urlPathServer;
    
    public ScreenService(final String urlPathServer) {
		if(urlPathServer != null) {
			this.urlPathServer = urlPathServer;
		} else {
			this.urlPathServer = "http://localhost:8080";
		}
	}

	public void run() {
        final ObjectMapper objectMapper = new ObjectMapper();
        LOG.info("Старт отправка скриншотов...");
        while (true) {
            String jsonImage = null;
            byte[] imageInByte = null;
            try {
                LOG.info("Создание скриншота...");
                final Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
                final BufferedImage capture = new Robot().createScreenCapture(screenRect);

                try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    ImageIO.write(capture, "jpg", baos);
                    baos.flush();
                    imageInByte = baos.toByteArray();
                    final PackageNetwork packageNetwork = new PackageNetwork(imageInByte, true);

                    final StringWriter stringEmp = new StringWriter();
                    objectMapper.writeValue(stringEmp, packageNetwork);

                    jsonImage = stringEmp.toString();

                } catch (final IOException e) {
                	LOG.error("Ошибка чтении байтов!", e);
                }
            } catch (final AWTException e) {
                LOG.error("Ошибка при создании изображения!", e);
            }

            if(jsonImage != null) {
                try {
                    LOG.info("Отправка скриншота...");
                    final URL url = new URL(this.urlPathServer + "/system/user/"+URLEncoder.encode(username, "UTF-8")+"/package/screen");
                    final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoOutput(true);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");

                    final String input = jsonImage.toString();
                    final OutputStream os = conn.getOutputStream();
                    os.write(input.getBytes());
                    os.flush();
                    os.close();

                    conn.disconnect();
                } catch (final Exception e) {
                	LOG.error("Ошибка доступа к серверу!", e);
                    if(imageInByte != null) {
                		final String text = LocalTime.now().format(formatter);
                		final String fileName = text + "_scr.kaer";

                		final LocalDate localDate = LocalDate.now();
                		final File path = new File(pathTemp + "\\" + localDate);
                		path.mkdirs();
                    	LOG.error("Сохранение в локальную папку: {} ...", path);
                    	try(final FileOutputStream fos = new FileOutputStream(pathTemp + "\\" + localDate + "\\" + fileName)){
                    		fos.write(imageInByte);
						}catch(final IOException e1){
							LOG.error("Ошибка при сохранении изображения в локальную папку!", e1);
						}
                    }
                }
            }
            try {
                TimeUnit.MINUTES.sleep(3);
            } catch (final InterruptedException e) {
            	LOG.error("Ошибка ожидании потока!", e);
            }
        }
    }
}
