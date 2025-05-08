package edu.sc.seis.TauP;

class BranchDescription {
    String name;
    String updown;
    String type;
    String branch_desc;
    int[] branches;
    float[] depths;
    String then;

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" goes ").append(updown).append(" as a ").append(type).append(" in ").append(branch_desc);
        for (int i : branches) {
            sb.append(" ").append(i);
        }
        sb.append(" depths: ");
        for (float f : depths) {
            sb.append(" ").append(f);
        }
        sb.append(" then ").append(then);
        return sb.toString();
    }
}
