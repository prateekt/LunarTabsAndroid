package com.PP.LunarTabsAndroid.Activities;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.herac.tuxguitar.song.models.TGBeat;
import org.herac.tuxguitar.song.models.TGSong;
import org.herac.tuxguitar.song.models.TGTrack;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.PP.AudioIcon.AudioIconAPI;
import com.PP.IntelliSeg.Abstract.Segment;
import com.PP.IntelliSeg.MeasureIncrementSegmenter.MeasureIncrementSegmenter;
import com.PP.LunarTabsAndroid.APIs.FileOpAPI;
import com.PP.LunarTabsAndroid.APIs.MediaPlayerAPI;
import com.PP.LunarTabsAndroid.APIs.TextToSpeechAPI;
import com.PP.LunarTabsAndroid.APIs.TuxGuitarUtil;
import com.PP.LunarTabsAndroid.APIs.VolumeAPI;
import com.PP.LunarTabsAndroid.APIs.WordActivatorAPI;
import com.PP.LunarTabsAndroid.Dialogs.GuitarFileLoaderDialog;
import com.PP.LunarTabsAndroid.Dialogs.MeasureIncrementDialog;
import com.PP.LunarTabsAndroid.Dialogs.MidiFollowingEnableDialog;
import com.PP.LunarTabsAndroid.Dialogs.PlaybackSpeedDialog;
import com.PP.LunarTabsAndroid.Dialogs.SelectSectionDialog;
import com.PP.LunarTabsAndroid.Dialogs.SetHomeDirectoryDialog;
import com.PP.LunarTabsAndroid.Dialogs.StomperEnableDialog;
import com.PP.LunarTabsAndroid.Dialogs.VoiceActionsDialog;
import com.PP.LunarTabsAndroid.InstrumentModels.ChordDB;
import com.PP.LunarTabsAndroid.InstrumentModels.ChordRecognizer;
import com.PP.LunarTabsAndroid.UI.AccListView;
import com.PP.LunarTabsAndroid.UI.DataModel;
import com.PP.LunarTabsAndroid.UI.InstructionContentDescription;
import com.PP.LunarTabsAndroid.UI.ResourceModel;
import com.PP.LunarTabsAndroid.UI.SerializedParams;
import com.PP.MidiServer.AbstractMidiServerActivity;
import com.PP.MidiServer.ChordRecognitionListener;
import com.PP.MidiServer.MidiServer;
import com.PP.StompDetector.InstructionStomp;
import com.PP.StompDetector.StompDetector;
import com.example.lunartabsandroid.R;
import com.root.gast.speech.activation.SpeechActivationListener;

public class MainActivity extends AbstractMidiServerActivity implements OnClickListener, SpeechActivationListener, ChordRecognitionListener  {
	
	//debug fags
	protected static final boolean MIDI_FOLLOWER_DEBUG = false;
	protected static final String FRAGMENT_MANAGER_TAG = "LunarTabs";

	//components
	protected Button loadTabFileButton;
	protected Button toggleModesButton;
	protected Button playSampleButton;
	protected Button prevMeasButton;
	protected Button nextMeasButton;
	protected Button upButton;
	protected Button downButton;
	protected Spinner trackChooser;
	protected AccListView instructionsList;
	
	//stomp detector
	protected static StompDetector stomper = null;	
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		//init stuff
		super.onCreate(savedInstanceState);
//		this.requestWindowFeature(Window.FEATURE_NO_TITLE);		
		setContentView(R.layout.activity_main);
	    this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);	
	    ResourceModel.getInstance().loadResources(this);
	    
		//load components
        loadTabFileButton = (Button) findViewById(R.id.loadTabFileButton);
        toggleModesButton = (Button) findViewById(R.id.toggleModesButton);
        playSampleButton = (Button) findViewById(R.id.playSampleButton);
        prevMeasButton = (Button) findViewById(R.id.prevMeasButton);
        nextMeasButton = (Button) findViewById(R.id.nextMeasButton);
        upButton = (Button) findViewById(R.id.upButton);
        downButton = (Button) findViewById(R.id.downButton);
        trackChooser = (Spinner) findViewById(R.id.trackChooser);
        instructionsList = (AccListView) findViewById(R.id.instructionsList);
                
        //register listeners
        if(!loadTabFileButton.hasOnClickListeners()) {
        	loadTabFileButton.setOnClickListener(this);
        }
        if(!toggleModesButton.hasOnClickListeners()) {
        	toggleModesButton.setOnClickListener(this);
        }
        if(!playSampleButton.hasOnClickListeners()) {
        	playSampleButton.setOnClickListener(this);
        }
        if(!prevMeasButton.hasOnClickListeners()) {
        	prevMeasButton.setOnClickListener(this);
        }
        if(!nextMeasButton.hasOnClickListeners()) {
        	nextMeasButton.setOnClickListener(this);
        }
        if(!upButton.hasOnClickListeners()) {
        	upButton.setOnClickListener(this);
        }
        if(!downButton.hasOnClickListeners()) {
        	downButton.setOnClickListener(this);
        }
        
        //colors
        loadTabFileButton.setBackgroundColor(Color.WHITE);
        loadTabFileButton.setTextColor(Color.BLACK);
        toggleModesButton.setBackgroundColor(Color.WHITE);
        toggleModesButton.setTextColor(Color.BLACK);
        playSampleButton.setBackgroundColor(Color.WHITE);
        playSampleButton.setTextColor(Color.BLACK);
        prevMeasButton.setBackgroundColor(Color.WHITE);
        prevMeasButton.setTextColor(Color.BLACK);
        nextMeasButton.setBackgroundColor(Color.WHITE);
        nextMeasButton.setTextColor(Color.BLACK);
        upButton.setBackgroundColor(Color.WHITE);
        upButton.setTextColor(Color.BLACK);
        downButton.setBackgroundColor(Color.WHITE);
        downButton.setTextColor(Color.BLACK);
        
        //init components
        int hilightColor = getResources().getColor(R.color.background_holo_light);        
        instructionsList.init(hilightColor,Color.WHITE);
        
        //set up segmenter
        if(DataModel.getInstance().getSegmenter()==null) {
        	DataModel.getInstance().setSegmenter(new MeasureIncrementSegmenter());
        }
        
        //enable APIs
        TextToSpeechAPI.init(this);
        
        //init data directory
        FileOpAPI.init();
        TuxGuitarUtil.cleanUp(FileOpAPI.SAVE_PATH);
        
        //Chord DB initialize
        ChordDB.getInstance();
        
        //init voice commands and restart if bundle requires
        String[] voiceCommands = ResourceModel.getInstance().voiceCommands;
        WordActivatorAPI.getInstance().init(voiceCommands, this);
        if(savedInstanceState!=null && savedInstanceState.containsKey(WordActivatorAPI.getInstance().toString())) {
        	boolean turnOn = savedInstanceState.getBoolean(WordActivatorAPI.getInstance().toString());
        	if(turnOn) {
          	   DataModel.getInstance().setVoiceActionsEnabled(true);        		
        	   WordActivatorAPI.getInstance().start();
        	}
        }
        
        //reinit stomper and restart if was on        
        if(stomper==null) {
        	stomper = new StompDetector(this);
        	stomper.addStompListener(new InstructionStomp(this));
        }
        stomper.setMainActivity(this);
        if(savedInstanceState!=null && savedInstanceState.containsKey(stomper.toString())) {
        	boolean turnOn = savedInstanceState.getBoolean(stomper.toString());
        	if(turnOn) {
        		stomper.start();
        	}
        }
        
        //init Midi Server and restart if bundle requires
        MidiServer.getInstance().clearChordRecognitionListeners();
        MidiServer.getInstance().addChordRecognitionListener(this);
        if(savedInstanceState!=null && savedInstanceState.containsKey(MidiServer.getInstance().toString())) {
        	boolean turnOn = savedInstanceState.getBoolean(MidiServer.getInstance().toString());
        	if(turnOn) {
        		MidiServer.getInstance().start();
        	}
        }
        
        //init Audio Icon
        AudioIconAPI.getInstance().init(this);
        
        //set application volume
//        VolumeAPI.getInstance().init(this);
 //       VolumeAPI.getInstance().setVolume(VolumeAPI.DEFAULT_VOLUME_FACTOR);
                        
	}
		
	@Override
	public void onStop() {
		
		//call on stop functions
		super.onStop();		
		WordActivatorAPI.getInstance().onStop();
		stomper.onStop();
		MidiServer.getInstance().onStop();
		
		//clean up and save
		TuxGuitarUtil.cleanUp(FileOpAPI.SAVE_PATH);
		DataModel.getInstance().saveInstance();
		SerializedParams.getInstance().saveInstance();
	}
	
	@Override
	public void onResume() {
		
		//call on resume functions (if not already running)
		super.onResume();	
		if(!DataModel.getInstance().isVoiceActionsEnabled()) {
			WordActivatorAPI.getInstance().onResume();		
		}
		if(!stomper.isEnabled()) {
			stomper.onResume();
		}
		if(!MidiServer.getInstance().isRunning()) {
			MidiServer.getInstance().onResume();
		}
		
		//reinit GUI from file (if exists)
        refreshGUI();		
        
        //garbage collect
        System.gc();
	}
		
	/**
	 * Refresh GUI based on current data model (either from file or in memory).
	 */
	public void refreshGUI() {
		DataModel dataModel = DataModel.getInstance();
		int prevInstSel = dataModel.getSelectedInstructionIndex();
		if(dataModel.getFileName()!=null && !dataModel.getFileName().trim().equals("")) {
			this.setTitle(dataModel.getFileName().trim());
		}
		if(dataModel.getSong()!=null && dataModel.getTracksList()!=null) {
			populateTrackOptions(dataModel.getTracksList(),dataModel.getTrackNum());
		}
		if(dataModel.getCurrentSegment()!=-1 && dataModel.getInstSegments()!=null) {
			
			//populate instructions
			Segment c_seg = dataModel.getInstSegments().get(dataModel.getCurrentSegment());			
			if(dataModel.isVerbose()) {
				populateInstructionPane(c_seg.getSfInst());					
			}
			else {
				populateInstructionPane(c_seg.getChordInst());
			}
			
			//re-enable highlighting and instruction selected
			instructionsList.setHilightEnabled(true);
			if(prevInstSel!=-1) {
				dataModel.setSelectedInstructionIndex(prevInstSel);
			}
			
			//set title
			this.setTitle(dataModel.getFileName().trim() + " " + c_seg.getTitlePresentation());			
		}
		
		//refresh instructions list
		instructionsList.refreshGUI();
	}

	@Override
	public void onClick(View v) {
		
		//stop media player
		MediaPlayerAPI.getInstance().stop();
		
		//handle button press
		if(v.getId()==loadTabFileButton.getId()) {
			showLoadFileDialog();
		}
		else if(v.getId()==toggleModesButton.getId()) {
			toggleModes();
		}
		else if(v.getId()==playSampleButton.getId()) {
			playSample();
		}		
		else if(v.getId()==prevMeasButton.getId()) {
			prevMeasure();
		}
		else if(v.getId()==nextMeasButton.getId()) {
			nextMeasure();
		}
		else if(v.getId()==upButton.getId()) {
			prevInstruction();
		}		
		else if(v.getId()==downButton.getId()) {
			nextInstruction();
		}				
	}
	
	public void prevInstruction() {
		DataModel dataModel = DataModel.getInstance();
		if(dataModel.getFilePath()!=null && dataModel.getSong()!=null &&
		dataModel.getCurrentSegment()>=0 && dataModel.getTrackNum()>=0 && 
		dataModel.getInstSegments()!=null && dataModel.getInstSegments().size()>0 &&
		dataModel.getCurrentSegment()>=0 &&
		dataModel.getInstSegments().get(dataModel.getCurrentSegment()).getChordInst().size()>0) {
			
			//update index and perform click
      	  	int selectedInstructionIndex = DataModel.getInstance().getSelectedInstructionIndex();			
			if(selectedInstructionIndex >= 0) {
				
				//decrement instruction index
				selectedInstructionIndex--;
				
				//perform click on GUI
				instructionsList.programmaticSelect(selectedInstructionIndex);
				
				//find and read instruction
				if(selectedInstructionIndex >= 0) {
					String c_inst = null;
					Segment cSeg = dataModel.getInstSegments().get(dataModel.getCurrentSegment());				
					if(dataModel.isVerbose()) {
						c_inst = cSeg.getSfInst().get(selectedInstructionIndex);
					}
					else {
						c_inst = cSeg.getChordInst().get(selectedInstructionIndex);					
					}
					if(c_inst!=null) {
						TextToSpeechAPI.speak(
								InstructionContentDescription.makeAccessibleInstruction(c_inst));
					}
				}
			}
			else {
				
				//no previous instruction
	    		TextToSpeechAPI.speak(ResourceModel.getInstance().ERROR_NO_PREV_INST);				
			}			
		}
    	else if(dataModel.getInstSegments()!=null && dataModel.getInstSegments().size()==0) {

    		//no data in section
    		TextToSpeechAPI.speak(ResourceModel.getInstance().ERROR_NO_DATA);
    	}
    	else {
    		TextToSpeechAPI.speak(ResourceModel.getInstance().ERROR_NO_FILE_LOADED);
    	}				
	}
	
	public void nextInstruction() {
		DataModel dataModel = DataModel.getInstance();
		if(dataModel.getFilePath()!=null && dataModel.getSong()!=null &&
		dataModel.getCurrentSegment()>=0 && dataModel.getTrackNum()>=0 && 
		dataModel.getInstSegments()!=null && dataModel.getInstSegments().size()>0 &&
		dataModel.getCurrentSegment()>=0) {
			
			//update index and perform click
			Segment cSeg = dataModel.getInstSegments().get(dataModel.getCurrentSegment());
			int numInst = cSeg.getChordInst().size();
      	  	int selectedInstructionIndex = DataModel.getInstance().getSelectedInstructionIndex();			
			if(selectedInstructionIndex < (numInst-1)) {
				
				//increment instruction
				selectedInstructionIndex++;
				
				//perform click on GUI
				instructionsList.programmaticSelect(selectedInstructionIndex);
				
				//find and read instruction
				String c_inst = null;
				if(dataModel.isVerbose()) {
					c_inst = cSeg.getSfInst().get(selectedInstructionIndex);
				}
				else {
					c_inst = cSeg.getChordInst().get(selectedInstructionIndex);					
				}
				if(c_inst!=null) {
					TextToSpeechAPI.speak(
							InstructionContentDescription.makeAccessibleInstruction(c_inst));
				}
				
			}
			else {
				//no next instruction
	    		TextToSpeechAPI.speak(ResourceModel.getInstance().ERROR_NO_NEXT_INST);				
			}			
		}
    	else if(dataModel.getInstSegments()!=null && dataModel.getInstSegments().size()==0) {
    		//no data in section
    		TextToSpeechAPI.speak(ResourceModel.getInstance().ERROR_NO_DATA);
    	}
    	else {
    		TextToSpeechAPI.speak(ResourceModel.getInstance().ERROR_NO_FILE_LOADED);
    	}				
	}
	
	public void playSample() {	
		DataModel dataModel = DataModel.getInstance();
		if(dataModel.getFilePath()!=null && dataModel.getSong()!=null && dataModel.getCurrentSegment()>=0 && dataModel.getTrackNum()>=0 && dataModel.getInstSegments()!=null && dataModel.getInstSegments().size()>0) {
			Segment cSeg = dataModel.getInstSegments().get(dataModel.getCurrentSegment());
			cSeg.play();
		}
    	else if(dataModel.getInstSegments()!=null && dataModel.getInstSegments().size()==0) {
    		TextToSpeechAPI.speak(ResourceModel.getInstance().ERROR_NO_DATA);
    	}
    	else {
    		TextToSpeechAPI.speak(ResourceModel.getInstance().ERROR_NO_FILE_LOADED);
    	}		
	}
	
	public void toggleModes() {
		DataModel dataModel=  DataModel.getInstance();
		if(dataModel.getFilePath()!=null && dataModel.getSong()!=null && dataModel.getCurrentSegment()>=0 && dataModel.getTrackNum()>=0) {		
			if(!dataModel.isOnPercussionTrack()) {
				if(dataModel.isVerbose()) {
					
					//populate instructions
					populateInstructionPane(dataModel.getInstSegments().get(dataModel.getCurrentSegment()).getChordInst());
					
					//flip stored flag
					dataModel.setVerbose(false);										
					
					//read currently selected instruction
					if(dataModel.getSelectedInstructionIndex() >= 0) {
						instructionsList.programmaticSelect(dataModel.getSelectedInstructionIndex());
						List<String> inst = dataModel.getInstSegments().get(dataModel.getCurrentSegment()).getChordInst();
						String instr = inst.get(dataModel.getSelectedInstructionIndex());
						TextToSpeechAPI.speak(
								InstructionContentDescription.makeAccessibleInstruction(instr));
					}
										
				}
				else {
					
					//populate instructions
					populateInstructionPane(dataModel.getInstSegments().get(dataModel.getCurrentSegment()).getSfInst());

					//flip stored flag
					dataModel.setVerbose(true);
					
					//read currently selected instruction
					if(dataModel.getSelectedInstructionIndex() >= 0) {
						instructionsList.programmaticSelect(dataModel.getSelectedInstructionIndex());						
						List<String> inst = dataModel.getInstSegments().get(dataModel.getCurrentSegment()).getSfInst();
						String instr = inst.get(dataModel.getSelectedInstructionIndex());
						TextToSpeechAPI.speak(
								InstructionContentDescription.makeAccessibleInstruction(instr));
					}					
				}
			}
		}
    	else if(dataModel.getInstSegments()!=null && dataModel.getInstSegments().size()==0) {
    		TextToSpeechAPI.speak(ResourceModel.getInstance().ERROR_NO_DATA);
    	}
    	else {
    		TextToSpeechAPI.speak(ResourceModel.getInstance().ERROR_NO_FILE_LOADED);
    	}		
	}
	
	public void nextMeasure() {
		DataModel dataModel = DataModel.getInstance();
		if(dataModel.getSong()!=null && dataModel.getInstSegments()!=null && dataModel.getCurrentSegment() < (dataModel.getInstSegments().size()-1)) {
			dataModel.setCurrentSegment(dataModel.getCurrentSegment()+1);
			if(dataModel.isVerbose()) {
				populateInstructionPane(dataModel.getInstSegments().get(dataModel.getCurrentSegment()).getSfInst());
			}
			else {
				populateInstructionPane(dataModel.getInstSegments().get(dataModel.getCurrentSegment()).getChordInst());				
			}
			DataModel.getInstance().clearSelectedInstructionIndex();
			instructionsList.refreshGUI();
			Segment c_seg = dataModel.getInstSegments().get(dataModel.getCurrentSegment());				
			this.setTitle(dataModel.getFileName().trim() + " " + c_seg.getTitlePresentation());			
		}
		else if(dataModel.getSong()!=null && dataModel.getInstSegments()!=null && dataModel.getCurrentSegment() == (dataModel.getInstSegments().size()-1)) {
			TextToSpeechAPI.speak(ResourceModel.getInstance().ERROR_LAST_SECTION);
		}
    	else if(dataModel.getInstSegments()!=null && dataModel.getInstSegments().size()==0) {
    		TextToSpeechAPI.speak(ResourceModel.getInstance().ERROR_NO_DATA);
    	}
    	else {
    		TextToSpeechAPI.speak(ResourceModel.getInstance().ERROR_NO_FILE_LOADED);
    	}		
	}
	
	public void prevMeasure() {
		DataModel dataModel = DataModel.getInstance();
		if(dataModel.getSong()!=null && dataModel.getInstSegments()!=null && dataModel.getInstSegments().size()>0 && dataModel.getCurrentSegment() > 0) {
			dataModel.setCurrentSegment(dataModel.getCurrentSegment()-1);
			if(dataModel.isVerbose()) {
				populateInstructionPane(dataModel.getInstSegments().get(dataModel.getCurrentSegment()).getSfInst());				
			}
			else {
				populateInstructionPane(dataModel.getInstSegments().get(dataModel.getCurrentSegment()).getChordInst());								
			}
			DataModel.getInstance().clearSelectedInstructionIndex();
			instructionsList.refreshGUI();			
			Segment c_seg = dataModel.getInstSegments().get(dataModel.getCurrentSegment());							
			this.setTitle(dataModel.getFileName().trim() + " " + c_seg.getTitlePresentation());			
		}
		else if(dataModel.getSong()!=null && dataModel.getInstSegments()!=null && dataModel.getCurrentSegment() == 0) {
			TextToSpeechAPI.speak(ResourceModel.getInstance().ERROR_FIRST_SECTION);
		}
    	else if(dataModel.getInstSegments()!=null && dataModel.getInstSegments().size()==0) {
    		TextToSpeechAPI.speak(ResourceModel.getInstance().ERROR_NO_DATA);
    	}
    	else {
    		TextToSpeechAPI.speak(ResourceModel.getInstance().ERROR_NO_FILE_LOADED);
    	}
	}
	
	public void loadInstructions() {
		
		//get model
		DataModel dataModel = DataModel.getInstance();
		
		//generate instructions
		dataModel.genInstructions();
		
		//populate instructions pane with current measure
		if(dataModel.getCurrentSegment() < dataModel.getInstSegments().size()) {
			if(dataModel.isVerbose()) {
				populateInstructionPane(dataModel.getInstSegments().get(dataModel.getCurrentSegment()).getSfInst());				
			}
			else {
				populateInstructionPane(dataModel.getInstSegments().get(dataModel.getCurrentSegment()).getChordInst());								
			}
		}
		
		//bug fix -- if out of bounds, reset back to start
		if(dataModel.getCurrentSegment() >= dataModel.getInstSegments().size()) {
			dataModel.setCurrentSegment(0);
		}
		
		//display
		Segment c_seg = dataModel.getInstSegments().get(dataModel.getCurrentSegment());							
		this.setTitle(dataModel.getFileName().trim() + " " + c_seg.getTitlePresentation());					
	}
	
	public void populateInstructionPane(List<String> instructions) {
    	ArrayAdapter<String> a_opts = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,instructions);
    	instructionsList.setAdapter(a_opts);
 	}
	
	public void showLoadFileDialog() {
		GuitarFileLoaderDialog dialog = new GuitarFileLoaderDialog(this,this);
		dialog.show();
	}
	
	public void showHomeDirectoryDialog() {
		SetHomeDirectoryDialog dialog = new SetHomeDirectoryDialog(this,this);
		dialog.show();
	}
	
	public void populateTrackOptions(List<String> tracksList, int start_sel_position) {
    	ArrayAdapter<String> a_opts = new ArrayAdapter<String>(this, R.layout.my_spinner,tracksList);
    	trackChooser.setAdapter(a_opts);
    	trackChooser.setSelection(start_sel_position);
    	trackChooser.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				DataModel.getInstance().setTrackNum(arg2);
				loadInstructions();				
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
    		
    	});
	}
		
    public void createTrackOptions() {
    	
    	//load data model
    	DataModel dataModel = DataModel.getInstance();
    	
        //populate options in list. Avoid duplicates.
        ArrayList<String> tracksList = new ArrayList<String>();
        Map<String,Integer> tracksDD = new HashMap<String,Integer>();
        Set<String> multipleEntries = new HashSet<String>();
        TGSong songLoaded = dataModel.getSong();
        if(songLoaded!=null && songLoaded.countTracks() > 0) {
        	for(int x=0; x < songLoaded.countTracks(); x++) {
        		TGTrack track = songLoaded.getTrack(x);
        		int offset = track.getOffset();
        		String capoStr = "";
        		if(offset!=0) {
        			String CAPO = ResourceModel.getInstance().CAPO;
        			capoStr = " ["+CAPO+" "+offset+"]";
        		}
        		String trackHash = track.getName().trim().toLowerCase() + capoStr;
        		if(tracksDD.containsKey(trackHash)) {
        			int newCnt = tracksDD.get(trackHash) + 1;
        			tracksDD.put(trackHash, newCnt);
        			multipleEntries.add(trackHash);
        		}
        		else {
        			tracksDD.put(trackHash, 1);
        		}
        	}
        	for(int x=(songLoaded.countTracks()-1); x>=0; x--) {
        		TGTrack track = songLoaded.getTrack(x);
        		int offset = track.getOffset();
        		String capoStr = "";
        		if(offset!=0) {
        			String CAPO = ResourceModel.getInstance().CAPO;
        			capoStr = " ["+CAPO+" "+offset+"]";
        		}
        		String trackHash = track.getName().trim().toLowerCase() + capoStr;
        		String trackName = track.getName().trim() + capoStr;
        		if(multipleEntries.contains(trackHash)) {
        			tracksList.add(0,trackName + " (" + tracksDD.get(trackHash) + ")");
        			tracksDD.put(trackHash, tracksDD.get(trackHash)-1);
        		}
        		else {
        			tracksList.add(0,trackName);        			
        		}
        	}
        }
        
        //populate in GUI
        populateTrackOptions(tracksList,0);
        
    	//store
    	DataModel.getInstance().setTracksList(tracksList);
    }	
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }   
    
    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
		//set title of menu buttons
		if(stomper.isEnabled()) {
			MenuItem stompModeMenuItem = menu.findItem(R.id.StompModeMenuItem);
			if(stompModeMenuItem!=null) {
				stompModeMenuItem.setTitle(ResourceModel.getInstance().DISABLE_STOMP_MODE);						
			}
		}
		else {
			MenuItem stompModeMenuItem = menu.findItem(R.id.StompModeMenuItem);
			if(stompModeMenuItem!=null) {
				stompModeMenuItem.setTitle(ResourceModel.getInstance().ENABLE_STOMP_MODE);						
			}
		}
    	if(DataModel.getInstance().isVoiceActionsEnabled()) {
			MenuItem voiceActionsMenuItem = menu.findItem(R.id.VoiceActionsMenuItem);
			if(voiceActionsMenuItem!=null) {
				voiceActionsMenuItem.setTitle(ResourceModel.getInstance().DISABLE_VOICE_ACTIONS);										
			}
    	}
    	else {
			MenuItem voiceActionsMenuItem = menu.findItem(R.id.VoiceActionsMenuItem);			
			if(voiceActionsMenuItem!=null) {
				voiceActionsMenuItem.setTitle(ResourceModel.getInstance().ENABLE_VOICE_ACTIONS);										
			}
    	}    	
    	if(MidiServer.getInstance().isRunning()) {
			MenuItem voiceActionsMenuItem = menu.findItem(R.id.MidiFollowingMenuItem);
			if(voiceActionsMenuItem!=null) {
				voiceActionsMenuItem.setTitle(ResourceModel.getInstance().DISABLE_MIDI_FOLLOWING);										
			}
    	}
    	else {
			MenuItem voiceActionsMenuItem = menu.findItem(R.id.MidiFollowingMenuItem);
			if(voiceActionsMenuItem!=null) {
				voiceActionsMenuItem.setTitle(ResourceModel.getInstance().ENABLE_MIDI_FOLLOWING);										
			}
    	}    	    	
        return true;    	
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	
    	//stop media player
    	MediaPlayerAPI.getInstance().stop();
    	
        // Handle item selection
        switch (item.getItemId()) {
        	case R.id.SetHomeDirMenuItem:
        		showHomeDirectoryDialog();
        		return true;
        	case R.id.SecIncMenuItem:
            	showSelectIncDialog();
                return true;
            case R.id.GoToMenuItem:
            	showSelectSectionDialog();
                return true;
            case R.id.PlaybackSpeedMenuItem:
            	showSetPlaybackSpeedDialog();
            	return true;
            case R.id.StompModeMenuItem:
            	stompModeDialog(item);
            	return true;
            case R.id.VoiceActionsMenuItem:
            	voiceActionsDialog(item);
            	return true;
            case R.id.CalibStompModeMenuItem:
            	calibrateStompMode();
            	return true;
            case R.id.MidiFollowingMenuItem:
            	midiFollowingDialog(item);
            	return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    public void midiFollowingDialog(MenuItem item) {
    	
    	//tab file must be loaded for stomper
    	DataModel dataModel = DataModel.getInstance();
		if(dataModel.getSong()!=null && dataModel.getTrackNum() >=0) {
    	
	    	//enable midi follower if not active
	    	if(!MidiServer.getInstance().isRunning()) {
	    		
	    		//ask if enable midi follower
				final Dialog dialog = new MidiFollowingEnableDialog(this,this,item);
				dialog.show();
								
	    	}	    	
	    	else if(MidiServer.getInstance().isRunning()) {
	    		
	    		//stop midi follower
	    		MidiServer.getInstance().stop();
	    		
	    		//change text on menu item
				item.setTitle(ResourceModel.getInstance().ENABLE_MIDI_FOLLOWING);
	    	}
	    	
		}
		else {
			TextToSpeechAPI.speak(ResourceModel.getInstance().ERROR_NO_FILE_LOADED);
		} 
	}
    
    public void calibrateStompMode() {
    	
    	//stop stomper and voice actions
    	stomper.onStop();
    	WordActivatorAPI.getInstance().onStop();
    	
    	//start new activity
		Intent i = new Intent(this, StomperCalibActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(i);												
    }
    
    public void voiceActionsDialog(MenuItem menuItem) {
    	if(!DataModel.getInstance().isVoiceActionsEnabled()) {
    		
    		//show dialog for voice actions
        	VoiceActionsDialog m = new VoiceActionsDialog(menuItem);
        	m.show(getFragmentManager(), FRAGMENT_MANAGER_TAG);   		
        	
    	}
    	else {
    		
    		//stop voice actions
     	   DataModel.getInstance().setVoiceActionsEnabled(false);
     	   WordActivatorAPI.getInstance().stopListening();
    		
    		//relabel menu item
     	   menuItem.setTitle(ResourceModel.getInstance().ENABLE_VOICE_ACTIONS);
    		
    	}
    }
    
    public void stompModeDialog(MenuItem item) {
    	
    	//tab file must be loaded for stomper
    	DataModel dataModel = DataModel.getInstance();
		if(dataModel.getSong()!=null && dataModel.getTrackNum() >=0) {
    	
	    	//enable stomper if not active
	    	if(!stomper.isEnabled()) {
	    		
	    		//show stomper enabled dialog
				final Dialog dialog = new StomperEnableDialog(this,stomper,this,item);
				dialog.show();	    		    		
				
				
	    	}	    	
	    	else if(stomper.isEnabled()) {
	    		
	    		//stop stomper
	    		stomper.stop();
	    		
	    		//change text on menu item
				item.setTitle(ResourceModel.getInstance().ENABLE_STOMP_MODE);					    		
	    	}
	    	
		}
		else {
			TextToSpeechAPI.speak(ResourceModel.getInstance().ERROR_NO_FILE_LOADED);
		}
    }
    
    public void showSelectIncDialog() {
    	DataModel dataModel = DataModel.getInstance();    	
    	if(dataModel.getSong()!=null && dataModel.getInstSegments()!=null && dataModel.getTrackNum()!=-1 && dataModel.getCurrentSegment()!=-1 && dataModel.getInstSegments()!=null) {    	
    		MeasureIncrementDialog m = new MeasureIncrementDialog(this);
    		m.show(getFragmentManager(), FRAGMENT_MANAGER_TAG);    	
    	}
    	else if(dataModel.getInstSegments()!=null && dataModel.getInstSegments().size()==0) {
    		TextToSpeechAPI.speak(ResourceModel.getInstance().ERROR_NO_DATA);
    	}
    	else {
    		TextToSpeechAPI.speak(ResourceModel.getInstance().ERROR_NO_FILE_LOADED);
    	}
    }
    
    public void showSetPlaybackSpeedDialog() {
    	PlaybackSpeedDialog m = new PlaybackSpeedDialog(this);
    	m.show(getFragmentManager(), FRAGMENT_MANAGER_TAG);
    }
    
    public void showSelectSectionDialog() {
    	DataModel dataModel = DataModel.getInstance();
    	if(dataModel.getSong()!=null && dataModel.getInstSegments()!=null && dataModel.getTrackNum()!=-1 && dataModel.getCurrentSegment()!=-1 && dataModel.getInstSegments()!=null) {
        	SelectSectionDialog m = new SelectSectionDialog(this);
        	m.show(getFragmentManager(), FRAGMENT_MANAGER_TAG);    		
    	}
    	else if(dataModel.getInstSegments()!=null && dataModel.getInstSegments().size()==0) {
    		TextToSpeechAPI.speak(ResourceModel.getInstance().ERROR_NO_DATA);
    	}
    	else {
    		TextToSpeechAPI.speak(ResourceModel.getInstance().ERROR_NO_FILE_LOADED);
    	}
    }
    
    public void playAudioIcon() {
    	DataModel dataModel = DataModel.getInstance();
    	if(dataModel.getSong()!=null && 
    			dataModel.getInstSegments()!=null && dataModel.getTrackNum()!=-1 && 
    			dataModel.getCurrentSegment()!=-1 && dataModel.getInstSegments()!=null
    			&& dataModel.getSelectedInstructionIndex()!=-1) {
    			
    			//get beat and play (if not on percussion track)
    			if(!dataModel.isOnPercussionTrack()) {
	    			Segment c_seg = dataModel.getInstSegments().get(dataModel.getCurrentSegment());
	    			List<TGBeat> beats = c_seg.getBeats();
	    			TGBeat beat = beats.get(dataModel.getSelectedInstructionIndex());
	    			if(beat!=null) {
	    				AudioIconAPI.getInstance().playBeatAudioIcon(beat);
	    			}
    			}
    			
    	}
    	else if(dataModel.getInstSegments()!=null && dataModel.getInstSegments().size()==0) {
    		TextToSpeechAPI.speak(ResourceModel.getInstance().ERROR_NO_DATA);
    	}
    	else if(dataModel.getInstSegments()!=null && dataModel.getInstSegments().size()!=0 &&
    			dataModel.getCurrentSegment()>=0 && dataModel.getTrackNum()>=0 && dataModel.getSelectedInstructionIndex()==-1) {
    		TextToSpeechAPI.speak(ResourceModel.getInstance().ERROR_NO_INST_SELECTED);
    	}    	
    	else {
    		TextToSpeechAPI.speak(ResourceModel.getInstance().ERROR_NO_FILE_LOADED);
    	}
    	
    }
    
    /**
     * Voice Activator callback
     */
	@Override
	public void activated(boolean success, String wordHeard) {
		
		//stop media player
		MediaPlayerAPI.getInstance().stop();
		
		//handle activation
		Log.d("ACTIVATED", wordHeard);
		String[] VOICE_COMMANDS = ResourceModel.getInstance().voiceCommands;
		if(wordHeard.equalsIgnoreCase(VOICE_COMMANDS[0])) {
			this.toggleModesButton.performClick();
		}
		else if(wordHeard.equalsIgnoreCase(VOICE_COMMANDS[1])) {
			this.playSampleButton.performClick();
		}
		else if(wordHeard.equalsIgnoreCase(VOICE_COMMANDS[2])) {
			this.nextMeasButton.performClick();
		}
		else if(wordHeard.equalsIgnoreCase(VOICE_COMMANDS[3])) {
			this.prevMeasButton.performClick();
		}
		else if(wordHeard.equalsIgnoreCase(VOICE_COMMANDS[4])) {
			this.upButton.performClick();
		}
		else if(wordHeard.equalsIgnoreCase(VOICE_COMMANDS[5])) {
			this.downButton.performClick();
		}
		else if(wordHeard.equalsIgnoreCase(VOICE_COMMANDS[6])) {
			playAudioIcon();
		}		
	}

	/**
	 * @return the instructionsList
	 */
	public AccListView getInstructionsList() {
		return instructionsList;
	}
	
	@Override
	public void chordRecognized(final String chord) {    
				
		//get chord hash
		final String chordHash = ChordRecognizer.getChordHash(chord);
		
		//match
		final DataModel dataModel = DataModel.getInstance();
		if(dataModel.getSong()!=null && 
			dataModel.getInstSegments()!=null && dataModel.getTrackNum()!=-1 && 
			dataModel.getCurrentSegment()!=-1 && dataModel.getInstSegments()!=null
			&& dataModel.getSelectedInstructionIndex()!=-1) {
			
			//see if chord hash matches target
			final Segment seg = dataModel.getInstSegments().get(dataModel.getCurrentSegment());
			final String target = seg.getMatchTargets().get(dataModel.getSelectedInstructionIndex());
			if(target.equals(chordHash) || ChordRecognizer.robustMidiMatch(chordHash, target)) {
	       
				//play success track
				if(MIDI_FOLLOWER_DEBUG) {
					this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(MainActivity.this, "Success: " + target, Toast.LENGTH_SHORT).show();
						}
					});
				}
				
				//update gui for next available index
				this.updateGUIForNextAvailableIndex();
				
			}
			else {
				
				//play buzzer
				if(MIDI_FOLLOWER_DEBUG) {
					this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(MainActivity.this, "Failure: " + chordHash + " ::: " + target, Toast.LENGTH_SHORT).show();
						}
					});
				}
			}
		}    
	}
	
	public boolean updateToNextAvailableIndex() {
		
		//get data model
		DataModel dataModel = DataModel.getInstance();		

		//increment to next available one or say end of track if not anymore.
		int initSeg = dataModel.getCurrentSegment();
		int segCtr = dataModel.getCurrentSegment();
		int instCtr = dataModel.getSelectedInstructionIndex()+1;
		outer:while(segCtr < dataModel.getInstSegments().size()) {
			if(segCtr >= 0) {
				Segment seg = dataModel.getInstSegments().get(segCtr);			
				while(instCtr < seg.getMatchTargets().size()) {
					if(instCtr >= 0) {
						String newTarget = seg.getMatchTargets().get(instCtr);
						if(!newTarget.equals("")) {
							dataModel.setCurrentSegment(segCtr);
							dataModel.setSelectedInstructionIndex(instCtr);
							break outer;
						}
					}
					instCtr++;					
				}
			}
			segCtr++;
			instCtr=0;
		}
		
		//if chose end of track, just set to last instruction.
		if(segCtr==dataModel.getInstSegments().size()) {
			dataModel.setCurrentSegment(segCtr-1);
			dataModel.setSelectedInstructionIndex(dataModel.getInstSegments().get(dataModel.getCurrentSegment()).getSfInst().size()-1);
		}
		
		return (initSeg!=segCtr);
	}
	
	public void updateGUIForNextAvailableIndex() {

		//update to next index
		final boolean segChanged = updateToNextAvailableIndex();

		//refresh gui
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				
				//update gui and perform programmatic selection
				int temp = DataModel.getInstance().getSelectedInstructionIndex();
				if(segChanged) {
					DataModel.getInstance().clearSelectedInstructionIndex();
					MainActivity.this.refreshGUI();				
				}
				MainActivity.this.instructionsList.programmaticSelect(temp);

				//find and read instruction using tts
				String c_inst = null;
				final DataModel dataModel = DataModel.getInstance();	
				final Segment seg = dataModel.getInstSegments().get(dataModel.getCurrentSegment());				
				if(dataModel.isVerbose()) {
					c_inst = seg.getSfInst().get(DataModel.getInstance().getSelectedInstructionIndex());
				}
				else {
					c_inst = seg.getChordInst().get(DataModel.getInstance().getSelectedInstructionIndex());          
				}
				if(c_inst!=null) {
					TextToSpeechAPI.speak(
							InstructionContentDescription.makeAccessibleInstruction(c_inst));
				}
			}
		});
	}
	
	@Override
	public void onSaveInstanceState(Bundle bundle) {
		bundle.putBoolean(stomper.toString(), stomper.isEnabled());
		bundle.putBoolean(WordActivatorAPI.getInstance().toString(), DataModel.getInstance().isVoiceActionsEnabled());
		bundle.putBoolean(MidiServer.getInstance().toString(), MidiServer.getInstance().isRunning());
	}

	/**
	 * @return the downButton
	 */
	public Button getDownButton() {
		return downButton;
	}	
}