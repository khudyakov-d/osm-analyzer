package ru.nsu.ccfit.khudyakov;

import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InputParser {

    private final Logger logger = LogManager.getLogger(InputParser.class);

    private final String[] args;

    private static final String ARCHIVE_FILE_OPTION = "a";

    private static final String RESULT_FILE_OPTION = "r";

    private final Options options;

    private final CommandLineParser commandLineParser = new DefaultParser();

    private final HelpFormatter helpFormatter = new HelpFormatter();

    public InputParser(String[] args) {
        this.args = args;
        this.options = prepareOptions();
    }

    private Options prepareOptions() {
        Options options = new Options();

        Option archiveOption = Option.builder(ARCHIVE_FILE_OPTION)
                .required()
                .desc("Archive containing osm file for analysis")
                .longOpt("archive-file")
                .hasArg()
                .argName("file")
                .build();
        options.addOption(archiveOption);

        Option resultFileOption = Option.builder(RESULT_FILE_OPTION)
                .optionalArg(true)
                .desc("The file where you want to save the result")
                .longOpt("result-file")
                .hasArg()
                .argName("file")
                .build();
        options.addOption(resultFileOption);

        return options;
    }

    public InputContext parse() {
        try {
            logger.debug("Start parsing command line input");

            CommandLine cmd = commandLineParser.parse(options, args);
            InputContext inputContext = new InputContext();

            inputContext.setArchiveFilePath(cmd.getOptionValue(ARCHIVE_FILE_OPTION));

            if (!cmd.hasOption(RESULT_FILE_OPTION)) {
                return inputContext;
            }
            inputContext.setResultFilePath(cmd.getOptionValue(RESULT_FILE_OPTION));

            logger.debug("End parsing command line input");

            return inputContext;
        } catch (ParseException e) {
            helpFormatter.printHelp("gradle run [-options] [args...]", options);
            logger.error(e.getMessage(), e);
            System.exit(1);
        }

        return null;
    }

}
