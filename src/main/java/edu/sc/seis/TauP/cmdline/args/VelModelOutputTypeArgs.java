package edu.sc.seis.TauP.cmdline.args;

import picocli.CommandLine;

import java.util.Objects;

public class VelModelOutputTypeArgs extends AbstractOutputTypeArgs {

    String outType = OutputTypes.ND;

    public VelModelOutputTypeArgs(String filebase) {
        super(filebase);
    }

    @Override
    public void setOutputFormat(String oType) {
      if (Objects.equals(oType, OutputTypes.ND) || Objects.equals(oType, OutputTypes.JSON)) {
          outType = oType;
      } else {
          throw new ArgumentValidationException("output type " + oType + " not recognized.");
      }
    }

    @Override
    public String getOutputFormat() {
        return outType;
    }

    @Override
    public String getOutFileExtension() {
        if (outType.equals(OutputTypes.JSON)) {
            return OutputTypes.JSON;
        } else {
            return OutputTypes.ND;
        }
    }

    @CommandLine.Option(names = {"--json"},
            description = "output as a \".json\" velocity file, otherwise will be a \".nd\" velocity file"
    )
    public void setJson(boolean _isJSON) {
        outType = OutputTypes.JSON;
    }
}
