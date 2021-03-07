package ru.nsu.ccfit.khudyakov;

public class Main {

    public static void main(String[] args) {
        InputParser inputParser = new InputParser(args);
        InputContext inputContext = inputParser.parse();
        OsmAnalyser staxOsmAnalyser = new StaxOsmAnalyser();

        staxOsmAnalyser.analyze(inputContext);
        staxOsmAnalyser.printReport(inputContext);
    }

}
