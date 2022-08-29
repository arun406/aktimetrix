package com.aktimetrix.core.referencedata.service;

import com.aktimetrix.core.referencedata.model.*;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReferenceDataFileParser {

    /**
     * parse and returns the measurement type definitions
     *
     * @param inputStream
     * @return
     * @throws IOException
     * @throws CsvException
     */
    public List<MeasurementTypeDefinition> parseAndReturnMeasurementTypeDefinitions(InputStream inputStream) throws IOException, CsvException {
        CSVReader csvReader = null;
        try (Reader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            csvReader = getReader(reader);
            List<String[]> records = csvReader.readAll();
            if (records != null && records.size() > 0) {
                return prepareMeasurementTypeDefinitions(records);
            }
        } finally {
            csvReader.close();
        }
        return new ArrayList<>();
    }

    /**
     * parse and returns the measurement unit definitions
     *
     * @param inputStream
     * @return
     * @throws IOException
     * @throws CsvException
     */
    public List<MeasurementUnitDefinition> parseAndReturnMeasurementUnitDefinitions(InputStream inputStream) throws IOException, CsvException {
        CSVReader csvReader = null;
        try (Reader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            csvReader = getReader(reader);
            List<String[]> records = csvReader.readAll();
            if (records != null && records.size() > 0) {
                return prepareMeasurementUnitDefinitions(records);
            }
        } finally {
            csvReader.close();
        }
        return new ArrayList<>();
    }


    /**
     * parse and returns the process definitions
     *
     * @param inputStream
     * @return
     * @throws IOException
     * @throws CsvException
     */
    public List<ProcessDefinition> parseAndReturnProcessDefinitions(InputStream inputStream) throws IOException, CsvException {
        CSVReader csvReader = null;
        try (Reader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            csvReader = getReader(reader);
            List<String[]> records = csvReader.readAll();
            if (records != null && records.size() > 0) {
                return prepareProcessDefinitions(records);
            }
        } finally {
            csvReader.close();
        }
        return new ArrayList<>();
    }

    /**
     * parse and returns the step definitions
     *
     * @param inputStream
     * @return
     * @throws IOException
     * @throws CsvException
     */
    public List<StepDefinition> parseAndReturnStepDefinitions(InputStream inputStream) throws IOException, CsvException {
        CSVReader csvReader = null;
        try (Reader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            csvReader = getReader(reader);
            List<String[]> records = csvReader.readAll();
            if (records != null && records.size() > 0) {
                return prepareStepDefinitions(records);
            }
        } finally {
            csvReader.close();
        }
        return new ArrayList<>();
    }

    /**
     * parse and returns the step definitions
     *
     * @param inputStream
     * @return
     * @throws IOException
     * @throws CsvException
     */
    public List<EventTypeDefinition> parseAndReturnEventTypeDefinitions(InputStream inputStream) throws IOException, CsvException {
        CSVReader csvReader = null;
        try (Reader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            csvReader = getReader(reader);
            List<String[]> records = csvReader.readAll();
            if (records != null && records.size() > 0) {
                return prepareEventTypeDefinitions(records);
            }
        } finally {
            csvReader.close();
        }
        return new ArrayList<>();
    }

    private List<EventTypeDefinition> prepareEventTypeDefinitions(List<String[]> records) {
        return null;
    }

    private List<StepDefinition> prepareStepDefinitions(List<String[]> records) {
        return null;
    }

    private List<ProcessDefinition> prepareProcessDefinitions(List<String[]> records) {
        return null;
    }

    private List<MeasurementTypeDefinition> prepareMeasurementTypeDefinitions(List<String[]> records) {
        return null;
    }

    private List<MeasurementUnitDefinition> prepareMeasurementUnitDefinitions(List<String[]> records) {
        return null;
    }

    /**
     * csv reader
     *
     * @param reader
     * @return
     */
    private static CSVReader getReader(Reader reader) {
        CSVParser parser = new CSVParserBuilder()
                .withSeparator(',')
                .withIgnoreQuotations(true)
                .build();

        return new CSVReaderBuilder(reader)
                .withSkipLines(2)
                .withCSVParser(parser)
                .build();
    }

    /**
     * close the reader
     *
     * @param reader
     * @throws IOException
     */
    public static void closeReader(CSVReader reader) throws IOException {
        if (reader != null) {
            reader.close();
        }
    }


}
