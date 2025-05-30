package edu.sc.seis.TauP.cmdline.args;

import edu.sc.seis.TauP.Alert;
import edu.sc.seis.TauP.PhaseName;
import edu.sc.seis.TauP.PhaseParseException;
import edu.sc.seis.TauP.TauPConfig;
import edu.sc.seis.TauP.cmdline.TauP_AbstractPhaseTool;
import picocli.CommandLine;

import java.io.FileReader;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.List;

public class PhaseArgs {


    public static final String DEFAULT_PHASES = "p,s,P,S,Pn,Sn,PcP,ScS,Pdiff,Sdiff,PKP,SKS,PKiKP,SKiKS,PKIKP,SKIKS";
    TauP_AbstractPhaseTool tool;

    /**
     * Parse comma separated list of phase names, expanding convience phase names like
     * ttp into real phase names.
     *
     * @param phaseNames string to parse
     * @return parsed list of phase names
     */
    public static List<String> extractPhaseNames(String phaseNames) {
        List<String> names = new ArrayList<>();
        for (String phaseName : splitPhaseNameList(phaseNames)) {
            if (phaseName.equalsIgnoreCase("ttp")
                    || phaseName.equalsIgnoreCase("tts")
                    || phaseName.equalsIgnoreCase("ttbasic")
                    || phaseName.equalsIgnoreCase("tts+")
                    || phaseName.equalsIgnoreCase("ttp+")
                    || phaseName.equalsIgnoreCase("ttall")) {
                if (phaseName.equalsIgnoreCase("ttp")
                        || phaseName.equalsIgnoreCase("ttp+")
                        || phaseName.equalsIgnoreCase("ttbasic")
                        || phaseName.equalsIgnoreCase("ttall")) {
                    names.add("p");
                    names.add("P");
                    names.add("Pn");
                    names.add("Pdiff");
                    names.add("PKP");
                    names.add("PKiKP");
                    names.add("PKIKP");
                }
                if (phaseName.equalsIgnoreCase("tts")
                        || phaseName.equalsIgnoreCase("tts+")
                        || phaseName.equalsIgnoreCase("ttbasic")
                        || phaseName.equalsIgnoreCase("ttall")) {
                    names.add("s");
                    names.add("S");
                    names.add("Sn");
                    names.add("Sdiff");
                    names.add("SKS");
                    names.add("SKIKS");
                }
                if (phaseName.equalsIgnoreCase("ttp+")
                        || phaseName.equalsIgnoreCase("ttbasic")
                        || phaseName.equalsIgnoreCase("ttall")) {
                    names.add("PcP");
                    names.add("pP");
                    names.add("pPdiff");
                    names.add("pPKP");
                    names.add("pPKIKP");
                    names.add("pPKiKP");
                    names.add("sP");
                    names.add("sPdiff");
                    names.add("sPKP");
                    names.add("sPKIKP");
                    names.add("sPKiKP");
                }
                if (phaseName.equalsIgnoreCase("tts+")
                        || phaseName.equalsIgnoreCase("ttbasic")
                        || phaseName.equalsIgnoreCase("ttall")) {
                    names.add("sS");
                    names.add("sSdiff");
                    names.add("sSKS");
                    names.add("sSKIKS");
                    names.add("ScS");
                    names.add("pS");
                    names.add("pSdiff");
                    names.add("pSKS");
                    names.add("pSKIKS");
                }
                if (phaseName.equalsIgnoreCase("ttbasic")
                        || phaseName.equalsIgnoreCase("ttall")) {
                    names.add("ScP");
                    names.add("SKP");
                    names.add("SKIKP");
                    names.add("PKKP");
                    names.add("PKIKKIKP");
                    names.add("SKKP");
                    names.add("SKIKKIKP");
                    names.add("PP");
                    names.add("PKPPKP");
                    names.add("PKIKPPKIKP");
                }
                if (phaseName.equalsIgnoreCase("ttall")) {
                    names.add("SKiKP");
                    names.add("PP");
                    names.add("ScS");
                    names.add("PcS");
                    names.add("PKS");
                    names.add("PKIKS");
                    names.add("PKKS");
                    names.add("PKIKKIKS");
                    names.add("SKKS");
                    names.add("SKIKKIKS");
                    names.add("SKSSKS");
                    names.add("SKIKSSKIKS");
                    names.add("SS");
                    names.add("SP");
                    names.add("PS");
                }
            } else {
                names.add(phaseName);
            }
        }
        return names;
    }

    public static String[] splitPhaseNameList(String phaseList) {
        phaseList = phaseList.trim();
        phaseList = phaseList.replace(' ', ',');
        // remove any empty phases, ie two commas next to each other
        // should be replaced with one comma
        phaseList = phaseList.replaceAll(",,+", ",");
        // remove comma at beginning
        if (phaseList.startsWith(",")) {
            phaseList = phaseList.substring(1);
        }
        if (phaseList.isEmpty()) {
            // phaseList is empty, no phases, so just return
            return new String[0];
        }
        // and comma at end
        if (phaseList.charAt(phaseList.length() - 1) == ',') {
            // we know that the length is > 1 as if not then we would have
            // returned from the previous if
            phaseList = phaseList.substring(0, phaseList.length() - 1);
        }
        return phaseList.split(",");
    }

    public void setTool(TauP_AbstractPhaseTool tool) {
        this.tool = tool;
    }

    List<String> phaseNames = new ArrayList<>();

    /**
     * names of phases to be used, ie P,S,PKIKP.
     *
     * @param phaseNamesStr phases to calculate
     */
    @CommandLine.Option(names = {"-p", "--phase", "--ph"},
            paramLabel = "phase",
            split = ",",
            description = "seismic phase names")
    public void setPhaseNames(List<String> phaseNamesStr) {
        if (tool != null) {
            tool.clearPhaseNames();
        }
        for (String phArg : phaseNamesStr) {
            for (String ph : extractPhaseNames(phArg)) {
                boolean found = false;
                for (String prev : phaseNames) {
                    if (prev.equals(ph)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    phaseNames.add(ph);
                }
            }
        }
    }


    @CommandLine.Option(names = "--phasefile", description = "read list of phase names from file")
    public void setPhaseFiles(List<String> phaseFile) {
        if (tool != null) {
            tool.clearPhaseNames();
        }
        phaseFileList = phaseFile;
    }

    public List<PhaseName> parsePhaseNameList() throws PhaseParseException {
        List<PhaseName> phases = new ArrayList<>();
            if (isEmpty()) {
                for (String pStr : extractPhaseNames(DEFAULT_PHASES)) {
                    phases.add( new PhaseName(pStr));
                }
            } else {
                for (String pStr : phaseNames) {
                    phases.add( new PhaseName(pStr));
                }
                for (String filename : phaseFileList) {
                    List<String> pList;
                    try {
                        pList = readPhaseFile(filename);
                    } catch (IOException e) {
                        throw new PhaseParseException("Unable to parse file: " + filename, e);
                    }
                    for (String pStr : pList) {
                        phases.add( new PhaseName(pStr));
                    }
                }
            }
        return phases;
    }


    public static String getPhaseNamesAsString(List<PhaseName> phaseNameList) {
        // in case of empty phase list
        if (phaseNameList.isEmpty())
            return "";
        StringBuffer phases = new StringBuffer();
        phases.append(phaseNameList.get(0).getName());
        for (PhaseName phaseName : phaseNameList.subList(1, phaseNameList.size())) {
            phases.append(",").append(phaseName.getName());
        }
        return phases.toString();
    }

    /**
     * Reads in list of phase names from a text file. So long as each phase name
     * is separated by some whitespace, " " or newline or tab, it should read
     * them fine. Also, comments are allowed, either # or // are comments to the
     * end of the line while c style slash-star make a block a comment.
     *
     * @param filename file to read
     * @return list of phase names from file
     */
    protected List<String> readPhaseFile(String filename) throws IOException {
        List<String> out = new ArrayList<>();
        FileReader fileIn = new FileReader(filename);
        StreamTokenizer tokenIn = new StreamTokenizer(fileIn);
        tokenIn.commentChar('#'); // '#' means ignore to end of line
        tokenIn.slashStarComments(true); // '/*...*/' means a comment
        tokenIn.slashSlashComments(true); // '//' means ignore to end of line
        tokenIn.wordChars('^', '^');
        tokenIn.wordChars('0', '9');
        tokenIn.wordChars('.', '.');
        tokenIn.wordChars('[', '[');
        tokenIn.wordChars(']', ']');
        while (tokenIn.nextToken() != StreamTokenizer.TT_EOF) {
            if (tokenIn.sval != null) {
                out.add(tokenIn.sval);
            } else {
                if (TauPConfig.DEBUG) {
                    Alert.info("Token.sval was null! nval=" + tokenIn.nval);
                }
            }
        }
        return out;
    }

    List<String> phaseFileList = new ArrayList<>();

    public boolean isEmpty() {
        return phaseNames.isEmpty() && phaseFileList.isEmpty();
    }
}
