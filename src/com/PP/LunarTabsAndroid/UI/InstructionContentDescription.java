package com.PP.LunarTabsAndroid.UI;

import android.util.Log;

public class InstructionContentDescription {
	
	public static String makeAccessibleInstruction(String instruction) {
		if(GUIDataModel.getInstance().isVerbose()) {
			if(instruction.indexOf("-") > -1) {
				instruction = instruction.replaceAll("-", "dash");
			}
		}
		else {
			if(instruction.indexOf("A")>-1) {
				instruction = instruction.replaceAll("A", "ae");
			}
		}
		return instruction;
	}

}
