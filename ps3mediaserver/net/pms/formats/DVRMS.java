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
package net.pms.formats;

import java.util.ArrayList;

import net.pms.PMS;

import net.pms.encoders.MEncoderVideo;
import net.pms.encoders.Player;


public class DVRMS extends Format {
	
	
	@Override
	public ArrayList<Class<? extends Player>> getProfiles() {
		ArrayList<Class<? extends Player>> a = new ArrayList<Class<? extends Player>>();
		for(String engine:PMS.get().getEnginesAsList()) {
			if (engine.equals(MEncoderVideo.ID))
				a.add(MEncoderVideo.class);
			/*if (engine.equals(FFMpegVideoRemux.ID))
				a.add(FFMpegVideoRemux.class);*/
		}
		return a;
	}

	@Override
	public boolean transcodable() {
		return true;
	}

	public DVRMS() {
		type = VIDEO;
	}

	@Override
	public String[] getId() {
		return new String [] { "dvr-ms" };
	}

	@Override
	public boolean ps3compatible() {
		return false;
	}

}
