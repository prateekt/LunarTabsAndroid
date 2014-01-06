package org.herac.tuxguitar.io.base;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.herac.tuxguitar.io.base.event.TGFileFormatEventManager;
import org.herac.tuxguitar.io.gpx.GPXInputStream;
import org.herac.tuxguitar.io.gtp.GP1InputStream;
import org.herac.tuxguitar.io.gtp.GP2InputStream;
import org.herac.tuxguitar.io.gtp.GP3InputStream;
import org.herac.tuxguitar.io.gtp.GP4InputStream;
import org.herac.tuxguitar.io.gtp.GP5InputStream;
import org.herac.tuxguitar.io.gtp.GTPSettings;
import org.herac.tuxguitar.io.ptb.PTInputStream;
import org.herac.tuxguitar.io.tg.TGInputStream;
import org.herac.tuxguitar.io.tg.TGOutputStream;
import org.herac.tuxguitar.io.tg.TGStream;

public class TGFileFormatManager {
	
	public static final String DEFAULT_EXTENSION = TGStream.TG_FORMAT_EXTENSION;
	
	private static TGFileFormatManager instance;
	
	private TGFileFormatEventManager eventManager;
	private TGSongLoader loader;
	private TGSongWriter writer;
	private List inputStreams;
	private List outputStreams;
	private List exporters;
	private List importers;
	
	private TGFileFormatManager(){
		this.eventManager = new TGFileFormatEventManager();
		this.loader = new TGSongLoader();
		this.writer = new TGSongWriter();
		this.inputStreams = new ArrayList();
		this.outputStreams = new ArrayList();
		this.exporters = new ArrayList();
		this.importers = new ArrayList();
		this.addDefaultStreams();
	}
	
	public static TGFileFormatManager instance(){
		if(instance == null){
			instance = new TGFileFormatManager();
		}
		return instance;
	}
	
	public TGFileFormatEventManager getEventManager() {
		return this.eventManager;
	}

	public TGSongLoader getLoader(){
		return this.loader;
	}
	
	public TGSongWriter getWriter(){
		return this.writer;
	}
	
	public void addInputStream(TGInputStreamBase stream){
		if(!this.inputStreams.contains(stream)){
			this.inputStreams.add(stream);
			this.eventManager.onInputStreamAdded(stream);
		}
	}
	
	public void removeInputStream(TGInputStreamBase stream){
		if( this.inputStreams.contains(stream)){
			this.inputStreams.remove(stream);
			this.eventManager.onInputStreamRemoved(stream);
		}
	}
	
	public int countInputStreams(){
		return this.inputStreams.size();
	}
	
	public void addOutputStream(TGOutputStreamBase stream){
		if(!this.outputStreams.contains(stream)){
			this.outputStreams.add(stream);
			this.eventManager.onOutputStreamAdded(stream);
		}
	}
	
	public void removeOutputStream(TGOutputStreamBase stream){
		if( this.outputStreams.contains(stream)){
			this.outputStreams.remove(stream);
			this.eventManager.onOutputStreamRemoved(stream);
		}
	}
	
	public int countOutputStreams(){
		return this.outputStreams.size();
	}
	
	public void addImporter(TGRawImporter importer){
		if(!this.importers.contains(importer)){
			this.importers.add(importer);
			this.eventManager.onRawImporterAdded(importer);
		}
	}
	
	public void removeImporter(TGRawImporter importer){
		if( this.importers.contains(importer)){
			this.importers.remove(importer);
			this.eventManager.onRawImporterRemoved(importer);
		}
	}
	
	public int countImporters(){
		return this.importers.size();
	}
	
	public void addExporter(TGRawExporter exporter){
		if(!this.exporters.contains(exporter)){
			this.exporters.add(exporter);
			this.eventManager.onRawExporterAdded(exporter);
		}
	}
	
	public void removeExporter(TGRawExporter exporter){
		if( this.exporters.contains(exporter)){
			this.exporters.remove(exporter);
			this.eventManager.onRawExporterRemoved(exporter);
		}
	}
	
	public int countExporters(){
		return this.exporters.size();
	}
	
	public Iterator getInputStreams(){
		return this.inputStreams.iterator();
	}
	
	public Iterator getOutputStreams(){
		return this.outputStreams.iterator();
	}
	
	public Iterator getImporters(){
		return this.importers.iterator();
	}
	
	public Iterator getExporters(){
		return this.exporters.iterator();
	}
	
	public List getInputFormats(){
		List formats = new ArrayList();
		Iterator it = getInputStreams();
		while(it.hasNext()){
			TGInputStreamBase stream = (TGInputStreamBase)it.next();
			TGFileFormat format = stream.getFileFormat();
			if(!existsFormat(format, formats)){
				formats.add(format);
			}
		}
		return formats;
	}
	
	public List getOutputFormats(){
		List formats = new ArrayList();
		Iterator it = getOutputStreams();
		while(it.hasNext()){
			TGOutputStreamBase stream = (TGOutputStreamBase)it.next();
			TGFileFormat format = stream.getFileFormat();
			if(!existsFormat(format, formats)){
				formats.add(format);
			}
		}
		return formats;
	}
	
	private boolean existsFormat(TGFileFormat format,List formats){
		Iterator it = formats.iterator();
		while(it.hasNext()){
			TGFileFormat comparator = (TGFileFormat)it.next();
			if(comparator.getName().equals(format.getName()) || comparator.getSupportedFormats().equals(format.getSupportedFormats())){
				return true;
			}
		}
		return false;
	}
	
	private void addDefaultStreams(){
		this.addInputStream(new TGInputStream());
		this.addOutputStream(new TGOutputStream());
        this.addInputStream(new GP5InputStream(new GTPSettings()));
        this.addInputStream(new GP4InputStream(new GTPSettings()));
        this.addInputStream(new GP3InputStream(new GTPSettings()));
        this.addInputStream(new GP2InputStream(new GTPSettings()));
        this.addInputStream(new GP1InputStream(new GTPSettings()));
        this.addInputStream(new GPXInputStream());
        this.addInputStream(new PTInputStream());	
    }
}
