package ru.nsu.ccfit.khudyakov;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Comparator.reverseOrder;
import static java.util.Map.Entry;
import static java.util.stream.Collectors.toMap;

public class StaxOsmAnalyser implements OsmAnalyser {

    private final Logger logger = LogManager.getLogger(StaxOsmAnalyser.class);

    private static final String NODE_ELEMENT = "node";

    private static final String USER_ATR = "user";

    private final Map<String, Integer> userOccurrenceMap = new HashMap<>();

    private static final String TAG_ELEMENT = "tag";

    private static final String KEY_ATR = "k";

    private final Map<String, Integer> tagOccurrenceMap = new HashMap<>();

    @Override
    public void analyze(InputContext inputContext) {
        if (inputContext == null) {
            logger.error("Empty input");
            System.exit(1);
        }

        logger.debug("Start reading archive");
        try (InputStream inputStream = new FileInputStream(inputContext.getArchiveFilePath());
             InputStream bufferedStream = new BufferedInputStream(inputStream);
             InputStream bzipStream = new BZip2CompressorInputStream(bufferedStream)) {

            findOccurrences(bzipStream);

            logger.debug("End reading archive");
        } catch (IOException e) {
            logger.error("Error during opening archive file. " + e.getMessage(), e);
            System.exit(1);
        }

    }

    @Override
    public void printReport(InputContext inputContext) {
        logger.debug("Start printing report");

        if (inputContext.getResultFilePath() == null) {
            print(System.out);
        }

        try (FileOutputStream outputStream = new FileOutputStream(inputContext.getResultFilePath());
             PrintStream printStream = new PrintStream(outputStream)) {
            print(printStream);
        } catch (IOException e) {
            logger.error("Couldn't write to result file" + e.getMessage(), e);
            print(System.out);
        }

        logger.debug("End printing report");
    }

    private void findOccurrences(InputStream inputStream) {
        try {
            logger.debug("Start parsing xml");

            XMLEventReader xmlEventReader = XMLInputFactory.newInstance().createXMLEventReader(inputStream);
            while (xmlEventReader.hasNext()) {
                XMLEvent xmlEvent = xmlEventReader.nextEvent();

                if (xmlEvent.isStartElement()) {
                    readNode(xmlEventReader, xmlEvent);
                }
            }

            logger.debug("End parsing xml");
        } catch (XMLStreamException e) {
            logger.error("Could't read osm file\n");
            System.exit(0);
        }
    }

    private void readNode(XMLEventReader xmlEventReader, XMLEvent xmlEvent) throws XMLStreamException {
        if (!checkAttributeOccurrence(xmlEvent, userOccurrenceMap, NODE_ELEMENT, USER_ATR)) {
            return;
        }
        readTags(xmlEventReader);
    }

    private void readTags(XMLEventReader xmlEventReader) throws XMLStreamException {

        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = xmlEventReader.nextEvent();

            if (xmlEvent.isStartElement()) {
                checkAttributeOccurrence(xmlEvent, tagOccurrenceMap, TAG_ELEMENT, KEY_ATR);
            }

            if (xmlEvent.isEndElement()) {
                EndElement endElement = xmlEvent.asEndElement();
                if (endElement.getName().getLocalPart().equals(NODE_ELEMENT)) {
                    break;
                }
            }
        }
    }

    private boolean checkAttributeOccurrence(XMLEvent xmlEvent,
                                             Map<String, Integer> attributeOccurrenceMap,
                                             String elementName,
                                             String attributeName) {
        StartElement element = xmlEvent.asStartElement();

        if (!element.getName().getLocalPart().equals(elementName)) {
            return false;
        }

        Attribute attribute = element.getAttributeByName(new QName(attributeName));
        if (attribute == null) {
            return true;
        }

        attributeOccurrenceMap.merge(attribute.getValue(), 1, Integer::sum);
        return true;
    }

    private void print(PrintStream printStream) {

        Map<String, Integer> sortedUserOccurrenceMap = userOccurrenceMap.entrySet().stream()
                .sorted(Entry.comparingByValue(reverseOrder()))
                .collect(toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        printStream.println("***************Node users statistic***************");
        for (Entry<String, Integer> entry : sortedUserOccurrenceMap.entrySet()) {
            printStream.println(entry.getKey() + " " + entry.getValue());
        }

        printStream.println();

        printStream.println("***************Tag keys statistic***************");
        for (Entry<String, Integer> entry : tagOccurrenceMap.entrySet()) {
            printStream.println(entry.getKey() + " " + entry.getValue());
        }
    }

}
