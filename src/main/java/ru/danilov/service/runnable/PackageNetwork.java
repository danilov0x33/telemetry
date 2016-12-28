package ru.danilov.service.runnable;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

//@JsonIgnoreProperties("LOG")
@XmlRootElement
public class PackageNetwork implements Serializable {
	private static final long serialVersionUID = -5091847070883347043L;
	
	//private final transient Logger LOG = LoggerFactory.getLogger(PackageNetwork.class);
	
	private byte[] pureData;
	private boolean compressed;
	private int uncompressedSize;
	
	public PackageNetwork() {
		
	}
	
	public PackageNetwork(byte[] pureData, boolean compressed) {
		uncompressedSize = pureData.length;
		this.compressed = compressed;
		if (compressed) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(uncompressedSize);
			try(GZIPOutputStream zos = new GZIPOutputStream(baos)){
				zos.write(pureData, 0, uncompressedSize);
			}catch(IOException e){
				//LOG.error("Ошибка при сжитии байтов!", e);
			}
			this.pureData = baos.toByteArray();
		} else {
			this.pureData = pureData;
		}

	}
	public byte[] getPureData(){
		if(compressed){
			ByteArrayInputStream bais = new ByteArrayInputStream(pureData);
			try(ByteArrayOutputStream uncompressed = new ByteArrayOutputStream(uncompressedSize); GZIPInputStream zis = new GZIPInputStream(bais)) {
				byte[] buf = new byte[1024];
				int len;
				while((len = zis.read(buf)) > 0){
					uncompressed.write(buf, 0, len);
				}
				return uncompressed.toByteArray();
			}catch(IOException e) {
				e.printStackTrace();
			}
		}
		return pureData;
	}
}