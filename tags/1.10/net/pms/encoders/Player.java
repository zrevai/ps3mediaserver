/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.encoders;

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;

import javax.swing.JComponent;

import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAMediaAudio;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAMediaSubtitle;
import net.pms.formats.Format;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.util.FileUtil;
import net.pms.util.Iso639;

public abstract class Player {
	
	public static final int VIDEO_SIMPLEFILE_PLAYER = 0; 
	public static final int AUDIO_SIMPLEFILE_PLAYER = 1;
	public static final int VIDEO_WEBSTREAM_PLAYER = 2;
	public static final int AUDIO_WEBSTREAM_PLAYER = 3;
	public static final int MISC_PLAYER = 4;
	public static final String NATIVE = "NATIVE"; //$NON-NLS-1$

	public boolean avisynth() {
		return false;
	}
	
	public boolean excludeFormat(Format extension) {
		return false;
	}
	
	public abstract int purpose();
	public abstract JComponent config();
	public abstract String id();
	public abstract String name();
	public abstract int type();
	public abstract String [] args();
	public abstract String mimeType();
	public abstract String executable();
	public boolean isInternalSubtitlesSupported() {
		return true;
	}
	public boolean isExternalSubtitlesSupported() {
		return true;
	}
	public boolean isTimeSeekable() {
		return false;
	}
	public abstract ProcessWrapper launchTranscode(String fileName, DLNAMediaInfo media, OutputParams params) throws IOException;
	
	public String toString() {
		return name();
	}
	
	public void setAudioAndSubs(String fileName, DLNAMediaInfo media, OutputParams params, PmsConfiguration configuration) {
		if (params.aid == null && media != null) {
			// check for preferred audio
			StringTokenizer st = new StringTokenizer(configuration.getMencoderAudioLanguages(), ","); //$NON-NLS-1$
			while (st != null && st.hasMoreTokens()) {
				String lang = st.nextToken();
				for(DLNAMediaAudio audio:media.audioCodes) {
					if (audio.matchCode(lang)) {
						params.aid = audio;
						st = null;
						break;
					}
				}
			}
		}
		if (params.aid == null && media.audioCodes.size() > 0) {
			// take a default audio track, dts first if possible
			for(DLNAMediaAudio audio:media.audioCodes) {
				if (audio.isDTS()) {
					params.aid = audio;
					break;
				}
			}
			if (params.aid == null)
				params.aid = media.audioCodes.get(0);
		}
		
		String currentLang = null;
		String matchedSub = null;
		if (params.aid != null)
			currentLang = params.aid.lang;
		
		if (params.sid != null && params.sid.id == -1) {
			params.sid = null;
			return;
		}
		
		StringTokenizer st1 = new StringTokenizer(configuration.getMencoderAudioSubLanguages(), ";"); //$NON-NLS-1$
		while (st1.hasMoreTokens()) {
			String pair = st1.nextToken();
			if (pair.contains(",")) { //$NON-NLS-1$
				String audio = pair.substring(0, pair.indexOf(",")); //$NON-NLS-1$
				String sub = pair.substring(pair.indexOf(",")+1); //$NON-NLS-1$
				if (Iso639.isCodesMatching(audio, currentLang)) {
					matchedSub = sub;
					break;
				}
			}
		}
		
		if (matchedSub != null && matchedSub.equals("off")) { //$NON-NLS-1$
			params.sid = null;
			return;
		}
		
		
		
		if (!configuration.isMencoderDisableSubs() && params.sid == null && media != null) {
			// check for subtitles again
			File video = new File(fileName);
			FileUtil.doesSubtitlesExists(video, media);
			
			if (configuration.getUseSubtitles()) {
				// priority to external subtitles
				for(DLNAMediaSubtitle sub:media.subtitlesCodes) {
					if (sub.file != null) {
						params.sid = sub;
						break;
					}
				}
			}
			
			//
			if (params.sid == null) {
				StringTokenizer st = new StringTokenizer(configuration.getMencoderSubLanguages(), ","); //$NON-NLS-1$
				while (st != null && st.hasMoreTokens()) {
					String lang = st.nextToken();
					for(DLNAMediaSubtitle sub:media.subtitlesCodes) {
						if (sub.matchCode(lang) && !Iso639.isCodesMatching(lang, matchedSub)) {
							params.sid = sub;
							st = null;
							break;
						}
					}
				}
			}
		}
		
	}

}