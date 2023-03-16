/*
 * The TauP Toolkit: Flexible Seismic Travel-Time and Raypath Utilities.
 * Copyright (C) 1998-2000 University of South Carolina
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 * 
 * The current version can be found at <A
 * HREF="www.seis.sc.edu">http://www.seis.sc.edu</A>
 * 
 * Bug reports and comments should be directed to H. Philip Crotwell,
 * crotwell@seis.sc.edu or Tom Owens, owens@seis.sc.edu
 * 
 */
package edu.sc.seis.TauP;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Convenience class that allows sac header variables to be associated with a
 * seismic phase name.
 * 
 * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001
 * 
 * 
 * 
 * @author H. Philip Crotwell
 * 
 */
public class PhaseName implements Serializable {

    /** name of the phase, ie PKIKP */
    public String name;

    /** sac t header to be associated with the phase */
    public int sacTNum = -1;

    /** list of sac t headers to be associated with the phase, including n triplications */
    public ArrayList<Integer> sacTNumTriplication = new ArrayList<Integer>();

    public PhaseName(String name) throws TauModelException {
        this.name = name;
        // check name is valid
        LegPuller.legPuller(name);
    }

    public PhaseName(String name, int sacTNum) throws TauModelException {
        this(name);
        this.sacTNum = sacTNum;
        sacTNumTriplication.add(sacTNum);
    }

    public PhaseName(String name, String sacTNumList) throws TauModelException {
        this(name);
        parseSacTNums(sacTNumList);
    }

    void parseSacTNums(String sacTNumList) throws TauModelException {
        for (int j = 0; j < sacTNumList.length(); j++) {
            char c = sacTNumList.charAt(j);
            int intForChar;
            if(Character.isDigit(c)) {
                /*
                 * There is an optional argument, so store it with the
                 * phase name.
                 */
                intForChar = Integer.parseInt(""+c);
            } else if(c == 'a') {
                /*
                 * There is an optional argument, use 10 for sac A, so
                 * store it with the phase name.
                 */
                intForChar = TauP_SetSac.A_HEADER;
            } else if(c == 'x') {
                /*
                 * There is an optional argument, use 11 for skipping a triplication.
                 */
                intForChar = TauP_SetSac.SKIP_HEADER;
            } else {
                throw new TauModelException("Problem with phase=" + name +
                        ", unknown SAC header TNum: "+c);
            }
            if (sacTNumTriplication.contains(intForChar)) {
                throw new TauModelException("SAC TNum is duplicated for phase "+name+", in "+sacTNumList);
            }
            sacTNumTriplication.add(intForChar);
        }
        if (sacTNumTriplication.size() > 0) {
            sacTNum = sacTNumTriplication.get(0);
        }
    }

    public boolean equals(PhaseName obj) {
        if(obj.name.equals(this.name) && obj.sacTNum == this.sacTNum) {
            return true;
        } else {
            return false;
        }
    }

    public String getName() {
        return name;
    }

    /**
     * Gets sac header for the index triplication. Zero is the first arrival, 1 is next arrival, etc.
     * Index zero should be same as this.sacTNum.
     *
     * @param index
     * @return index arrival for the phase
     */
    public int sacTNumForTriplication(int index) {
        return sacTNumTriplication.get(index);
    }

    public String toString() {
        return name;
    }
}
