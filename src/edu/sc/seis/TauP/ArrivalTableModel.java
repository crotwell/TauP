package edu.sc.seis.TauP;

import javax.swing.table.AbstractTableModel;

/*
  The TauP Toolkit: Flexible Seismic Travel-Time and Raypath Utilities.
  Copyright (C) 1998-2000 University of South Carolina

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  as published by the Free Software Foundation; either version 2
  of the License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

  The current version can be found at 
  <A HREF="www.seis.sc.edu">http://www.seis.sc.edu</A>

  Bug reports and comments should be directed to 
  H. Philip Crotwell, crotwell@seis.sc.edu or
  Tom Owens, owens@seis.sc.edu

*/


/**
* @author H. Philip Crotwell
* @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001



*/
public class ArrivalTableModel extends AbstractTableModel {


    protected Arrival[] arrivals = new Arrival[0];

    public ArrivalTableModel() {

    }

    public int getRowCount() {
        return arrivals.length;
    }

    public int getColumnCount() {
        return 8;
    }

    public Object getValueAt(int row, int col) {
        switch (col) {
        case 0:
            return float8_1.form(arrivals[row].getModuloDistDeg());
	case 1:
	    return float8_1.form(arrivals[row].getModuloDist() * 
				 arrivals[row].getPhase().getTauModel().getRadiusOfEarth());
        case 2:
            return float8_1.form(arrivals[row].getSourceDepth());
        case 3:
            return arrivals[row].getName();
        case 4:
            return float8_2.form(arrivals[row].getTime());
        case 5:
            return float8_3.form(Math.PI/180.0*arrivals[row].getRayParam());
        case 6:
            return float8_1.form(arrivals[row].getDistDeg());
        case 7:
            return (arrivals[row].getName().equals(arrivals[row].getPuristName()) ? "=" : "*");
        case 8:
            return arrivals[row].getPuristName();
        default:
            return "";
        }
    }

    public String getColumnName(int col) {
        switch (col) {
        case 0:
            return "Dist";
        case 1:
	    return "Dist (km)";
	case 2:
            return "Depth";
        case 3:
            return "Name";
        case 4:
            return "Time";
        case 5:
            return "Ray Param";
        case 6:
            return "Purist Dist";
        case 7:
            return "";
        case 8:
            return "Purist Name";
        default:
            return "";
        }
    }

    public void setArrivals(Arrival[] arrivals) {
        this.arrivals = arrivals;
        fireTableDataChanged();
    }

    private static Format float6_1 = new Format("%6.1f");
    private static Format float8_1 = new Format("%8.1f");
    private static Format float8_2 = new Format("%8.2f");
    private static Format float8_3 = new Format("%8.3f");
    private static Format float8_4 = new Format("%8.4f");
}
