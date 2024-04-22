package edu.sc.seis.TauP.cli;

public class TauModelOutputTypeArgs extends  AbstractOutputTypeArgs {
    public TauModelOutputTypeArgs(String defaultFormat, String filebase) {
        super(filebase);
        setOutputType(defaultFormat);
    }

    @Override
    public void setOutputType(String oType) {
        if (! oType.equalsIgnoreCase(OutputTypes.TAUP)) {
            throw new IllegalArgumentException("Only 'taup' allowed for taup create");
        }
    }

    @Override
    public String getOuputFormat() {
        return OutputTypes.TAUP;
    }

    @Override
    public String getOutFileExtension() {
        return OutputTypes.TAUP;
    }
}
