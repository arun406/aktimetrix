package com.aktimetrix.aktimetrix.reference.data.service;

import com.aktimetrix.aktimetrix.reference.data.dto.FlightGroupDTO;
import com.aktimetrix.aktimetrix.reference.data.dto.LineDTO;
import com.aktimetrix.aktimetrix.reference.data.dto.ProductGroupDTO;
import com.aktimetrix.aktimetrix.reference.data.dto.RouteDTO;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EncoreFileParserService {

    /**
     * returns the CSVReader
     *
     * @param inputStream
     * @return
     */
    public static List<ProductGroupDTO> parseAndReturnProductGroups(InputStream inputStream) throws IOException, CsvException {
        CSVReader csvReader = null;
        try (Reader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            csvReader = getReader(reader);
            List<String[]> records = csvReader.readAll();
            if (records != null && records.size() > 0) {
                return prepareProductGroups(records);
            }
        } finally {
            csvReader.close();
        }
        return new ArrayList<>();
    }

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

    public static List<FlightGroupDTO> parseAndReturnFlightGroups(InputStream inputStream) throws IOException, CsvException {
        CSVReader csvReader = null;
        try (Reader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            csvReader = getReader(reader);
            List<String[]> records = csvReader.readAll();
            if (records != null && records.size() > 0) {
                return prepareFlightGroups(records);
            }
        } finally {
            csvReader.close();
        }
        return new ArrayList<>();
    }

    public static List<RouteDTO> parseAndReturnRoutes(InputStream inputStream) throws IOException, CsvException {
        CSVReader csvReader = null;
        try (Reader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            csvReader = getReader(reader);
            List<String[]> records = csvReader.readAll();
            if (records != null && records.size() > 0) {
                return prepareRoutes(records);
            }
        } finally {
            csvReader.close();
        }
        return new ArrayList<>();
    }


    public static List<LineDTO> parseAndReturnLines(InputStream inputStream) throws IOException, CsvException {
        CSVReader csvReader = null;
        try (Reader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            csvReader = getReader(reader);
            List<String[]> records = csvReader.readAll();
            if (records != null && records.size() > 0) {
                return prepareLines(records);
            }
        } finally {
            csvReader.close();
        }
        return new ArrayList<>();
    }

    /**
     * product group
     *
     * @param records
     * @return
     */
    public static List<ProductGroupDTO> prepareProductGroups(List<String[]> records) {
        return records.stream()
                .filter(record -> Arrays.stream(record).noneMatch(s -> s.equals("EOF")))
                .map(record -> new ProductGroupDTO(record[0], record[1], record[2], record[3], record[4]))
                .collect(Collectors.toList());
    }

    private static List<FlightGroupDTO> prepareFlightGroups(List<String[]> records) {
        return records.stream()
                .filter(record -> Arrays.stream(record).noneMatch(s -> s.equals("EOF")))
                .map(record -> new FlightGroupDTO(record[0], record[1], record[2], record[3]))
                .collect(Collectors.toList());
    }

    private static List<RouteDTO> prepareRoutes(List<String[]> records) {
        return records.stream()
                .filter(record -> Arrays.stream(record).noneMatch(s -> s.equals("ENDOFFILE")))
                .map(record -> new RouteDTO(record[0], record[1], record[2], record[3]))
                .collect(Collectors.toList());
    }

    private static List<LineDTO> prepareLines(List<String[]> records) {
        return records.stream()
                .filter(record -> Arrays.stream(record).noneMatch(s -> s.equals("END_OF_FILE")))
                .map(record -> {
                    LineDTO dto = new LineDTO();
                    dto.setAirline(record[0]);
                    dto.setForwarderCode(record[1]);
                    dto.setAirport(record[2]);
                    dto.setExportInd(record[3]);
                    dto.setImportInd(record[4]);
                    dto.setTransitInd(record[5]);
                    dto.setProductCode(record[6]);
                    dto.setProductGroupCode(record[7]);
                    dto.setDow(record[8]);
                    dto.setFlightNo(record[9]);
                    dto.setFlightGroupCode(record[10]);
                    dto.setAcCategory(record[11]);
                    dto.setFohBeforeFwbInd(record[12]);
                    dto.setFWB(record[13]);
                    dto.setLAT(record[14]);
                    dto.setRCS(record[15]);
                    dto.setDEP(record[16]);
                    dto.setARR(record[17]);
                    dto.setAWR(record[18]);
                    dto.setRCF(record[19]);
                    dto.setNFD(record[20]);
                    dto.setAWD(record[21]);
                    dto.setDLV(record[22]);
                    dto.setRCFT(record[23]);
                    dto.setTFDT(record[24]);
                    dto.setRCTT(record[25]);
                    dto.setARRT(record[26]);
                    dto.setDEPT(record[27]);
                    dto.setWtInd(record[28]);
                    dto.setVolInd(record[29]);
                    dto.setEFreightInd(record[30]);
                    dto.setNotes(record[31]);
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
