package edu.sc.seis.TauP.cli;

public class TauModelOutputTypeArgs extends  AbstractOutputTypeArgs {
    public TauModelOutputTypeArgs(String defaultFormat, String filebase) {
        super(filebase);
        setOutputFormat(defaultFormat);
    }

    @Override
    public void setOutputFormat(String oType) {
        if (! oType.equalsIgnoreCase(OutputTypes.TAUP)) {
            throw new IllegalArgumentException("Only 'taup' allowed for taup create");
        }
    }

    @Override
    public String getOutputFormat() {
        return OutputTypes.TAUP;
    }

    @Override
    public String getOutFileExtension() {
        return OutputTypes.TAUP;
    }
}
