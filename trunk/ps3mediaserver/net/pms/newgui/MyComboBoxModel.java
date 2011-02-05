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
package net.pms.newgui;

import javax.swing.DefaultComboBoxModel;

public class MyComboBoxModel extends DefaultComboBoxModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9094365556516842551L;

	public MyComboBoxModel() {
		super();
		// TODO Auto-generated constructor stub
	}

	public MyComboBoxModel(Object[] items) {
		super(items);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Object getElementAt(int index) {
		String s = (String) super.getElementAt(index);
		//if (s.startsWith("[")) {
		//	s = s.substring(s.lastIndexOf("]")).trim();
		//}
		return s;
	}
	
	

}
